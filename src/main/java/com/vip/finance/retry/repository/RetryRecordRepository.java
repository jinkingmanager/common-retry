package com.vip.finance.retry.repository;

import com.vip.finance.retry.entity.RetryRecord;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetryRecordRepository{

	RetryRecord selectByPrimaryKey(Integer id);
	
  	int insert(RetryRecord retryRecord);
  	
    List<RetryRecord> selectByBizKey(String bizId);

    List<RetryRecord> selectByParams(@Param("retryInfoId") Integer retryInfoId, @Param("bizId") String bizId);
}