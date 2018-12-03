package com.vip.finance.retry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum RetryStatusEnum {

    RETRYING("retrying"), // 重试中

    PAUSE("pause"), // 暂停重试

    SUCC("succ"); // 重试成功

    private String status;

    public static boolean isIn(String status,RetryStatusEnum... enums){

        for (RetryStatusEnum statusEnum : enums){
            if(StringUtils.equalsIgnoreCase(status,statusEnum.getStatus())){
                return true;
            }
        }

        return false;
    }
}
