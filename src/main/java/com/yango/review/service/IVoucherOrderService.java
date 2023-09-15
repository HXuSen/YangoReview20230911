package com.yango.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yango.review.dto.Result;
import com.yango.review.entity.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(VoucherOrder voucherOrder);

}