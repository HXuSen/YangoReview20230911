package com.yango.review.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yango.review.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.yango.review.utils.RedisConstants.*;

/**
 * ClassName: CacheClient
 * Package: com.yango.review.utils
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/12-17:32
 */
@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key = keyPrefix + id;
        //1.根据id从redis中查询
        String json = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //2.1存在 返回
            return JSONUtil.toBean(json, type);
        }
        if (json != null){
            return null;
        }
        //2.2不存在 查询数据库
        R r = dbFallback.apply(id);
        //3.1不存在,返回错误
        if (r == null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //3.2存在 写入redis,返回
        this.set(key,r,time,unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        String key = keyPrefix + id;
        //1.根据id从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //命中,判断是否过期
        //未过期,返回信息
        if (expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        //已过期
        //获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //获取成功,开启独立线程
        if (isLock){
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        //获取失败,返回过期信息
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "locked");
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
