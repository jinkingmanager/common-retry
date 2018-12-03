package com.vip.finance.retry.model;

import com.google.common.base.Preconditions;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryInfoModel extends RetryBaseModel{

    private String bizId; // 业务ID，必传
    private Class beanClassName; // bean对应的class
    private String beanName; // 重试时需要调用的beanName
    private String methodName; // 重试时需要调用的methodName
    private Integer priority; // 当前重试的优先级 不传的话默认是3 --普通
    private String reqParams; // 请求参数，json格式
    private Class reqParamsClassName; // 请求参数对应的class
    private String falloffType; // 超时时间类型，详见FalloffTypeEnum
    private Integer falloffInterval; // 延时时间，默认为60秒，单位为秒
    private Integer maxRetryCount; // 最大重试次数，不传默认为99
    private int retryTime; // 多长时间后重试，单位为秒,不传的话默认为30，即30秒之后重试

    public void check() {
        Preconditions.checkNotNull(bizId,"BizId can not be null!");
        Preconditions.checkNotNull(beanClassName,"BeanClassName can not be null!");
        Preconditions.checkNotNull(beanName, "BeanName can not be null!");
        Preconditions.checkNotNull(methodName, "Method name can not be null!");
        Preconditions.checkNotNull(reqParams, "ReqParams can not be null!");
        Preconditions.checkNotNull(reqParamsClassName,"ReqParamsClassName can not be null!");
    }

}
