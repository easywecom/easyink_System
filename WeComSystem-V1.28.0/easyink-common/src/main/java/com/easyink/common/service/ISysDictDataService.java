package com.easyink.common.service;

import com.easyink.common.core.domain.entity.SysDictData;

import java.util.List;

/**
 * 字典 业务层
 *
 * @author admin
 */
public interface ISysDictDataService {
    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    List<SysDictData> selectDictDataList(SysDictData dictData);


    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    SysDictData selectDictDataById(Long dictCode);

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     * @return 结果
     */
    int deleteDictDataByIds(Long[] dictCodes);

    /**
     * 新增保存字典数据信息
     *
     * @param dictData 字典数据信息
     * @return 结果
     */
    int insertDictData(SysDictData dictData);

    /**
     * 修改保存字典数据信息
     *
     * @param dictData 字典数据信息
     * @return 结果
     */
    int updateDictData(SysDictData dictData);
}
