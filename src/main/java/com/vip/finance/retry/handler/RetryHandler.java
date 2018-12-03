package com.vip.finance.retry.handler;

import com.alibaba.fastjson.JSON;
import com.vip.finance.retry.entity.RetryInfo;
import com.vip.finance.retry.manager.RetryInfoMananger;
import com.vip.finance.retry.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;

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
    public void run() {
        retry(fundRetryInfo,fundRetryInfoMananger);
    }

    private void retry(RetryInfo fundRetryInfo, RetryInfoMananger fundRetryInfoMananger){
        try {
            // 构造对应的需要重试的数据
            log.debug("retry info:", fundRetryInfo.toString());
            // 获取bean class
            Class beanClazz = getClass().getClassLoader().loadClass(fundRetryInfo.getBeanClassName());

            // 获取params class
            Class paramClazz = getClass().getClassLoader().loadClass(fundRetryInfo.getReqParamsClassName());

            // 获取对应的方法
            Method retryMethod = beanClazz.getMethod(fundRetryInfo.getMethodName(),paramClazz);

            if (null != retryMethod) {
                try {
                    // 执行重试方法
                    retryMethod.invoke(SpringContextUtil.getBean(fundRetryInfo.getBeanName()),
                            JSON.parseObject(fundRetryInfo.getReqParams(), paramClazz));

                    // 如果不抛出异常，则表示重试成功，处理数据
                    fundRetryInfoMananger.retrySucc(fundRetryInfo.getId());
                } catch (InvocationTargetException e) {
                    // 继续抛出异常，则addRetryCount
                    log.error("retry exception,e=", e);
                    fundRetryInfoMananger.addRetryCount(fundRetryInfo.getId(), ((InvocationTargetException) e).getTargetException().toString());
                } catch (Exception e) {
                    // 其他异常，打印日志，不做其他操作
                    log.error("inner ---- handler retry error,e=", e);
                }
            }
        } catch (Exception e) {
            // 其他异常，打印日志，不做其他操作
            log.error("out ---- handler retry error ,e=", e);
        }

    }
}
