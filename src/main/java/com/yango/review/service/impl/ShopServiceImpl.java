package com.yango.review.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.dto.Result;
import com.yango.review.entity.Shop;
import com.yango.review.mapper.ShopMapper;
import com.yango.review.service.IShopService;
import com.yango.review.utils.CacheClient;
import com.yango.review.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.yango.review.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //if (shop == null) {
        //    return Result.fail("店铺不存在");
        //}
        //逻辑过期解决缓存击穿
        //Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return Result.ok(shop);
    }

    //private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    //public Shop queryWithLogicalExpire(Long id){
    //    //1.根据id从redis中查询商铺缓存
    //    String shopJSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
    //    //2.判断是否存在
    //    if (StrUtil.isBlank(shopJSON)) {
    //        return null;
    //    }
    //    RedisData redisData = JSONUtil.toBean(shopJSON, RedisData.class);
    //    Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
    //    LocalDateTime expireTime = redisData.getExpireTime();
    //    //命中,判断是否过期
    //    //未过期,返回信息
    //    if (expireTime.isAfter(LocalDateTime.now())){
    //        return shop;
    //    }
    //    //已过期
    //    //获取锁
    //    String lockKey = LOCK_SHOP_KEY + id;
    //    boolean isLock = tryLock(lockKey);
    //    //获取成功,开启独立线程
    //    if (isLock){
    //        CACHE_REBUILD_EXECUTOR.submit(() -> {
    //            try {
    //                this.saveShop2Redis(id,20L);
    //            } catch (Exception e) {
    //                throw new RuntimeException(e);
    //            } finally {
    //                unLock(lockKey);
    //            }
    //        });
    //    }
    //    //获取失败,返回过期信息
    //    return shop;
    //}

    //public Shop queryWithMutex(Long id){
    //    //1.根据id从redis中查询商铺缓存
    //    String shopJSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
    //    //2.判断是否存在
    //    if (StrUtil.isNotBlank(shopJSON)) {
    //        //2.1存在 返回
    //        Shop shop = JSONUtil.toBean(shopJSON, Shop.class);
    //        return shop;
    //    }
    //    if (shopJSON != null){
    //        return null;
    //    }
    //    //加锁
    //    String lockKey = LOCK_SHOP_KEY + id;
    //    Shop shop;
    //    try {
    //        boolean isLock = tryLock(lockKey);
    //        if (!isLock) {
    //            //加锁失败,休眠,重试
    //            Thread.sleep(50);
    //            return queryWithMutex(id);
    //        }
    //        //双检
    //        shopJSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
    //        if (StrUtil.isNotBlank(shopJSON)) {
    //            shop = JSONUtil.toBean(shopJSON, Shop.class);
    //            return shop;
    //        }
    //
    //        //2.2不存在 查询数据库
    //        shop = getById(id);
    //        //3.1不存在,返回错误
    //        if (shop == null) {
    //            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
    //            return null;
    //        }
    //        //3.2存在 写入redis,返回
    //        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //    }catch (InterruptedException e){
    //        throw new RuntimeException();
    //    } finally {
    //        unLock(lockKey);
    //    }
    //
    //    return shop;
    //}

    //public Shop queryWithPassThrough(Long id){
    //    //1.根据id从redis中查询商铺缓存
    //    String shopJSON = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
    //    //2.判断是否存在
    //    if (StrUtil.isNotBlank(shopJSON)) {
    //        //2.1存在 返回
    //        Shop shop = JSONUtil.toBean(shopJSON, Shop.class);
    //        return shop;
    //    }
    //    if (shopJSON != null){
    //        return null;
    //    }
    //    //2.2不存在 查询数据库
    //    Shop dbShop = getById(id);
    //    //3.1不存在,返回错误
    //    if (dbShop == null) {
    //        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
    //        return null;
    //    }
    //    //3.2存在 写入redis,返回
    //    stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(dbShop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //    return dbShop;
    //}

    //private boolean tryLock(String key){
    //    Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "locked");
    //    return BooleanUtil.isTrue(flag);
    //}
    //private void unLock(String key){
    //    stringRedisTemplate.delete(key);
    //}
    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺ID不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    //public void saveShop2Redis(Long id,Long expireSeconds){
    //    Shop shop = getById(id);
    //    RedisData redisData = new RedisData();
    //    redisData.setData(shop);
    //    redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
    //
    //    stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    //}
}