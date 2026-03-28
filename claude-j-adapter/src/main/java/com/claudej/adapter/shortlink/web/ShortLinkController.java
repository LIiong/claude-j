package com.claudej.adapter.shortlink.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.shortlink.web.request.CreateShortLinkRequest;
import com.claudej.adapter.shortlink.web.response.ShortLinkResponse;
import com.claudej.application.shortlink.command.CreateShortLinkCommand;
import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.application.shortlink.service.ShortLinkApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/short-links")
public class ShortLinkController {

    private final ShortLinkApplicationService shortLinkApplicationService;

    public ShortLinkController(ShortLinkApplicationService shortLinkApplicationService) {
        this.shortLinkApplicationService = shortLinkApplicationService;
    }

    @PostMapping
    public ApiResult<ShortLinkResponse> createShortLink(
            @Valid @RequestBody CreateShortLinkRequest request,
            HttpServletRequest httpRequest) {

        CreateShortLinkCommand command = new CreateShortLinkCommand();
        command.setOriginalUrl(request.getOriginalUrl());

        ShortLinkDTO dto = shortLinkApplicationService.createShortLink(command);

        ShortLinkResponse response = new ShortLinkResponse();
        response.setShortCode(dto.getShortCode());
        response.setOriginalUrl(dto.getOriginalUrl());

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + ":" + httpRequest.getServerPort();
        response.setShortUrl(baseUrl + "/s/" + dto.getShortCode());

        return ApiResult.ok(response);
    }
}
