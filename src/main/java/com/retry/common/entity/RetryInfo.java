package com.retry.common.entity;

import com.retry.common.enums.RetryPriorityEnum;
import com.retry.common.model.RetryInfoModel;
import com.retry.common.enums.DecayTypeEnum;
import com.retry.common.enums.RetryStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryInfo {

    private Integer id;
    private String bizId;
    private String beanClassName;
    private String beanName;
    private String methodName;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String status;
    private Integer priority;
    private String reqParams;
    private String reqParamsClassName;
    private String falloffType;
    private Integer falloffInterval;
    private Date retryTime;
    private Date createTime;
    private Date updateTime;


    /**
     * 创建重试信息
     *
     * @param retryInfoModel
     */
    public RetryInfo(RetryInfoModel retryInfoModel) {

        Instant instant = Instant.now();

        this.bizId = retryInfoModel.getBizId();
        this.beanClassName = retryInfoModel.getBeanClassName().getName();
        this.reqParamsClassName = retryInfoModel.getReqParamsClassName().getName();
        this.beanName = retryInfoModel.getBeanName();
        this.methodName = retryInfoModel.getMethodName();
        this.createTime = Date.from(instant);
        this.updateTime = this.createTime;
        if (0 != retryInfoModel.getPriority()) {
            this.priority = retryInfoModel.getPriority();
        } else {
            this.priority = RetryPriorityEnum.LOW.getPriority();
        }
        this.status = RetryStatusEnum.RETRYING.getStatus();
        this.retryCount = 0;
        this.retryTime = Date.from(instant.plus(retryInfoModel.getRetryTime(), ChronoUnit.SECONDS));
        this.reqParams = retryInfoModel.getReqParams();
        if (retryInfoModel.getMaxRetryCount() != 0) {
            this.maxRetryCount = retryInfoModel.getMaxRetryCount();
        } else {
            this.maxRetryCount = 99;
        }
        if (StringUtils.isBlank(retryInfoModel.getFalloffType())) {
            this.falloffType = DecayTypeEnum.NO.getType();
        } else {
            this.falloffType = retryInfoModel.getFalloffType();
        }

        if (0 == retryInfoModel.getFalloffInterval()) {
            this.falloffInterval = 60;
        } else {
            this.falloffInterval = retryInfoModel.getFalloffInterval();
        }
    }
}
















