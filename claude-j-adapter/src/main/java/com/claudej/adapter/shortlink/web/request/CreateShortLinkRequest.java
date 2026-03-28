package com.claudej.adapter.shortlink.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateShortLinkRequest {

    @NotBlank(message = "原始URL不能为空")
    private String originalUrl;
}
