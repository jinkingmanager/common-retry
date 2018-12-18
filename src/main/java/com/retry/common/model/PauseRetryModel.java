package com.retry.common.model;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PauseRetryModel extends RetryBaseModel {

    private Integer id; // 必填， 必须通过id来指定某一个重试数据，一个bizId可能对应多个重试数据（虽然理论上应该同时只有一个处于活动状态）

    private String bizId; // 业务ID

    @Override
    public void check() {
        Preconditions.checkNotNull(id, "Id can not be null!");
        Preconditions.checkNotNull(bizId, "bizId can not be null!");
    }
}
