package com.gsms.gsms.model.enums.errorcode;

import com.gsms.gsms.infra.exception.ErrorCode;

/**
 * 缺陷相关错误码
 */
public enum DefectErrorCode implements ErrorCode {

    DEFECT_NOT_FOUND(2401, "缺陷不存在"),
    DEFECT_PARENT_TASK_NOT_FOUND(2402, "需求不存在"),
    DEFECT_PARENT_TASK_INVALID(2403, "只能选择需求（父任务）"),
    DEFECT_ASSIGNEE_NOT_MEMBER(2404, "责任人不是项目成员"),
    DEFECT_NO_PERMISSION(2405, "无权限操作该缺陷"),
    DEFECT_CREATE_FAILED(2406, "创建缺陷失败"),
    DEFECT_UPDATE_FAILED(2407, "更新缺陷失败"),
    DEFECT_DELETE_FAILED(2408, "删除缺陷失败"),
    DEFECT_EXPORT_FAILED(2409, "导出缺陷失败");

    private final Integer code;
    private final String message;

    DefectErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
