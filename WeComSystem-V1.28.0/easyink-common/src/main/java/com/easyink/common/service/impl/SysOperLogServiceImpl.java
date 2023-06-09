package com.easyink.common.service.impl;

import com.easyink.common.core.domain.system.SysOperLog;
import com.easyink.common.mapper.SysOperLogMapper;
import com.easyink.common.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 服务层处理
 *
 * @author admin
 */
@Service
public class SysOperLogServiceImpl implements ISysOperLogService {
    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    @Override
    public void insertOperlog(SysOperLog operLog) {
        operLogMapper.insertOperlog(operLog);
    }

    /**
     * 查询系统操作日志集合
     *
     * @param operLog 操作日志对象
     * @return 操作日志集合
     */
    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog operLog) {
        return operLogMapper.selectOperLogList(operLog);
    }

    /**
     * 批量删除系统操作日志
     *
     * @param corpId
     * @param operIds 需要删除的操作日志ID
     * @return 结果
     */
    @Override
    public int deleteOperLogByIds(String corpId, Long[] operIds) {
        return operLogMapper.deleteOperLogByIds(corpId, operIds);
    }


    /**
     * 清空操作日志
     * @param corpId
     */
    @Override
    public void cleanOperLog(String corpId) {
        operLogMapper.deleteOperLogByIds(corpId, null);
    }
}
