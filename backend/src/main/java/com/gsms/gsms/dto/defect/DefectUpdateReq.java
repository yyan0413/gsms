package com.gsms.gsms.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * 缺陷更新请求 DTO
 */
@Schema(description = "缺陷更新请求")
public class DefectUpdateReq {

    @Schema(description = "缺陷ID", required = true)
    @NotNull(message = "缺陷ID不能为空")
    private Long id;

    @Schema(description = "父任务ID（需求）")
    private Long parentTaskId;

    @Schema(description = "缺陷描述")
    private String description;

    @Schema(description = "责任人ID")
    private Long assigneeId;

    @Schema(description = "截图附件ID")
    private Long screenshotId;

    @Schema(description = "缺陷等级")
    private String severity;

    @Schema(description = "状态（用于办结操作）")
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getScreenshotId() {
        return screenshotId;
    }

    public void setScreenshotId(Long screenshotId) {
        this.screenshotId = screenshotId;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
