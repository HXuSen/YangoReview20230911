package com.yango.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.entity.Follow;
import com.yango.review.mapper.FollowMapper;
import com.yango.review.service.IFollowService;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

}