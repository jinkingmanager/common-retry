package com.vip.finance.retry.handler;

import com.alibaba.fastjson.JSON;
import com.vip.finance.retry.entity.RetryInfo;
import com.vip.finance.retry.manager.RetryInfoMananger;
import com.vip.finance.retry.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RetryHandler implements Runnable {

    private RetryInfo fundRetryInfo;

    private RetryInfoMananger fundRetryInfoMananger;

    public RetryHandler(RetryInfo fundRetryInfo, RetryInfoMananger fundRetryInfoMananger) {
        this.fundRetryInfo = fundRetryInfo;
        this.fundRetryInfoMananger = fundRetryInfoMananger;
    }

    @Override
    @Transactional
    public void run() {
        retry(fundRetryInfo,fundRetryInfoMananger);
    }

    private void retry(RetryInfo retryInfo, RetryInfoMananger fundRetryInfoMananger){
        try {
            // 构造对应的需要重试的数据
            log.info("Thread" + Thread.currentThread().getName() + "|| deal retry info:", retryInfo.toString());
            // 获取bean class
            Class beanClazz = getClass().getClassLoader().loadClass(retryInfo.getBeanClassName());

            // 获取params class
            Class paramClazz = getClass().getClassLoader().loadClass(retryInfo.getReqParamsClassName());

            // 获取对应的方法
            Method retryMethod = beanClazz.getDeclaredMethod(retryInfo.getMethodName(),paramClazz);

            if (null != retryMethod) {
                try {
                    // 执行重试方法
                    retryMethod.invoke(SpringContextUtil.getBean(retryInfo.getBeanName()),
                            JSON.parseObject(retryInfo.getReqParams(), paramClazz));

                    // 如果不抛出异常，则表示重试成功，处理数据
                    fundRetryInfoMananger.retrySucc(retryInfo.getId());
                } catch (InvocationTargetException e) {
                    // 继续抛出异常，则addRetryCount
                    log.error("Thread" + Thread.currentThread().getName() + "|| retry exception,e=", e);
                    fundRetryInfoMananger.addRetryCount(retryInfo.getId(), ((InvocationTargetException) e).getTargetException().toString());
                } catch (Exception e) {
                    // 其他异常，打印日志，不做其他操作
                    log.error("Thread" + Thread.currentThread().getName() + "|| inner ---- handler retry error,e=", e);
                }
            }
            log.error("Thread" + Thread.currentThread().getName() + "|| Can not find method " + retryInfo.getMethodName() +" , please check params and scope" );
        } catch (Exception e) {
            // 其他异常，打印日志，不做其他操作
            log.error("Thread" + Thread.currentThread().getName() + "|| out ---- handler retry error ,e=", e);
        }

    }
}
