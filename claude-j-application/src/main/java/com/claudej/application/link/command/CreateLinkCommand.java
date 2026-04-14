package com.claudej.application.link.command;

import lombok.Data;

/**
 * 创建链接命令
 */
@Data
public class CreateLinkCommand {

    private String name;
    private String url;
    private String description;
    private String category;
}
