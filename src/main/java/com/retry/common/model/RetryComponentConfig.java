package com.retry.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * retry config
 * 在代码中retry时，需要设置的数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryComponentConfig {

    // 重试次数
    private int retryTimes = 3;

    // 重试间隔时间
    private int duration = 5;

    // 间隔时间单位，默认为秒
    private TemporalUnit durationUnit = ChronoUnit.SECONDS;

}
