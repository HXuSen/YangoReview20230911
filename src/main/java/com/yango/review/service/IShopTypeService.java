package com.yango.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yango.review.dto.Result;
import com.yango.review.entity.ShopType;

public interface IShopTypeService extends IService<ShopType> {

    Result queryList();

}
