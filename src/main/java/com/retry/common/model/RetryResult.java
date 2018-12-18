package com.retry.common.model;

import com.retry.common.entity.RetryInfo;
import com.retry.common.entity.RetryRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetryResult implements Serializable {

    private boolean succ = true;

    private String message;

    private Integer effectRows; // 受影响条数

    private List<RetryInfo> retryInfoList;

    private List<RetryRecord> retryRecordList;
}
