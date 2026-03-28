package com.claudej.infrastructure.shortlink.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import com.claudej.domain.shortlink.repository.ShortLinkRepository;
import com.claudej.infrastructure.shortlink.persistence.converter.ShortLinkConverter;
import com.claudej.infrastructure.shortlink.persistence.dataobject.ShortLinkDO;
import com.claudej.infrastructure.shortlink.persistence.mapper.ShortLinkMapper;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ShortLinkRepositoryImpl implements ShortLinkRepository {

    private final ShortLinkMapper shortLinkMapper;

    public ShortLinkRepositoryImpl(ShortLinkMapper shortLinkMapper) {
        this.shortLinkMapper = shortLinkMapper;
    }

    @Override
    public ShortLink save(ShortLink shortLink) {
        ShortLinkDO dataObject = ShortLinkConverter.toDataObject(shortLink);
        dataObject.setOriginalUrlHash(sha256(shortLink.getOriginalUrlValue()));
        dataObject.setUpdateTime(LocalDateTime.now());

        if (shortLink.getId() == null) {
            shortLinkMapper.insert(dataObject);
            shortLink.setId(dataObject.getId());
        } else {
            shortLinkMapper.updateById(dataObject);
        }

        return shortLink;
    }

    @Override
    public Optional<ShortLink> findByShortCode(ShortCode shortCode) {
        LambdaQueryWrapper<ShortLinkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShortLinkDO::getShortCode, shortCode.getValue());

        ShortLinkDO dataObject = shortLinkMapper.selectOne(wrapper);
        return Optional.ofNullable(dataObject).map(ShortLinkConverter::toDomain);
    }

    @Override
    public Optional<ShortLink> findByOriginalUrl(OriginalUrl originalUrl) {
        String urlHash = sha256(originalUrl.getValue());

        LambdaQueryWrapper<ShortLinkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShortLinkDO::getOriginalUrlHash, urlHash);

        ShortLinkDO dataObject = shortLinkMapper.selectOne(wrapper);
        if (dataObject != null && dataObject.getOriginalUrl().equals(originalUrl.getValue())) {
            return Optional.of(ShortLinkConverter.toDomain(dataObject));
        }
        return Optional.empty();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
