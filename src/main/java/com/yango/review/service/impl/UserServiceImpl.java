package com.yango.review.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yango.review.dto.LoginFormDTO;
import com.yango.review.dto.Result;
import com.yango.review.dto.UserDTO;
import com.yango.review.entity.User;
import com.yango.review.mapper.UserMapper;
import com.yango.review.service.IUserService;
import com.yango.review.utils.RegexUtils;
import com.yango.review.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yango.review.utils.RedisConstants.*;
import static com.yango.review.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.检查手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //1.1格式错误
            return Result.fail("手机号格式错误");
        }
        //1.2格式正确,生成验证码
        String code = RandomUtil.randomNumbers(6);
        //2.保存到session
        //session.setAttribute("code",code);
        //2.保存到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //3.发送验证码
        log.debug("调用云平台发送验证码:{}",code);
        //4.返回结果
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.检查手机号
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        //2.校验验证码
        //Object cacheCode = session.getAttribute("code");
        //2.从redis中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)){
            //3.不一致,返回错误信息
            return Result.fail("验证码错误");
        }
        //4.一致,根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        //5.用户是否存在
        if (user == null) {
            //6.不存在,创建新用户,保存用户数据
            user = createUserWithPhone(loginForm.getPhone());
        }
        //7.存在,保存到session中
        //UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //session.setAttribute("user", userDTO);
        //7.保存用户到redis中
        //7.1随机生成token,作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2将User对象转换为Hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        //7.3存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //8返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        //获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        //保存
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth - 1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        //获取今天是本月第几天
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        int dayCount = 0;
        while (true){
            if ((num & 1 ) == 0) {
                break;
            }else{
                dayCount++;
            }
            num >>>= 1;
        }
        return Result.ok(dayCount);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}