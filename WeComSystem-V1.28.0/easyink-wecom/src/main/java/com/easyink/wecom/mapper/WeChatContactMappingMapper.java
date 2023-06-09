package com.easyink.wecom.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyink.wecom.domain.WeChatContactMapping;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天关系映射Mapper接口
 *
 * @author admin
 * @date 2020-12-27
 */
@Repository
public interface WeChatContactMappingMapper extends BaseMapper<WeChatContactMapping> {
    /**
     * 查询聊天关系映射
     *
     * @param id 聊天关系映射ID
     * @return 聊天关系映射
     */
    WeChatContactMapping selectWeChatContactMappingById(Long id);

    /**
     * 查询聊天关系映射列表
     *
     * @param weChatContactMapping 聊天关系映射
     * @return 聊天关系映射集合
     */
    List<WeChatContactMapping> selectWeChatContactMappingList(WeChatContactMapping weChatContactMapping);

    /**
     * 新增聊天关系映射
     *
     * @param weChatContactMapping 聊天关系映射
     * @return 结果
     */
    int insertWeChatContactMapping(WeChatContactMapping weChatContactMapping);

    /**
     * 修改聊天关系映射
     *
     * @param weChatContactMapping 聊天关系映射
     * @return 结果
     */
    int updateWeChatContactMapping(WeChatContactMapping weChatContactMapping);

    /**
     * 删除聊天关系映射
     *
     * @param id 聊天关系映射ID
     * @return 结果
     */
    int deleteWeChatContactMappingById(Long id);

    /**
     * 批量删除聊天关系映射
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteWeChatContactMappingByIds(Long[] ids);
}
