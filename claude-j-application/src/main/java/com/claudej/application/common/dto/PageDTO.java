package com.claudej.application.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页结果 DTO - 应用层分页查询的返回结果
 */
@Data
public class PageDTO<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean first;
    private boolean last;
    private boolean empty;

    /**
     * 创建空分页结果
     */
    public static <T> PageDTO<T> empty(int page, int size) {
        PageDTO<T> pageDTO = new PageDTO<T>();
        pageDTO.setContent(java.util.Collections.emptyList());
        pageDTO.setTotalElements(0L);
        pageDTO.setTotalPages(0);
        pageDTO.setPage(page);
        pageDTO.setSize(size);
        pageDTO.setFirst(true);
        pageDTO.setLast(true);
        pageDTO.setEmpty(true);
        return pageDTO;
    }
}