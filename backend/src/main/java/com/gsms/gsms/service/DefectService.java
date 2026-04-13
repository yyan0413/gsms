package com.gsms.gsms.service;

import com.gsms.gsms.dto.defect.DefectCreateReq;
import com.gsms.gsms.dto.defect.DefectInfoResp;
import com.gsms.gsms.dto.defect.DefectQueryReq;
import com.gsms.gsms.dto.defect.DefectUpdateReq;
import com.gsms.gsms.infra.common.PageResult;

import javax.servlet.http.HttpServletResponse;

/**
 * 缺陷服务接口
 */
public interface DefectService {

    /**
     * 创建缺陷
     */
    DefectInfoResp create(DefectCreateReq req, Long currentUserId);

    /**
     * 更新缺陷
     */
    DefectInfoResp update(DefectUpdateReq req, Long currentUserId);

    /**
     * 删除缺陷（软删除）
     */
    void delete(Long id, Long currentUserId);

    /**
     * 办结缺陷
     */
    void close(Long id, Long currentUserId);

    /**
     * 获取缺陷详情
     */
    DefectInfoResp getDetail(Long id, Long currentUserId);

    /**
     * 分页查询缺陷列表
     */
    PageResult<DefectInfoResp> list(DefectQueryReq req, Long currentUserId);

    /**
     * 导出缺陷列表
     */
    void export(DefectQueryReq req, Long currentUserId, HttpServletResponse response);
}
