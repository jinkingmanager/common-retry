package com.vip.finance.retry.manager;

import com.vip.finance.retry.model.PauseRetryModel;
import com.vip.finance.retry.model.QueryRetryModel;
import com.vip.finance.retry.model.RetryResult;

/**
 * 用于retry时做各种重试实现的分发
 */
public interface RetryFacade {

    /**
     * 重试分发，会扫描数据，按照优先级及时间处理
     * @return
     */
    String retryDispatch();

    /**
     * 暂停指定的重试记录，如果已经是暂停状态，幂等
     * 如果已经是成功状态，不允许重试
     * @param pauseRetryModel
     * @return
     */
    RetryResult pauseRetry(PauseRetryModel pauseRetryModel);

    /**
     * 恢复指定的重试记录，如果不是暂停状态，则不允许resume
     * @param resumeRetryModel
     * @return
     */
    RetryResult resumeRetry(PauseRetryModel resumeRetryModel);

    /**
     * 根据id bizId status查询重试列表
     * @param queryRetryInfoModel
     * @return
     */
    RetryResult queryRetryListByParams(QueryRetryModel queryRetryInfoModel);

    /**
     * 根据id bizId 查询重试记录列表
     * @param queryRetryInfoModel
     * @return
     */
    RetryResult queryRetryRecordByParams(QueryRetryModel queryRetryInfoModel);
}
