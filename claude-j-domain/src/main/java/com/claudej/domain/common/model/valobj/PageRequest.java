package com.claudej.domain.common.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 分页请求值对象 - 分页查询的通用参数
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PageRequest {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final int page;
    private final int size;
    private final String sortField;
    private final SortDirection sortDirection;

    /**
     * 创建分页请求
     *
     * @param page     页码（从0开始）
     * @param size     每页条数（1-100）
     * @param sortField 排序字段（可选）
     * @param sortDirection 排序方向（可选，默认ASC）
     */
    public PageRequest(Integer page, Integer size, String sortField, SortDirection sortDirection) {
        // 页码校验
        if (page != null && page < 0) {
            throw new BusinessException(ErrorCode.PAGE_NUMBER_NEGATIVE);
        }
        this.page = page != null ? page : DEFAULT_PAGE;

        // 每页条数校验
        if (size != null && (size < MIN_SIZE || size > MAX_SIZE)) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_INVALID);
        }
        this.size = size != null ? size : DEFAULT_SIZE;

        // 排序字段处理
        if (sortField != null) {
            String trimmed = sortField.trim();
            this.sortField = trimmed.isEmpty() ? null : trimmed;
        } else {
            this.sortField = null;
        }

        // 排序方向处理
        this.sortDirection = sortDirection != null ? sortDirection : SortDirection.ASC;
    }

    /**
     * 静态工厂方法，处理可能为null的参数
     */
    public static PageRequest of(Integer page, Integer size, String sortField, SortDirection sortDirection) {
        return new PageRequest(page, size, sortField, sortDirection);
    }

    /**
     * 默认分页请求
     */
    public static PageRequest defaultPage() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE, null, null);
    }

    /**
     * 计算数据偏移量（用于数据库查询）
     */
    public long getOffset() {
        return (long) page * size;
    }
}