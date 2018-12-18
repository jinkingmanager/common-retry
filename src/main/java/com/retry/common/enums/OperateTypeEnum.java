package com.retry.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum OperateTypeEnum {

    RETRY("retry"),

    PAUSE("pause"),

    RESUME("resume"),

    SUCC("succ"),
    ;

    private String type;
}
