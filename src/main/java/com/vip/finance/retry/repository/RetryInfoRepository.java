package com.vip.finance.retry.repository;

import com.vip.finance.retry.entity.RetryInfo;
import com.vip.finance.retry.model.PauseRetryModel;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RetryInfoRepository {

    RetryInfo selectByPrimaryKey(Integer id);

    RetryInfo selectByIds(@Param("id") Integer id, @Param("bizId") String bizId);

    List<RetryInfo> selectRetryingData();

    int addRetryCount(@Param("id") int id, @Param("newRetryTime") Date newRetryTime);

    int retrySucc(int id);

    int insert(RetryInfo retryInfo);

    int pause(@Param("id") Integer id, @Param("bizId") String bizId);

    int resume(@Param("id") Integer id, @Param("bizId") String bizId);

    List<RetryInfo> selectByParams(@Param("id") Integer retryInfoId, @Param("bizId") String bizId,
                                   @Param("status") String retryInfoStatus);

}