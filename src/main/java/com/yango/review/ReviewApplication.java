package com.yango.review;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * ClassName: ReviewApplication
 * Package: com.yango.review
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/11-16:04
 */
@SpringBootApplication
@MapperScan("com.yango.review.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class ReviewApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReviewApplication.class,args);
    }
}
