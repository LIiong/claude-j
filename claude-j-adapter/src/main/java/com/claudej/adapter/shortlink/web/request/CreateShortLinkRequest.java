package com.claudej.adapter.shortlink.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "创建短链接请求")
@Data
public class CreateShortLinkRequest {

    @Schema(description = "原始长链接URL", example = "https://example.com/very/long/url")
    @NotBlank(message = "原始URL不能为空")
    private String originalUrl;
}
