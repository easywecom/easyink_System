package com.easyink.wecom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easyink.wecom.domain.entity.transfer.WeResignedGroupTransferRecord;
import com.easyink.wecom.mapper.WeResignedGroupTransferRecordMapper;
import com.easyink.wecom.service.WeResignedGroupTransferRecordService;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

/**
 * 类名: 离职客户群继承记录表业务层接口实现类
 *
 * @author : silver_chariot
 * @date : 2021/12/6 14:34
 */
@Service
public class WeResignedGroupTransferRecordServiceImpl extends ServiceImpl<WeResignedGroupTransferRecordMapper, WeResignedGroupTransferRecord> implements WeResignedGroupTransferRecordService {

    private final WeResignedGroupTransferRecordMapper weResignedGroupTransferRecordMapper;

    public WeResignedGroupTransferRecordServiceImpl(@NotNull WeResignedGroupTransferRecordMapper weResignedGroupTransferRecordMapper) {
        this.weResignedGroupTransferRecordMapper = weResignedGroupTransferRecordMapper;
    }

}
