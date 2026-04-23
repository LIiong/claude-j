package com.claudej.domain.common.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果值对象 - 分页查询的返回结果
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Page<T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
    private final boolean first;
    private final boolean last;
    private final boolean empty;

    /**
     * 创建分页结果
     *
     * @param content       数据列表
     * @param totalElements 总条数
     * @param page          当前页码
     * @param size          每页条数
     */
    public Page(List<T> content, long totalElements, int page, int size) {
        this.content = content != null ? content : Collections.emptyList();
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = calculateTotalPages(totalElements, size);
        this.first = page == 0;
        this.last = page >= totalPages - 1 || totalPages == 0;
        this.empty = this.content.isEmpty();
    }

    /**
     * 计算总页数
     */
    private static int calculateTotalPages(long totalElements, int size) {
        if (size == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return !last;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return !first;
    }

    /**
     * 创建空分页结果
     */
    public static <T> Page<T> empty(int page, int size) {
        return new Page<T>(Collections.emptyList(), 0L, page, size);
    }
}