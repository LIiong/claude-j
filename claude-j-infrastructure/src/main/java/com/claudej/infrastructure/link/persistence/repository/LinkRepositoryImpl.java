package com.claudej.infrastructure.link.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.repository.LinkRepository;
import com.claudej.infrastructure.link.persistence.converter.LinkConverter;
import com.claudej.infrastructure.link.persistence.dataobject.LinkDO;
import com.claudej.infrastructure.link.persistence.mapper.LinkMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 链接 Repository 实现
 */
@Repository
public class LinkRepositoryImpl implements LinkRepository {

    private final LinkMapper linkMapper;
    private final LinkConverter linkConverter;

    public LinkRepositoryImpl(LinkMapper linkMapper, LinkConverter linkConverter) {
        this.linkMapper = linkMapper;
        this.linkConverter = linkConverter;
    }

    @Override
    public Link save(Link link) {
        LinkDO linkDO = linkConverter.toDO(link);
        if (link.getId() == null) {
            // 新增
            linkDO.setCreateTime(LocalDateTime.now());
            linkDO.setUpdateTime(LocalDateTime.now());
            linkDO.setDeleted(0);
            linkMapper.insert(linkDO);
            link.setId(linkDO.getId());
        } else {
            // 更新
            linkDO.setUpdateTime(LocalDateTime.now());
            linkMapper.updateById(linkDO);
        }
        return link;
    }

    @Override
    public Optional<Link> findById(Long id) {
        // MyBatis-Plus逻辑删除会自动过滤已删除记录
        LinkDO linkDO = linkMapper.selectById(id);
        return Optional.ofNullable(linkConverter.toDomain(linkDO));
    }

    @Override
    public void deleteById(Long id) {
        // 使用MyBatis-Plus逻辑删除
        linkMapper.deleteById(id);
    }

    @Override
    public List<Link> findAll() {
        // MyBatis-Plus逻辑删除会自动过滤已删除记录
        List<LinkDO> linkDOList = linkMapper.selectList(null);
        return linkConverter.toDomainList(linkDOList);
    }

    @Override
    public List<Link> findByCategory(LinkCategory category) {
        LambdaQueryWrapper<LinkDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkDO::getCategory, category.getValue());
        // 逻辑删除会自动过滤
        List<LinkDO> linkDOList = linkMapper.selectList(wrapper);
        return linkConverter.toDomainList(linkDOList);
    }

    @Override
    public boolean existsById(Long id) {
        // MyBatis-Plus逻辑删除会自动过滤已删除记录
        return linkMapper.selectById(id) != null;
    }
}
