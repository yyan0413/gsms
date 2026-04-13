package com.gsms.gsms.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 缺陷创建请求 DTO
 */
@Schema(description = "缺陷创建请求")
public class DefectCreateReq {

    @Schema(description = "父任务ID（需求）", required = true)
    @NotNull(message = "需求不能为空")
    private Long parentTaskId;

    @Schema(description = "缺陷描述", required = true)
    @NotBlank(message = "缺陷描述不能为空")
    private String description;

    @Schema(description = "责任人ID（非必填）")
    private Long assigneeId;

    @Schema(description = "截图附件ID（非必填）")
    private Long screenshotId;

    @Schema(description = "缺陷等级", required = true)
    @NotNull(message = "缺陷等级不能为空")
    private String severity;

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
}
