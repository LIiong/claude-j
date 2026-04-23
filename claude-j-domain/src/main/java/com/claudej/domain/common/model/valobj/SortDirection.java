package com.claudej.domain.common.model.valobj;

import lombok.Getter;

/**
 * 排序方向枚举 - 分页查询的通用值对象
 */
@Getter
public enum SortDirection {

    ASC("ASC", "升序"),
    DESC("DESC", "降序");

    private final String value;
    private final String description;

    SortDirection(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 从字符串解析排序方向
     * 不区分大小写，无效值返回默认 ASC
     *
     * @param value 字符串值
     * @return 排序方向枚举
     */
    public static SortDirection fromString(String value) {
        if (value == null) {
            return ASC;
        }
        String upperValue = value.toUpperCase();
        for (SortDirection direction : values()) {
            if (direction.value.equals(upperValue)) {
                return direction;
            }
        }
        return ASC;
    }
}