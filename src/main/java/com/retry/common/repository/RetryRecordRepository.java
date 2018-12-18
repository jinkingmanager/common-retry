package com.retry.common.repository;

import com.retry.common.entity.RetryRecord;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetryRecordRepository{

	RetryRecord selectByPrimaryKey(Integer id);
	
  	int insert(RetryRecord retryRecord);
  	
    List<RetryRecord> selectByBizKey(String bizId);

    List<RetryRecord> selectByParams(@Param("retryInfoId") Integer retryInfoId, @Param("bizId") String bizId);
}