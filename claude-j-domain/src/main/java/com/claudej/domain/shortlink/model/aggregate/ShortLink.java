package com.claudej.domain.shortlink.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ShortLink {

    private Long id;
    private ShortCode shortCode;
    private final OriginalUrl originalUrl;
    private final LocalDateTime createTime;
    private LocalDateTime expireTime;

    private ShortLink(OriginalUrl originalUrl, LocalDateTime createTime) {
        this.originalUrl = originalUrl;
        this.createTime = createTime;
    }

    /**
     * 工厂方法：创建无码短链（短码在持久化获取 ID 后分配）
     */
    public static ShortLink create(OriginalUrl originalUrl) {
        return new ShortLink(originalUrl, LocalDateTime.now());
    }

    /**
     * 从持久化层重建聚合根
     */
    public static ShortLink reconstruct(Long id, ShortCode shortCode, OriginalUrl originalUrl,
                                         LocalDateTime createTime, LocalDateTime expireTime) {
        ShortLink shortLink = new ShortLink(originalUrl, createTime);
        shortLink.id = id;
        shortLink.shortCode = shortCode;
        shortLink.expireTime = expireTime;
        return shortLink;
    }

    /**
     * 分配短码（仅允许一次）
     */
    public void assignShortCode(ShortCode shortCode) {
        if (this.shortCode != null) {
            throw new BusinessException(ErrorCode.SHORT_CODE_ALREADY_ASSIGNED);
        }
        this.shortCode = shortCode;
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 检查短链是否已过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 便捷获取原始 URL 字符串值
     */
    public String getOriginalUrlValue() {
        return originalUrl.getValue();
    }
}
