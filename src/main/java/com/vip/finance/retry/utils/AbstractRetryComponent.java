package com.vip.finance.retry.utils;

import com.alibaba.fastjson.JSONObject;
import com.vip.finance.retry.exceptions.CommonRetryException;
import com.vip.finance.retry.manager.RetryFacade;
import com.vip.finance.retry.model.RetryComponentConfig;
import com.vip.finance.retry.model.RetryInfoModel;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.CheckedFunction1;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于 resilience4j-retry 封装的代码中直接retry的组件
 */
@Component
@Data
@Slf4j
@NoArgsConstructor
public abstract class AbstractRetryComponent<T, R> {

    @Autowired
    private RetryFacade retryFacade;

    public R retryTemplate(T t) {

        log.info(" request:{}",
                JSONObject.toJSONString(t));

        RetryComponentConfig retryComponentConfig = initRetryConfig();

        RetryConfig retryConfig = RetryConfig.custom().maxAttempts(retryComponentConfig.getRetryTimes())
                .waitDuration(Duration.of(retryComponentConfig.getDuration(), retryComponentConfig.getDurationUnit()))
                .retryExceptions(CommonRetryException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);

        Retry retry = registry.retry("retry-template" + ThreadLocalRandom.current().nextLong());


        CheckedFunction1<T, R> decorated =
                Retry.decorateCheckedFunction(retry, t1 -> handleBiz(t1));

        R response = null;
        try {
            response = decorated.apply(t);
        } catch (CommonRetryException throwable) {
            log.info("after retry failed, go go go save retry info......");
            log.error("error:", throwable.getMessage());
            RetryInfoModel retryInfoModel = wrapRetryInfoInDb(t);

            retryInfoModel.check();

            retryFacade.saveRetryInfo(retryInfoModel);
        } catch (Throwable throwable) {
            log.error("error:", throwable.getMessage());
        }

        return response;
    }

    /**
     * 处理实际的retry操作，这里只需要调用super.retryTemplate()t 即可
     *
     * @param t
     * @return
     */
    public abstract R handleBizWithRetry(T t);

    /**
     * 初始化retry相关配置
     *
     * @return
     */
    public abstract RetryComponentConfig initRetryConfig();

    /**
     * 具体的retry业务逻辑，各个业务方自己实现
     *
     * @param t
     * @return
     * @throws CommonRetryException
     */
    public abstract R handleBiz(T t) throws CommonRetryException;

    /**
     * retry N次失败后，需要将对应数据存入db 用于后续的批处理重试
     *
     * @param t
     * @return
     */
    public abstract RetryInfoModel wrapRetryInfoInDb(T t);

}
