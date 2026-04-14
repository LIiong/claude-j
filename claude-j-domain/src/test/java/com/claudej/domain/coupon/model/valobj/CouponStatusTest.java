package com.claudej.domain.coupon.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponStatusTest {

    @Test
    void should_allowUse_when_available() {
        assertThat(CouponStatus.AVAILABLE.canUse()).isTrue();
        assertThat(CouponStatus.USED.canUse()).isFalse();
        assertThat(CouponStatus.EXPIRED.canUse()).isFalse();
    }

    @Test
    void should_allowExpire_when_available() {
        assertThat(CouponStatus.AVAILABLE.canExpire()).isTrue();
        assertThat(CouponStatus.USED.canExpire()).isFalse();
        assertThat(CouponStatus.EXPIRED.canExpire()).isFalse();
    }

    @Test
    void should_transitionToUsed_when_available() {
        CouponStatus newStatus = CouponStatus.AVAILABLE.toUsed();
        assertThat(newStatus).isEqualTo(CouponStatus.USED);
    }

    @Test
    void should_throwException_when_useFromUsed() {
        assertThatThrownBy(() -> CouponStatus.USED.toUsed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许使用");
    }

    @Test
    void should_throwException_when_useFromExpired() {
        assertThatThrownBy(() -> CouponStatus.EXPIRED.toUsed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许使用");
    }

    @Test
    void should_transitionToExpired_when_available() {
        CouponStatus newStatus = CouponStatus.AVAILABLE.toExpired();
        assertThat(newStatus).isEqualTo(CouponStatus.EXPIRED);
    }

    @Test
    void should_throwException_when_expireFromUsed() {
        assertThatThrownBy(() -> CouponStatus.USED.toExpired())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许过期");
    }

    @Test
    void should_throwException_when_expireFromExpired() {
        assertThatThrownBy(() -> CouponStatus.EXPIRED.toExpired())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许过期");
    }
}
