package com.yango.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.dto.Result;
import com.yango.review.entity.SeckillVoucher;
import com.yango.review.entity.VoucherOrder;
import com.yango.review.mapper.VoucherOrderMapper;
import com.yango.review.service.ISeckillVoucherService;
import com.yango.review.service.IVoucherOrderService;
import com.yango.review.utils.RedisIdWorker;
import com.yango.review.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券信息
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null){
            return Result.fail("优惠券不存在");
        }
        //2.判断是否开始或结束
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //3.判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            //获取代理对象(事务)
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //一人一单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0){
            return Result.fail("一人仅限一单");
        }
        //4.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                //where id = ? and stock = ? #乐观锁
                //.eq("voucher_id", voucherId).eq("stock",seckillVoucher.getStock())
                //stock > 0
                .eq("voucher_id", voucherId).gt("stock",0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        //5.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("order"));
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //6.返回订单ID
        return Result.ok(voucherOrder.getId());
    }
}