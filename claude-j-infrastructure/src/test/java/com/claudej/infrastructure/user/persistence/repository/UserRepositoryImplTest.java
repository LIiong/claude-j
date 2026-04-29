package com.claudej.infrastructure.user.persistence.repository;

import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    void should_saveNewUser_when_userHasNoId() {
        // Given
        User user = User.create(new Username("testuser1"), new InviteCode("ABC234"));

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername().getValue()).isEqualTo("testuser1");
    }

    @Test
    void should_findUserByUserId_when_userExists() {
        // Given
        User user = User.create(new Username("testuser2"), new InviteCode("DEF567"));
        User saved = userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUserId(saved.getUserId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername().getValue()).isEqualTo("testuser2");
    }

    @Test
    void should_returnEmpty_when_userNotFound() {
        // When
        Optional<User> found = userRepository.findByUserId(new UserId("UR1234567890ABCDEF"));

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_findUserByUsername_when_userExists() {
        // Given
        User user = User.create(new Username("testuser3"), new InviteCode("GHJ234"));
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByUsername(new Username("testuser3"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername().getValue()).isEqualTo("testuser3");
    }

    @Test
    void should_findUserByInviteCode_when_userExists() {
        // Given
        User user = User.create(new Username("testuser4"), new InviteCode("KLM567"));
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByInviteCode(new InviteCode("KLM567"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getInviteCodeValue()).isEqualTo("KLM567");
    }

    @Test
    void should_findUsersByInviterId_when_usersExist() {
        // Given
        User inviter = User.create(new Username("inviter"), new InviteCode("NPQ234"));
        User savedInviter = userRepository.save(inviter);

        User invited1 = User.create(new Username("invited1"), new InviteCode("RST567"));
        invited1.setInviter(savedInviter.getUserId());
        userRepository.save(invited1);

        User invited2 = User.create(new Username("invited2"), new InviteCode("VWX234"));
        invited2.setInviter(savedInviter.getUserId());
        userRepository.save(invited2);

        // When
        List<User> invitedUsers = userRepository.findByInviterId(savedInviter.getUserId());

        // Then
        assertThat(invitedUsers).hasSize(2);
    }

    @Test
    void should_returnTrue_when_usernameExists() {
        // Given
        User user = User.create(new Username("existinguser"), new InviteCode("YZA567"));
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByUsername(new Username("existinguser"))).isTrue();
    }

    @Test
    void should_returnFalse_when_usernameDoesNotExist() {
        // When & Then
        assertThat(userRepository.existsByUsername(new Username("nonexistentuser"))).isFalse();
    }

    @Test
    void should_returnTrue_when_inviteCodeExists() {
        // Given
        User user = User.create(new Username("testuser5"), new InviteCode("BCD234"));
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByInviteCode(new InviteCode("BCD234"))).isTrue();
    }

    @Test
    void should_returnFalse_when_inviteCodeDoesNotExist() {
        // When & Then
        assertThat(userRepository.existsByInviteCode(new InviteCode("FGH567"))).isFalse();
    }

    @Test
    void should_updateUser_when_userHasId() {
        // Given
        User user = User.create(new Username("updateuser"), new InviteCode("JKL234"));
        User saved = userRepository.save(user);

        // When - 冻结用户并保存
        saved.freeze();
        User updated = userRepository.save(saved);

        // Then
        Optional<User> found = userRepository.findByUserId(updated.getUserId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus().name()).isEqualTo("FROZEN");
    }

    @Test
    void should_saveUserWithEmailAndPhone_when_provided() {
        // Given
        User user = User.create(new Username("testuser6"), new InviteCode("MNP567"));
        user.setEmail(new com.claudej.domain.user.model.valobj.Email("test@example.com"));
        user.setPhone(new com.claudej.domain.user.model.valobj.Phone("13800138000"));

        // When
        User saved = userRepository.save(user);

        // Then
        Optional<User> found = userRepository.findByUserId(saved.getUserId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail().getValue()).isEqualTo("test@example.com");
        assertThat(found.get().getPhone().getValue()).isEqualTo("13800138000");
    }
}
