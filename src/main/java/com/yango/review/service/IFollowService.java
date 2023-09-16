package com.yango.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yango.review.dto.Result;
import com.yango.review.entity.Follow;

public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}