package com.gsms.gsms.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 缺陷等级枚举
 */
public enum DefectSeverity {
    FATAL(1, "致命"),
    SERIOUS(2, "严重"),
    NORMAL(3, "一般");

    @EnumValue
    private final Integer code;
    private final String name;

    DefectSeverity(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonValue
    public String toString() {
        return name();
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据代码获取枚举
     */
    public static DefectSeverity fromCode(Integer code) {
        if (code == null) {
            return NORMAL;
        }
        for (DefectSeverity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        return NORMAL;
    }

    /**
     * 根据名称获取枚举
     */
    public static DefectSeverity fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NORMAL;
        }
        for (DefectSeverity severity : values()) {
            if (severity.name().equalsIgnoreCase(name)) {
                return severity;
            }
        }
        return NORMAL;
    }
}
