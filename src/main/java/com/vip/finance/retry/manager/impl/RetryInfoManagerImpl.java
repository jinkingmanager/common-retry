package com.vip.finance.retry.manager.impl;

import com.vip.finance.retry.entity.RetryInfo;
import com.vip.finance.retry.entity.RetryRecord;
import com.vip.finance.retry.enums.FalloffTypeEnum;
import com.vip.finance.retry.enums.OperateTypeEnum;
import com.vip.finance.retry.enums.RetryStatusEnum;
import com.vip.finance.retry.manager.RetryInfoMananger;
import com.vip.finance.retry.model.PauseRetryModel;
import com.vip.finance.retry.model.RetryInfoModel;
import com.vip.finance.retry.model.RetryResult;
import com.vip.finance.retry.repository.RetryInfoRepository;
import com.vip.finance.retry.repository.RetryRecordRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Component("retryInfoManager")
public class RetryInfoManagerImpl implements RetryInfoMananger {

    @Autowired
    private RetryInfoRepository retryInfoRepository;

    @Autowired
    private RetryRecordRepository retryRecordRepository;

    @Override
    @Transactional
    public RetryResult saveRetryInfo(RetryInfoModel retryInfoModel) {

        RetryResult result = new RetryResult();

        // 构造重试数据
        RetryInfo retryInfo = new RetryInfo(retryInfoModel);

        int insertResult = retryInfoRepository.insert(retryInfo);

        if (insertResult != 1) {
            result.setSucc(false);
            result.setMessage("保存重试信息异常");
        }

        return result;
    }

    @Override
    public RetryResult queryRetryList() {

        RetryResult result = new RetryResult();

        // 查询待重试的数据，按照priority、重试时间、失败次数来排序
        List<RetryInfo> list = retryInfoRepository.selectRetryingData();

        result.setRetryInfoList(list);

        return result;
    }

    @Override
    @Transactional
    public RetryResult addRetryCount(int id, String failReason) {

        RetryResult retryResult = new RetryResult();

        RetryInfo retryInfo = retryInfoRepository.selectByPrimaryKey(id);
        if (null == retryInfo) {
            retryResult.setSucc(false);
            retryResult.setMessage("重试信息不存在");
            return retryResult;
        }

        if (retryInfo.getRetryCount() > retryInfo.getMaxRetryCount()) {
            retryResult.setSucc(false);
            retryResult.setMessage("已超出最大重试次数");
            return retryResult;
        }

        // 插入对应的重试记录
        RetryRecord retryRecord = new RetryRecord();

        retryRecord.setBizId(retryInfo.getBizId());
        retryRecord.setCreateTime(Date.from(Instant.now()));
        retryRecord.setReason(StringUtils.substring(failReason,0,255));
        retryRecord.setRetryInfoId(id);
        retryRecord.setOperateType(OperateTypeEnum.RETRY.getType());

        retryRecordRepository.insert(retryRecord);


        // 更新重试触发时间
        // 恒定时间间隔
        Date newRetryTime = null;
        if (StringUtils.equalsIgnoreCase(FalloffTypeEnum.AVERAGE.getType(), retryInfo.getFalloffType())) {
            newRetryTime = Date.from(Instant.ofEpochMilli(retryInfo.getRetryTime().getTime())
                    .plus(retryInfo.getFalloffInterval(), ChronoUnit.SECONDS));
        }
        // 递增衰减
        else if (StringUtils.equalsIgnoreCase(FalloffTypeEnum.INCREASE.getType(), retryInfo.getFalloffType())) {
            newRetryTime = Date.from(Instant.ofEpochMilli(
                    retryInfo.getRetryTime().getTime()).plus(retryInfo.getRetryCount() * retryInfo.getFalloffInterval(),
                    ChronoUnit.SECONDS));
        }
        // 更新次数
        int result = retryInfoRepository.addRetryCount(id, newRetryTime);


        if (result != 1) {
            retryResult.setSucc(false);
            retryResult.setMessage("更新次数异常");
        }

        return retryResult;
    }

    @Override
    @Transactional
    public RetryResult retrySucc(int id) {
        RetryResult retryResult = new RetryResult();

        RetryInfo retryInfo = retryInfoRepository.selectByPrimaryKey(id);
        if (null == retryInfo) {
            retryResult.setSucc(false);
            retryResult.setMessage("重试信息不存在");
            return retryResult;
        }
        // 插入对应的重试记录
        RetryRecord retryRecord = new RetryRecord();

        retryRecord.setCreateTime(Date.from(Instant.now()));
        retryRecord.setReason("SUCC");
        retryRecord.setRetryInfoId(id);
        retryRecord.setBizId(retryInfo.getBizId());
        retryRecord.setOperateType(OperateTypeEnum.SUCC.getType());

        retryRecordRepository.insert(retryRecord);

        // 更新次数
        int result = retryInfoRepository.retrySucc(id);


        if (result != 1) {
            retryResult.setSucc(false);
            retryResult.setMessage("更新重试状态异常");
        }

        return retryResult;
    }

    @Transactional
    @Override
    public RetryResult pause(PauseRetryModel pauseRetryModel) {

        RetryInfo retryInfo = retryInfoRepository.selectByIds(pauseRetryModel.getId(), pauseRetryModel.getBizId());

        RetryResult result = new RetryResult();

        if (null == retryInfo) {
            result.setSucc(false);
            result.setMessage("cannot find retryInfo using pauseRetryModel :" + pauseRetryModel.toString());

            return result;
        }


        // 已成功的 不能再暂停
        if(RetryStatusEnum.isIn(retryInfo.getStatus(),RetryStatusEnum.SUCC)){
            result.setSucc(false);
            result.setMessage("Succ retryInfo can not be paused!");

            return result;
        }

        // 已暂停的，直接返回成功
        if(RetryStatusEnum.isIn(retryInfo.getStatus(),RetryStatusEnum.PAUSE)){

            result.setSucc(true);
            result.setMessage("idempotent deal!");

            return result;
        }

        int count = retryInfoRepository.pause(pauseRetryModel.getId(),pauseRetryModel.getBizId());

        if(count != 1){
            result.setMessage("pause action effect rows error!");
            result.setSucc(false);

            return result;
        }

        result.setSucc(true);
        result.setEffectRows(count);

        // 处理record
        RetryRecord retryRecord = new RetryRecord();
        retryRecord.setOperateType(OperateTypeEnum.PAUSE.getType());
        retryRecord.setBizId(retryInfo.getBizId());
        retryRecord.setReason("PAUSE");
        retryRecord.setRetryInfoId(retryInfo.getId());
        retryRecord.setCreateTime(Date.from(Instant.now()));

        retryRecordRepository.insert(retryRecord);

        return result;
    }

    @Transactional
    @Override
    public RetryResult resume(PauseRetryModel resumeRetryModel) {

        RetryInfo retryInfo = retryInfoRepository.selectByIds(resumeRetryModel.getId(), resumeRetryModel.getBizId());

        RetryResult result = new RetryResult();

        if (null == retryInfo) {
            result.setSucc(false);
            result.setMessage("cannot find retryInfo using resumeRetryModel :" + resumeRetryModel.toString());

            return result;
        }


        // 已成功的 不能再resume
        if(RetryStatusEnum.isIn(retryInfo.getStatus(),RetryStatusEnum.SUCC)){
            result.setSucc(false);
            result.setMessage("Succ retryInfo can not be resumed!");

            return result;
        }

        // 已resume的，直接返回成功
        if(RetryStatusEnum.isIn(retryInfo.getStatus(),RetryStatusEnum.RETRYING)){

            result.setSucc(true);
            result.setMessage("idempotent deal!");

            return result;
        }

        int count = retryInfoRepository.resume(resumeRetryModel.getId(),resumeRetryModel.getBizId());

        if(count != 1){
            result.setMessage("resume action effect rows error!");
            result.setSucc(false);

            return result;
        }

        result.setSucc(true);
        result.setEffectRows(count);

        // 处理record
        RetryRecord retryRecord = new RetryRecord();
        retryRecord.setOperateType(OperateTypeEnum.RESUME.getType());
        retryRecord.setBizId(retryInfo.getBizId());
        retryRecord.setReason("RESUME");
        retryRecord.setRetryInfoId(retryInfo.getId());
        retryRecord.setCreateTime(Date.from(Instant.now()));

        retryRecordRepository.insert(retryRecord);

        return result;
    }


}
