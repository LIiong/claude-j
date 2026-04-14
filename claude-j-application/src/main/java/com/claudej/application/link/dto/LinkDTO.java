package com.claudej.application.link.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链接 DTO
 */
@Data
public class LinkDTO {

    private Long id;
    private String name;
    private String url;
    private String description;
    private String category;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
