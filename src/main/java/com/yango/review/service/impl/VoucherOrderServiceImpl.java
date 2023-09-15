package com.yango.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.dto.Result;
import com.yango.review.entity.VoucherOrder;
import com.yango.review.mapper.VoucherOrderMapper;
import com.yango.review.service.ISeckillVoucherService;
import com.yango.review.service.IVoucherOrderService;
import com.yango.review.utils.RedisIdWorker;
import com.yango.review.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("订单异常:{}",e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("不允许重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }
    //@Override
    //public Result seckillVoucher(Long voucherId) {
    //    //1.查询优惠券信息
    //    SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
    //    if (seckillVoucher == null){
    //        return Result.fail("优惠券不存在");
    //    }
    //    //2.判断是否开始或结束
    //    if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
    //        return Result.fail("秒杀尚未开始");
    //    }
    //    if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
    //        return Result.fail("秒杀已经结束");
    //    }
    //    //3.判断库存是否充足
    //    if (seckillVoucher.getStock() < 1) {
    //        return Result.fail("库存不足");
    //    }
    //    Long userId = UserHolder.getUser().getId();
    //    //synchronized (userId.toString().intern()) {
    //    //    //获取代理对象(事务)
    //    //    IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //    //    return proxy.createVoucherOrder(voucherId);
    //    //}
    //    //SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
    //    RLock lock = redissonClient.getLock("lock:order:" + userId);
    //    //boolean isLock = lock.tryLock(5);
    //    boolean isLock = lock.tryLock();
    //    if (!isLock){
    //        return Result.fail("不允许重复下单");
    //    }
    //    try {
    //        //获取代理对象(事务)
    //        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
    //        return proxy.createVoucherOrder(voucherId);
    //    } catch (IllegalStateException e) {
    //        throw new RuntimeException(e);
    //    } finally {
    //        lock.unlock();
    //    }
    //}

    private IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();;
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        int r = result.intValue();
        if (r != 0){
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        long orderId = redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        orderTasks.add(voucherOrder);
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }
    @Transactional
    public Result createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //一人一单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0){
            return Result.fail("一人仅限一单");
        }
        //4.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                //where id = ? and stock = ? #乐观锁
                //.eq("voucher_id", voucherId).eq("stock",seckillVoucher.getStock())
                //stock > 0
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock",0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        save(voucherOrder);
        //6.返回订单ID
        return Result.ok(voucherOrder.getId());
    }
}