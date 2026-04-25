package com.claudej.adapter.shortlink.web;

import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.application.shortlink.service.ShortLinkApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "短链跳转", description = "短链接跳转服务")
@Controller
@RequestMapping("/s")
public class ShortLinkRedirectController {

    private final ShortLinkApplicationService shortLinkApplicationService;

    public ShortLinkRedirectController(ShortLinkApplicationService shortLinkApplicationService) {
        this.shortLinkApplicationService = shortLinkApplicationService;
    }

    @Operation(summary = "短链跳转", description = "根据短码跳转到原始长链接")
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        ShortLinkDTO dto = shortLinkApplicationService.resolveShortLink(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, dto.getOriginalUrl());

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
