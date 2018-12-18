package com.retry.common.model;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueryRetryModel extends RetryBaseModel {

    private Integer retryInfoId;

    private String bizId;

    private String retryInfoStatus;

    @Override
    public void check() {
        Preconditions.checkArgument(
                retryInfoId == null && StringUtils.isBlank(bizId),
                "retryInfoId or bizId cannot be null at the same time!");
    }
}
