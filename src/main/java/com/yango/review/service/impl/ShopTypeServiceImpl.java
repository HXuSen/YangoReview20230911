package com.yango.review.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.dto.Result;
import com.yango.review.entity.ShopType;
import com.yango.review.mapper.ShopTypeMapper;
import com.yango.review.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.stream.Collectors;

import static com.yango.review.utils.RedisConstants.CACHE_TYPE_KEY;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        List<String> typeList = stringRedisTemplate.opsForList().range(CACHE_TYPE_KEY, 0, -1);
        if (CollectionUtil.isNotEmpty(typeList)){
            List<ShopType> shopTypes = typeList.stream()
                    .map(str -> JSONUtil.toBean(str, ShopType.class))
                    .sorted()
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        List<ShopType> dbShopTypes = query().orderByAsc("sort").list();
        if (CollectionUtil.isEmpty(dbShopTypes)){
            return Result.fail("无数据");
        }
        dbShopTypes.stream()
                .forEach(shopType -> stringRedisTemplate.opsForList().leftPush(CACHE_TYPE_KEY,JSONUtil.toJsonStr(shopType)));

        return Result.ok(dbShopTypes);
    }
}