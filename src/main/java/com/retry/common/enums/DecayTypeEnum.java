package com.retry.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DecayTypeEnum {

    NO("no"), // 不需要衰减

    INCREASE("increase"), // 递增衰减

    AVERAGE("average") // 间隔恒定
    ;
    private String type;
}
