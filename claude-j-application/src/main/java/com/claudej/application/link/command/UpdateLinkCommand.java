package com.claudej.application.link.command;

import lombok.Data;

/**
 * 更新链接命令
 */
@Data
public class UpdateLinkCommand {

    private Long id;
    private String name;
    private String url;
    private String description;
    private String category;
}
