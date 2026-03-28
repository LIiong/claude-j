package com.claudej.domain.shortlink.repository;

import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;

import java.util.Optional;

public interface ShortLinkRepository {

    ShortLink save(ShortLink shortLink);

    Optional<ShortLink> findByShortCode(ShortCode shortCode);

    Optional<ShortLink> findByOriginalUrl(OriginalUrl originalUrl);
}
