package com.yango.review.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yango.review.service.IShopService;
import com.yango.review.service.impl.ShopServiceImpl;
import com.yango.review.utils.RedisIdWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClassName: ReviewApplicationTest
 * Package: com.yango.review.test
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/12-16:53
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class ReviewApplicationTest {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;

    //@Test
    //public void testSaveShop(){
    //    shopService.saveShop2Redis(1L,10L);
    //}

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0;i < 100;i++){
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time=" + (end - begin));
    }
}
