package com.claudej.application.shortlink.command;

import lombok.Data;

@Data
public class CreateShortLinkCommand {

    private String originalUrl;
}
