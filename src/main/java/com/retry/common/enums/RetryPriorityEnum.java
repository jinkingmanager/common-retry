package com.retry.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum RetryPriorityEnum {

    LOW(1),

    MIDDLE(2),

    HIGH(3);

    private int priority;

}
