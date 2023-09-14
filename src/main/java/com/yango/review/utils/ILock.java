package com.yango.review.utils;

/**
 * ClassName: ILock
 * Package: com.yango.review.utils
 * Description:
 *
 * @Author HuangXuSen
 * @Create 2023/9/14-17:30
 */
public interface ILock {

    boolean tryLock(long timeoutSec);

    void unLock();
}
