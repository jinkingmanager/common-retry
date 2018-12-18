package com.retry.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class RetryRecord {

    private Integer id;
    private String bizId;
    private String operateType;
    private Integer retryInfoId;
    private String reason;
    private Date createTime;
}
















