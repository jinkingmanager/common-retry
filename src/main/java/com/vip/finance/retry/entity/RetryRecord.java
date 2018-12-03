package com.vip.finance.retry.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RetryRecord {

    private Integer id;
    private String bizId;
    private String operateType;
    private Integer retryInfoId;
    private String reason;
    private Date createTime;
}
















