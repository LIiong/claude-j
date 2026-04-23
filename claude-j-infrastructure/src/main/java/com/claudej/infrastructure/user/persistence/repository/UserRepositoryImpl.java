package com.claudej.infrastructure.user.persistence.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;
import com.claudej.domain.user.repository.UserRepository;
import com.claudej.infrastructure.common.persistence.PageHelper;
import com.claudej.infrastructure.user.persistence.converter.UserConverter;
import com.claudej.infrastructure.user.persistence.dataobject.UserDO;
import com.claudej.infrastructure.user.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户 Repository 实现
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    public UserRepositoryImpl(UserMapper userMapper, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.userConverter = userConverter;
    }

    @Override
    public User save(User user) {
        UserDO userDO = userConverter.toDO(user);
        if (user.getId() == null) {
            userMapper.insert(userDO);
            user.setId(userDO.getId());
        } else {
            userMapper.updateById(userDO);
        }
        return user;
    }

    @Override
    public Optional<User> findByUserId(UserId userId) {
        UserDO userDO = userMapper.selectByUserId(userId.getValue());
        return Optional.ofNullable(userConverter.toDomain(userDO));
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        UserDO userDO = userMapper.selectByUsername(username.getValue());
        return Optional.ofNullable(userConverter.toDomain(userDO));
    }

    @Override
    public Optional<User> findByInviteCode(InviteCode inviteCode) {
        UserDO userDO = userMapper.selectByInviteCode(inviteCode.getValue());
        return Optional.ofNullable(userConverter.toDomain(userDO));
    }

    @Override
    public List<User> findByInviterId(UserId inviterId) {
        List<UserDO> userDOList = userMapper.selectByInviterId(inviterId.getValue());
        return userDOList.stream()
                .map(userConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUsername(Username username) {
        return userMapper.existsByUsername(username.getValue());
    }

    @Override
    public boolean existsByInviteCode(InviteCode inviteCode) {
        return userMapper.existsByInviteCode(inviteCode.getValue());
    }

    @Override
    public com.claudej.domain.common.model.valobj.Page<User> findByInviterId(UserId inviterId, PageRequest pageRequest) {
        Page<UserDO> mybatisPage = PageHelper.createMybatisPlusPage(pageRequest);
        IPage<UserDO> iPage = userMapper.selectPageByInviterId(mybatisPage, inviterId.getValue());
        return PageHelper.toDomainPage(iPage, userConverter::toDomain);
    }
}
