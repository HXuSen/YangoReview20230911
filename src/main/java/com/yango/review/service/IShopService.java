package com.yango.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yango.review.dto.Result;
import com.yango.review.entity.Shop;

public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}