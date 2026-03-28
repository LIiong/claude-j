package com.claudej.infrastructure.shortlink.persistence.converter;

import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import com.claudej.infrastructure.shortlink.persistence.dataobject.ShortLinkDO;

public class ShortLinkConverter {

    private ShortLinkConverter() {
    }

    public static ShortLinkDO toDataObject(ShortLink shortLink) {
        ShortLinkDO dataObject = new ShortLinkDO();
        dataObject.setId(shortLink.getId());
        dataObject.setShortCode(shortLink.getShortCode() != null ? shortLink.getShortCode().getValue() : null);
        dataObject.setOriginalUrl(shortLink.getOriginalUrlValue());
        dataObject.setCreateTime(shortLink.getCreateTime());
        dataObject.setExpireTime(shortLink.getExpireTime());
        return dataObject;
    }

    public static ShortLink toDomain(ShortLinkDO dataObject) {
        ShortCode shortCode = dataObject.getShortCode() != null
                ? new ShortCode(dataObject.getShortCode())
                : null;

        return ShortLink.reconstruct(
                dataObject.getId(),
                shortCode,
                new OriginalUrl(dataObject.getOriginalUrl()),
                dataObject.getCreateTime(),
                dataObject.getExpireTime()
        );
    }
}
