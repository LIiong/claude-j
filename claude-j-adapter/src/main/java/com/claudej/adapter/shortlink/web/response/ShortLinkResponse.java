package com.claudej.adapter.shortlink.web.response;

import lombok.Data;

@Data
public class ShortLinkResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
}
