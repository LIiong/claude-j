package com.claudej.domain.link.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.model.valobj.LinkName;
import com.claudej.domain.link.model.valobj.LinkUrl;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 链接聚合根 - 链接管理的核心领域对象
 */
@Getter
public class Link {

    private Long id;
    private LinkName name;
    private LinkUrl url;
    private String description;
    private LinkCategory category;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Link(LinkName name, LinkUrl url, String description, LinkCategory category, LocalDateTime createTime) {
        this.name = name;
        this.url = url;
        this.description = description;
        this.category = category;
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新链接
     */
    public static Link create(LinkName name, LinkUrl url, String description, LinkCategory category) {
        if (name == null) {
            throw new BusinessException(ErrorCode.LINK_NAME_EMPTY);
        }
        if (url == null) {
            throw new BusinessException(ErrorCode.LINK_URL_EMPTY);
        }
        LocalDateTime now = LocalDateTime.now();
        return new Link(name, url, description, category, now);
    }

    /**
     * 从持久化层重建聚合根
     */
    public static Link reconstruct(Long id, LinkName name, LinkUrl url, String description,
                                    LinkCategory category, LocalDateTime createTime, LocalDateTime updateTime) {
        Link link = new Link(name, url, description, category, createTime);
        link.id = id;
        link.updateTime = updateTime;
        return link;
    }

    /**
     * 更新链接信息
     */
    public void update(LinkName newName, LinkUrl newUrl, String newDescription, LinkCategory newCategory) {
        if (newName != null) {
            this.name = newName;
        }
        if (newUrl != null) {
            this.url = newUrl;
        }
        if (newDescription != null) {
            this.description = newDescription;
        }
        if (newCategory != null) {
            this.category = newCategory;
        }
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 便捷获取链接名称字符串值
     */
    public String getNameValue() {
        return name.getValue();
    }

    /**
     * 便捷获取链接 URL 字符串值
     */
    public String getUrlValue() {
        return url.getValue();
    }

    /**
     * 便捷获取分类字符串值
     */
    public String getCategoryValue() {
        return category == null ? null : category.getValue();
    }
}
