package com.yango.review.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: RedisConfig
 * Package: com.yango.review.config
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/14-18:24
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.162.101:6379").setPassword("hxsstu");
        return Redisson.create(config);
    }
}
