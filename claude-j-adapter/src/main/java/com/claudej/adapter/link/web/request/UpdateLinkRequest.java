package com.claudej.adapter.link.web.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 更新链接请求
 */
@Data
public class UpdateLinkRequest {

    @NotNull(message = "链接ID不能为空")
    private Long id;

    @Size(max = 100, message = "链接名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "链接地址长度不能超过500")
    private String url;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;
}
