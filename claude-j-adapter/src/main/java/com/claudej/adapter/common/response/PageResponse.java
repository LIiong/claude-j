package com.claudej.adapter.common.response;

import lombok.Data;

import java.util.List;

/**
 * 分页响应对象 - REST API 分页结果
 */
@Data
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean first;
    private boolean last;
    private boolean empty;
}