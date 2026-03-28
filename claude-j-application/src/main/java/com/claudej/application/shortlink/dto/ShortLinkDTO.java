package com.claudej.application.shortlink.dto;

import lombok.Data;

@Data
public class ShortLinkDTO {

    private String shortCode;
    private String originalUrl;
}
