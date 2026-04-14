package com.claudej.adapter.link.web.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链接响应
 */
@Data
public class LinkResponse {

    private Long id;
    private String name;
    private String url;
    private String description;
    private String category;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
