package com.yango.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yango.review.dto.LoginFormDTO;
import com.yango.review.dto.Result;
import com.yango.review.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}