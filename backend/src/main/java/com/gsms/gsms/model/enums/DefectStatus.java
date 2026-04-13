package com.gsms.gsms.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 缺陷状态枚举
 */
public enum DefectStatus {
    PENDING("PENDING", "未整改"),
    FIXED("FIXED", "已整改"),
    CLOSED("CLOSED", "已办结");

    @EnumValue
    private final String code;
    private final String name;

    DefectStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonValue
    public String toString() {
        return name();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据代码获取枚举
     */
    public static DefectStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return PENDING;
        }
        for (DefectStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return PENDING;
    }

    /**
     * 根据名称获取枚举
     */
    public static DefectStatus fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return PENDING;
        }
        for (DefectStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return PENDING;
    }
}
