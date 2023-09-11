package com.yango.review.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yango.review.dto.UserDTO;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yango.review.utils.RedisConstants.LOGIN_USER_KEY;
import static com.yango.review.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * ClassName: LoginInterceptor
 * Package: com.yango.review.utils
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/11-17:14
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否需要拦截--->threadlocal中是否有用户
        if (UserHolder.getUser() == null){
            response.setStatus(401);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
