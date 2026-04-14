package com.claudej.domain.link.repository;

import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.model.valobj.LinkCategory;

import java.util.List;
import java.util.Optional;

/**
 * 链接 Repository 端口接口
 */
public interface LinkRepository {

    /**
     * 保存链接
     */
    Link save(Link link);

    /**
     * 根据 ID 查找链接
     */
    Optional<Link> findById(Long id);

    /**
     * 根据 ID 删除链接
     */
    void deleteById(Long id);

    /**
     * 查询所有链接
     */
    List<Link> findAll();

    /**
     * 根据分类查询链接
     */
    List<Link> findByCategory(LinkCategory category);

    /**
     * 检查链接是否存在
     */
    boolean existsById(Long id);
}
