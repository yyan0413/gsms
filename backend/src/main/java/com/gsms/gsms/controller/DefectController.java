package com.gsms.gsms.controller;

import com.gsms.gsms.dto.defect.DefectCreateReq;
import com.gsms.gsms.dto.defect.DefectInfoResp;
import com.gsms.gsms.dto.defect.DefectQueryReq;
import com.gsms.gsms.dto.defect.DefectUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.service.DefectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 缺陷管理控制器
 */
@RestController
@RequestMapping("/api/projects/{projectId}/defects")
@Tag(name = "缺陷管理")
public class DefectController {

    private static final Logger logger = LoggerFactory.getLogger(DefectController.class);

    @Autowired
    private DefectService defectService;

    @PostMapping
    @Operation(summary = "创建缺陷")
    public Result<DefectInfoResp> create(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody DefectCreateReq req) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        logger.info("创建缺陷请求: projectId={}, parentTaskId={}, severity={}",
                projectId, req.getParentTaskId(), req.getSeverity());
        DefectInfoResp defect = defectService.create(req, currentUserId);
        return Result.success(defect);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新缺陷")
    public Result<DefectInfoResp> update(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "缺陷ID", required = true) @PathVariable Long id,
            @Valid @RequestBody DefectUpdateReq req) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        logger.info("更新缺陷请求: projectId={}, defectId={}", projectId, id);
        req.setId(id);
        DefectInfoResp defect = defectService.update(req, currentUserId);
        return Result.success(defect);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除缺陷")
    public Result<Void> delete(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "缺陷ID", required = true) @PathVariable Long id) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        logger.info("删除缺陷请求: projectId={}, defectId={}", projectId, id);
        defectService.delete(id, currentUserId);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "办结缺陷")
    public Result<Void> close(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "缺陷ID", required = true) @PathVariable Long id) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        logger.info("办结缺陷请求: projectId={}, defectId={}", projectId, id);
        defectService.close(id, currentUserId);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取缺陷详情")
    public Result<DefectInfoResp> getDetail(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "缺陷ID", required = true) @PathVariable Long id) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        DefectInfoResp defect = defectService.getDetail(id, currentUserId);
        return Result.success(defect);
    }

    @GetMapping
    @Operation(summary = "查询缺陷列表")
    public Result<PageResult<DefectInfoResp>> list(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            DefectQueryReq req) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        req.setProjectId(projectId);
        PageResult<DefectInfoResp> result = defectService.list(req, currentUserId);
        return Result.success(result);
    }

    @GetMapping("/export")
    @Operation(summary = "导出缺陷列表")
    public void export(
            @Parameter(description = "项目ID", required = true) @PathVariable Long projectId,
            DefectQueryReq req,
            HttpServletResponse response) {
        Long currentUserId = com.gsms.gsms.infra.utils.UserContext.getCurrentUserId();
        req.setProjectId(projectId);
        logger.info("导出缺陷列表请求: projectId={}", projectId);
        defectService.export(req, currentUserId, response);
    }
}
