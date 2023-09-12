package com.yango.review.test;

import com.yango.review.service.IShopService;
import com.yango.review.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

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

    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(1L,10L);
    }
}
