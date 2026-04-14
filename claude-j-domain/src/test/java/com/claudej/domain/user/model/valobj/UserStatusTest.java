package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserStatusTest {

    @Test
    void should_haveCorrectDescriptions() {
        // Assert
        assertThat(UserStatus.ACTIVE.getDescription()).isEqualTo("活跃");
        assertThat(UserStatus.INACTIVE.getDescription()).isEqualTo("非活跃");
        assertThat(UserStatus.FROZEN.getDescription()).isEqualTo("已冻结");
    }

    @Test
    void should_allowFreeze_when_active() {
        // Assert
        assertThat(UserStatus.ACTIVE.canFreeze()).isTrue();
    }

    @Test
    void should_allowFreeze_when_inactive() {
        // Assert
        assertThat(UserStatus.INACTIVE.canFreeze()).isTrue();
    }

    @Test
    void should_notAllowFreeze_when_frozen() {
        // Assert
        assertThat(UserStatus.FROZEN.canFreeze()).isFalse();
    }

    @Test
    void should_allowUnfreeze_when_frozen() {
        // Assert
        assertThat(UserStatus.FROZEN.canUnfreeze()).isTrue();
    }

    @Test
    void should_notAllowUnfreeze_when_notFrozen() {
        // Assert
        assertThat(UserStatus.ACTIVE.canUnfreeze()).isFalse();
        assertThat(UserStatus.INACTIVE.canUnfreeze()).isFalse();
    }

    @Test
    void should_allowActivate_when_inactive() {
        // Assert
        assertThat(UserStatus.INACTIVE.canActivate()).isTrue();
    }

    @Test
    void should_notAllowActivate_when_activeOrFrozen() {
        // Assert
        assertThat(UserStatus.ACTIVE.canActivate()).isFalse();
        assertThat(UserStatus.FROZEN.canActivate()).isFalse();
    }

    @Test
    void should_allowDeactivate_when_active() {
        // Assert
        assertThat(UserStatus.ACTIVE.canDeactivate()).isTrue();
    }

    @Test
    void should_notAllowDeactivate_when_inactiveOrFrozen() {
        // Assert
        assertThat(UserStatus.INACTIVE.canDeactivate()).isFalse();
        assertThat(UserStatus.FROZEN.canDeactivate()).isFalse();
    }

    @Test
    void should_transitionToFrozen_when_active() {
        // Act
        UserStatus newStatus = UserStatus.ACTIVE.toFrozen();

        // Assert
        assertThat(newStatus).isEqualTo(UserStatus.FROZEN);
    }

    @Test
    void should_transitionToFrozen_when_inactive() {
        // Act
        UserStatus newStatus = UserStatus.INACTIVE.toFrozen();

        // Assert
        assertThat(newStatus).isEqualTo(UserStatus.FROZEN);
    }

    @Test
    void should_throwException_when_freezingFrozenUser() {
        // Act & Assert
        assertThatThrownBy(() -> UserStatus.FROZEN.toFrozen())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许冻结");
    }

    @Test
    void should_transitionToActive_when_frozen() {
        // Act
        UserStatus newStatus = UserStatus.FROZEN.toActive();

        // Assert
        assertThat(newStatus).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_transitionToActive_when_inactive() {
        // Act
        UserStatus newStatus = UserStatus.INACTIVE.toActive();

        // Assert
        assertThat(newStatus).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_throwException_when_activatingActiveUser() {
        // Act & Assert
        assertThatThrownBy(() -> UserStatus.ACTIVE.toActive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许激活");
    }

    @Test
    void should_transitionToInactive_when_active() {
        // Act
        UserStatus newStatus = UserStatus.ACTIVE.toInactive();

        // Assert
        assertThat(newStatus).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void should_throwException_when_deactivatingInactiveUser() {
        // Act & Assert
        assertThatThrownBy(() -> UserStatus.INACTIVE.toInactive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许停用");
    }

    @Test
    void should_throwException_when_deactivatingFrozenUser() {
        // Act & Assert
        assertThatThrownBy(() -> UserStatus.FROZEN.toInactive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许停用");
    }
}
