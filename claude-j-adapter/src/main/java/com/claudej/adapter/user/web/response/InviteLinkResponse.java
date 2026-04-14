package com.claudej.adapter.user.web.response;

import lombok.Data;

/**
 * 邀请链接响应
 */
@Data
public class InviteLinkResponse {

    private String shortCode;
    private String shortUrl;
}
