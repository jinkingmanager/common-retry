package com.vip.finance.retry.manager;

import com.vip.finance.retry.model.PauseRetryModel;
import com.vip.finance.retry.model.RetryInfoModel;
import com.vip.finance.retry.model.RetryResult;

public interface RetryInfoMananger {

    int DEFAULT_MAX_RETRY_COUNT = 99;

    /**
     * 保存重试数据
     *
     * @param retryInfoModel
     * @return
     */
    RetryResult saveRetryInfo(RetryInfoModel retryInfoModel);

    /**
     * 查询保存的retry数据
     *
     * @return
     */
    RetryResult queryRetryList();

    /**
     * 重试失败的时候增加次数,不会有外部调用，所以直接传入id
     * 如果有外部调用，则需要加入bizid做双因子验证
     *
     * @param id
     * @param failReason
     * @return
     */
    RetryResult addRetryCount(int id, String failReason);

    /**
     * 重试成功时更新重试数据,不会有外部调用，所以直接传入id
     * 如果有外部调用，则需要加入bizid做双因子验证
     *
     * @param id
     * @return
     */
    RetryResult retrySucc(int id);

    /**
     * 暂停重试
     *
     * @param pauseRetryModel
     * @return
     */
    RetryResult pause(PauseRetryModel pauseRetryModel);

    /**
     * 恢复重试
     *
     * @param resumeRetryModel
     * @return
     */
    RetryResult resume(PauseRetryModel resumeRetryModel);
}
