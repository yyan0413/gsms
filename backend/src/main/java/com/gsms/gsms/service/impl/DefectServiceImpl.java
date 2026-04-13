package com.gsms.gsms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gsms.gsms.dto.defect.DefectCreateReq;
import com.gsms.gsms.dto.defect.DefectInfoResp;
import com.gsms.gsms.dto.defect.DefectQueryReq;
import com.gsms.gsms.dto.defect.DefectUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.exception.BusinessException;
import com.gsms.gsms.model.entity.Attachment;
import com.gsms.gsms.model.entity.Defect;
import com.gsms.gsms.model.entity.ProjectMember;
import com.gsms.gsms.model.entity.Task;
import com.gsms.gsms.model.entity.User;
import com.gsms.gsms.model.enums.DefectSeverity;
import com.gsms.gsms.model.enums.DefectStatus;
import com.gsms.gsms.model.enums.ProjectMemberRole;
import com.gsms.gsms.model.enums.errorcode.DefectErrorCode;
import com.gsms.gsms.repository.AttachmentMapper;
import com.gsms.gsms.repository.DefectMapper;
import com.gsms.gsms.repository.ProjectMemberMapper;
import com.gsms.gsms.repository.TaskMapper;
import com.gsms.gsms.repository.UserMapper;
import com.gsms.gsms.service.DefectService;
import com.gsms.gsms.service.storage.StorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 缺陷服务实现
 */
@Service
public class DefectServiceImpl implements DefectService {

    private static final Logger logger = LoggerFactory.getLogger(DefectServiceImpl.class);

    @Autowired
    private DefectMapper defectMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private StorageService storageService;

    @Value("${attachment.storage.type:local}")
    private String storageType;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public DefectInfoResp create(DefectCreateReq req, Long currentUserId) {
        // 1. 验证父任务（需求）存在性
        Task parentTask = taskMapper.selectById(req.getParentTaskId());
        if (parentTask == null || parentTask.getIsDeleted() == 1) {
            throw new BusinessException(DefectErrorCode.DEFECT_PARENT_TASK_NOT_FOUND);
        }

        // 2. 验证是否为父任务（不能是子任务）
        if (parentTask.getParentId() != null && parentTask.getParentId() != 0) {
            throw new BusinessException(DefectErrorCode.DEFECT_PARENT_TASK_INVALID);
        }

        // 3. 验证责任人是否为项目成员（如果指定了责任人）
        if (req.getAssigneeId() != null) {
            List<Long> memberIds = projectMemberMapper.selectProjectIdsByUserId(req.getAssigneeId());
            if (!memberIds.contains(parentTask.getProjectId())) {
                throw new BusinessException(DefectErrorCode.DEFECT_ASSIGNEE_NOT_MEMBER);
            }
        }

        // 4. 处理截图上传（如果有）
        String screenshotUrl = null;
        if (req.getScreenshotId() != null) {
            Attachment attachment = attachmentMapper.selectById(req.getScreenshotId());
            if (attachment != null && attachment.getIsDeleted() == 0) {
                screenshotUrl = storageService.getUrl(attachment.getFilePath());
            }
        }

        // 5. 创建缺陷
        Defect defect = new Defect();
        defect.setProjectId(parentTask.getProjectId());
        defect.setParentTaskId(req.getParentTaskId());
        defect.setDescription(req.getDescription());
        defect.setAssigneeId(req.getAssigneeId());
        defect.setSeverity(req.getSeverity());
        defect.setStatus(DefectStatus.PENDING.getCode()); // 默认状态：未整改
        defect.setScreenshotUrl(screenshotUrl);
        defect.setCreatorId(currentUserId);
        defect.setCreateTime(LocalDateTime.now());
        defect.setUpdateTime(LocalDateTime.now());
        defect.setIsDeleted(0);

        defectMapper.insert(defect);

        logger.info("用户 {} 创建缺陷成功: parentId={}, severity={}", currentUserId, req.getParentTaskId(), req.getSeverity());

        return convertToInfoResp(defect);
    }

    @Override
    @Transactional
    public DefectInfoResp update(DefectUpdateReq req, Long currentUserId) {
        // 1. 查询缺陷
        Defect defect = defectMapper.selectById(req.getId());
        if (defect == null || defect.getIsDeleted() == 1) {
            throw new BusinessException(DefectErrorCode.DEFECT_NOT_FOUND);
        }

        // 2. 验证项目访问权限
        validateProjectAccess(defect.getProjectId(), currentUserId);

        // 3. 验证是否为项目经理
        if (!isProjectManager(defect.getProjectId(), currentUserId)) {
            throw new BusinessException(DefectErrorCode.DEFECT_NO_PERMISSION);
        }

        // 4. 验证父任务（如果修改了）
        if (req.getParentTaskId() != null && !req.getParentTaskId().equals(defect.getParentTaskId())) {
            Task parentTask = taskMapper.selectById(req.getParentTaskId());
            if (parentTask == null || parentTask.getIsDeleted() == 1) {
                throw new BusinessException(DefectErrorCode.DEFECT_PARENT_TASK_NOT_FOUND);
            }
            if (parentTask.getParentId() != null && parentTask.getParentId() != 0) {
                throw new BusinessException(DefectErrorCode.DEFECT_PARENT_TASK_INVALID);
            }
            defect.setParentTaskId(req.getParentTaskId());
        }

        // 5. 验证责任人（如果修改了）
        if (req.getAssigneeId() != null) {
            List<Long> memberIds = projectMemberMapper.selectProjectIdsByUserId(req.getAssigneeId());
            if (!memberIds.contains(defect.getProjectId())) {
                throw new BusinessException(DefectErrorCode.DEFECT_ASSIGNEE_NOT_MEMBER);
            }
            defect.setAssigneeId(req.getAssigneeId());
        }

        // 6. 更新字段
        if (req.getDescription() != null) {
            defect.setDescription(req.getDescription());
        }
        if (req.getSeverity() != null) {
            defect.setSeverity(req.getSeverity());
        }
        if (req.getScreenshotId() != null) {
            Attachment attachment = attachmentMapper.selectById(req.getScreenshotId());
            if (attachment != null && attachment.getIsDeleted() == 0) {
                defect.setScreenshotUrl(storageService.getUrl(attachment.getFilePath()));
            }
        }
        if (req.getStatus() != null) {
            defect.setStatus(req.getStatus());
        }

        defect.setUpdateUserId(currentUserId);
        defect.setUpdateTime(LocalDateTime.now());

        defectMapper.updateById(defect);

        logger.info("用户 {} 更新缺陷成功: id={}", currentUserId, req.getId());

        return convertToInfoResp(defect);
    }

    @Override
    @Transactional
    public void delete(Long id, Long currentUserId) {
        // 1. 查询缺陷
        Defect defect = defectMapper.selectById(id);
        if (defect == null || defect.getIsDeleted() == 1) {
            throw new BusinessException(DefectErrorCode.DEFECT_NOT_FOUND);
        }

        // 2. 验证项目访问权限
        validateProjectAccess(defect.getProjectId(), currentUserId);

        // 3. 验证是否为项目经理
        if (!isProjectManager(defect.getProjectId(), currentUserId)) {
            throw new BusinessException(DefectErrorCode.DEFECT_NO_PERMISSION);
        }

        // 4. 软删除
        defect.setIsDeleted(1);
        defect.setUpdateTime(LocalDateTime.now());
        defect.setUpdateUserId(currentUserId);
        defectMapper.updateById(defect);

        logger.info("用户 {} 删除缺陷成功: id={}", currentUserId, id);
    }

    @Override
    @Transactional
    public void close(Long id, Long currentUserId) {
        // 1. 查询缺陷
        Defect defect = defectMapper.selectById(id);
        if (defect == null || defect.getIsDeleted() == 1) {
            throw new BusinessException(DefectErrorCode.DEFECT_NOT_FOUND);
        }

        // 2. 验证项目访问权限
        validateProjectAccess(defect.getProjectId(), currentUserId);

        // 3. 验证是否为项目经理
        if (!isProjectManager(defect.getProjectId(), currentUserId)) {
            throw new BusinessException(DefectErrorCode.DEFECT_NO_PERMISSION);
        }

        // 4. 更新状态为已办结
        defect.setStatus(DefectStatus.CLOSED.getCode());
        defect.setUpdateTime(LocalDateTime.now());
        defect.setUpdateUserId(currentUserId);
        defectMapper.updateById(defect);

        logger.info("用户 {} 办结缺陷成功: id={}", currentUserId, id);
    }

    @Override
    public DefectInfoResp getDetail(Long id, Long currentUserId) {
        // 1. 查询缺陷
        Defect defect = defectMapper.selectById(id);
        if (defect == null || defect.getIsDeleted() == 1) {
            throw new BusinessException(DefectErrorCode.DEFECT_NOT_FOUND);
        }

        // 2. 验证项目访问权限
        validateProjectAccess(defect.getProjectId(), currentUserId);

        return convertToInfoResp(defect);
    }

    @Override
    public PageResult<DefectInfoResp> list(DefectQueryReq req, Long currentUserId) {
        // 1. 验证项目访问权限
        validateProjectAccess(req.getProjectId(), currentUserId);

        // 2. 构建查询条件
        LambdaQueryWrapper<Defect> queryWrapper = Wrappers.<Defect>lambdaQuery()
                .eq(Defect::getProjectId, req.getProjectId())
                .eq(Defect::getIsDeleted, 0);

        // 责任人筛选
        if (req.getAssigneeIds() != null && !req.getAssigneeIds().isEmpty()) {
            queryWrapper.in(Defect::getAssigneeId, req.getAssigneeIds());
        }

        // 创建时间筛选
        if (req.getCreateTimeStart() != null) {
            queryWrapper.ge(Defect::getCreateTime, req.getCreateTimeStart());
        }
        if (req.getCreateTimeEnd() != null) {
            queryWrapper.le(Defect::getCreateTime, req.getCreateTimeEnd());
        }

        // 缺陷等级筛选
        if (req.getSeverities() != null && !req.getSeverities().isEmpty()) {
            queryWrapper.in(Defect::getSeverity, req.getSeverities());
        }

        // 需求筛选
        if (req.getParentTaskIds() != null && !req.getParentTaskIds().isEmpty()) {
            queryWrapper.in(Defect::getParentTaskId, req.getParentTaskIds());
        }

        // 状态筛选
        if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
            queryWrapper.in(Defect::getStatus, req.getStatuses());
        }

        // 3. 分页查询
        com.github.pagehelper.PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<Defect> defects = defectMapper.selectList(queryWrapper);
        com.github.pagehelper.Page<Defect> page = (com.github.pagehelper.Page<Defect>) defects;

        // 4. 转换为响应 DTO
        List<DefectInfoResp> respList = defects.stream()
                .map(this::convertToInfoResp)
                .collect(Collectors.toList());

        return PageResult.success(respList, page.getTotal(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public void export(DefectQueryReq req, Long currentUserId, HttpServletResponse response) {
        // 1. 验证项目访问权限
        validateProjectAccess(req.getProjectId(), currentUserId);

        // 2. 查询所有符合条件的缺陷（不分页）
        req.setPageNum(1);
        req.setPageSize(Integer.MAX_VALUE);
        PageResult<DefectInfoResp> pageResult = list(req, currentUserId);
        List<DefectInfoResp> defects = pageResult.getData();

        // 3. 生成 Excel 文件
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("缺陷列表");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("缺陷ID");
            headerRow.createCell(1).setCellValue("需求标题");
            headerRow.createCell(2).setCellValue("缺陷描述");
            headerRow.createCell(3).setCellValue("缺陷等级");
            headerRow.createCell(4).setCellValue("责任人");
            headerRow.createCell(5).setCellValue("状态");
            headerRow.createCell(6).setCellValue("创建时间");
            headerRow.createCell(7).setCellValue("创建人");

            // 填充数据
            int rowNum = 1;
            for (DefectInfoResp defect : defects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(defect.getId());
                row.createCell(1).setCellValue(defect.getParentTaskTitle());
                row.createCell(2).setCellValue(defect.getDescription());
                row.createCell(3).setCellValue(defect.getSeverityName());
                row.createCell(4).setCellValue(defect.getAssigneeName() != null ? defect.getAssigneeName() : "");
                row.createCell(5).setCellValue(defect.getStatusName());
                row.createCell(6).setCellValue(defect.getCreateTime() != null ?
                    defect.getCreateTime().format(DATE_FORMATTER) : "");
                row.createCell(7).setCellValue(defect.getCreatorName());
            }

            // 4. 设置响应头
            String fileName = "缺陷列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));

            // 5. 写入响应流
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            logger.info("用户 {} 导出缺陷列表成功: projectId={}, count={}", currentUserId, req.getProjectId(), defects.size());
        } catch (IOException e) {
            logger.error("导出缺陷列表失败", e);
            throw new RuntimeException("导出缺陷列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证项目访问权限
     */
    private void validateProjectAccess(Long projectId, Long userId) {
        List<Long> projectIds = projectMemberMapper.selectProjectIdsByUserId(userId);
        if (!projectIds.contains(projectId)) {
            throw new BusinessException(DefectErrorCode.DEFECT_NO_PERMISSION);
        }
    }

    /**
     * 检查用户是否为项目经理
     */
    private boolean isProjectManager(Long projectId, Long userId) {
        List<ProjectMember> members = projectMemberMapper.selectMembersByProjectId(projectId);
        return members.stream()
                .anyMatch(m -> m.getUserId().equals(userId) &&
                        m.getRoleType().equals(ProjectMemberRole.PROJECT_MANAGER.getCode()));
    }

    /**
     * 转换为响应 DTO
     */
    private DefectInfoResp convertToInfoResp(Defect defect) {
        DefectInfoResp resp = new DefectInfoResp();
        resp.setId(defect.getId());
        resp.setProjectId(defect.getProjectId());
        resp.setParentTaskId(defect.getParentTaskId());
        resp.setDescription(defect.getDescription());
        resp.setAssigneeId(defect.getAssigneeId());
        resp.setSeverity(defect.getSeverity());
        resp.setSeverityName(DefectSeverity.fromName(defect.getSeverity()).getName());
        resp.setStatus(defect.getStatus());
        resp.setStatusName(DefectStatus.fromCode(defect.getStatus()).getName());
        resp.setScreenshotUrl(defect.getScreenshotUrl());
        resp.setCreatorId(defect.getCreatorId());
        resp.setCreateTime(defect.getCreateTime());
        resp.setUpdateTime(defect.getUpdateTime());

        // 填充需求标题
        Task parentTask = taskMapper.selectById(defect.getParentTaskId());
        if (parentTask != null && parentTask.getIsDeleted() == 0) {
            resp.setParentTaskTitle(parentTask.getTitle());
        }

        // 填充责任人姓名
        if (defect.getAssigneeId() != null) {
            User assignee = userMapper.selectById(defect.getAssigneeId());
            if (assignee != null) {
                resp.setAssigneeName(assignee.getUsername());
            }
        }

        // 填充创建人姓名
        User creator = userMapper.selectById(defect.getCreatorId());
        if (creator != null) {
            resp.setCreatorName(creator.getUsername());
        }

        // 计算操作权限（仅项目经理可操作）
        boolean isManager = isProjectManager(defect.getProjectId(), defect.getCreatorId());
        resp.setCanEdit(isManager);
        resp.setCanDelete(isManager);
        resp.setCanClose(isManager);

        return resp;
    }
}
