package com.claudej.adapter.shortlink.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "短链接响应")
@Data
public class ShortLinkResponse {

    @Schema(description = "短链接编码", example = "aB3x9K")
    private String shortCode;

    @Schema(description = "短链接完整URL", example = "http://localhost:8081/s/aB3x9K")
    private String shortUrl;

    @Schema(description = "原始长链接", example = "https://example.com/very/long/url")
    private String originalUrl;
}
