package com.yango.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.entity.User;
import com.yango.review.mapper.UserMapper;
import com.yango.review.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}