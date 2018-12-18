package com.retry.common.manager.impl;

import com.retry.common.entity.RetryInfo;
import com.retry.common.entity.RetryRecord;
import com.retry.common.handler.RetryHandler;
import com.retry.common.manager.RetryFacade;
import com.retry.common.manager.RetryInfoMananger;
import com.retry.common.model.PauseRetryModel;
import com.retry.common.model.QueryRetryModel;
import com.retry.common.model.RetryInfoModel;
import com.retry.common.model.RetryResult;
import com.retry.common.repository.RetryInfoRepository;
import com.retry.common.repository.RetryRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("retryFacade")
public class RetryFacadeImpl implements RetryFacade {

    @Autowired
    private RetryInfoMananger retryInfoMananger;

    @Autowired
    private RetryRecordRepository retryRecordRepository;

    @Autowired
    private RetryInfoRepository retryInfoRepository;

    // 定义线程池
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(30,
            200,
            20,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(400),
            new CustomizableThreadFactory("-RetryThread-")
    );

    @Override
    public String retryDispatch() {

        // 查询数据
        RetryResult result = retryInfoMananger.queryRetryList();

        log.info("query retry info result: ", result);

        if (result.isSucc() && CollectionUtils.isNotEmpty(result.getRetryInfoList())) {
            log.info("query retry info list size:", result.getRetryInfoList().size());
            // 线程池处理
            for (RetryInfo retryInfo : result.getRetryInfoList()) {

                executor.execute(new RetryHandler(retryInfo, retryInfoMananger));
            }
        }

        return "succ";
    }

    @Override
    public RetryResult pauseRetry(PauseRetryModel pauseRetryModel) {

        pauseRetryModel.check();

        return retryInfoMananger.pause(pauseRetryModel);
    }

    @Override
    public RetryResult resumeRetry(PauseRetryModel resumeRetryModel) {

        resumeRetryModel.check();

        return retryInfoMananger.resume(resumeRetryModel);
    }

    @Override
    public RetryResult queryRetryListByParams(QueryRetryModel queryRetryInfoModel) {
        queryRetryInfoModel.check();

        RetryResult retryResult = new RetryResult();

        List<RetryInfo> retryInfoList = retryInfoRepository.selectByParams(queryRetryInfoModel.getRetryInfoId(),
                queryRetryInfoModel.getBizId(), queryRetryInfoModel.getRetryInfoStatus());

        retryResult.setEffectRows(retryInfoList.size());
        retryResult.setRetryInfoList(retryInfoList);

        return retryResult;
    }

    @Override
    public RetryResult queryRetryRecordByParams(QueryRetryModel queryRetryInfoModel) {

        queryRetryInfoModel.check();

        RetryResult result = new RetryResult();

        List<RetryRecord> retryRecordList = retryRecordRepository.selectByParams(queryRetryInfoModel.getRetryInfoId(),
                queryRetryInfoModel.getBizId());

        result.setRetryRecordList(retryRecordList);
        result.setEffectRows(retryRecordList.size());
        return result;
    }

    @Override
    public RetryResult saveRetryInfo(RetryInfoModel retryInfoModel) {
        return retryInfoMananger.saveRetryInfo(retryInfoModel);
    }


}
