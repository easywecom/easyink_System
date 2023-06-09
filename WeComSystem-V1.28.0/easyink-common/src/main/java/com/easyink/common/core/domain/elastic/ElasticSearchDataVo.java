package com.easyink.common.core.domain.elastic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author admin
 * @description http交互Vo对象
 * @date 2020/12/9 14:14
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElasticSearchDataVo {
    /**
     * 索引名
     */
    private String idxName;
    /**
     * 数据存储对象
     */
    private ElasticSearchEntity elasticSearchEntity;
}
