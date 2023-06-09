package com.easyink.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyink.wecom.domain.entity.WeExternalUseridMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 外部联系人externalUserId明密文映射表(WeExternalUseridMapping)表数据库访问层
 *
 * @author wx
 * @since 2023-03-14 18:01:41
 */
@Mapper
public interface WeExternalUseridMappingMapper extends BaseMapper<WeExternalUseridMapping> {

    /**
     * 新增或更新映射关系
     *
     * @param weExternalUseridMapping {@link WeExternalUseridMapping}
     */
    void insertOrUpdate(WeExternalUseridMapping weExternalUseridMapping);

    /**
     * 批量插入或者更新
     *
     * @param mappings {@link WeExternalUseridMapping}
     * @return affected rows
     */
    Integer batchInsertOrUpdate(@Param("list") List<WeExternalUseridMapping> mappings);
}

