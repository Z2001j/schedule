package com.common.schedule.admin.service;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.ScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleValidatorTest {

    private ScheduleValidator validator;

    @BeforeEach
    void setup() {
        AdminProperties properties = new AdminProperties();
        properties.setMinPeriodMs(1000);
        validator = new ScheduleValidator(properties);
    }

    @Test
    void validCronPasses() {
        assertThatCode(() -> validator.validate(ScheduleType.CRON, "0/5 * * * * ?", null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void invalidCronRejected() {
        assertThatThrownBy(() -> validator.validate(ScheduleType.CRON, "not-a-cron", null, null))
                .isInstanceOf(ScheduleException.class);
    }

    @Test
    void blankCronRejected() {
        assertThatThrownBy(() -> validator.validate(ScheduleType.CRON, "  ", null, null))
                .isInstanceOf(ScheduleException.class);
    }

    @Test
    void fixedRateBelowMinimumRejected() {
        assertThatThrownBy(() -> validator.validate(ScheduleType.FIXED_RATE, null, 500L, null))
                .isInstanceOf(ScheduleException.class);
    }

    @Test
    void fixedRateAtMinimumPasses() {
        assertThatCode(() -> validator.validate(ScheduleType.FIXED_RATE, null, 1000L, null))
                .doesNotThrowAnyException();
    }

    @Test
    void fixedDelayNullRejected() {
        assertThatThrownBy(() -> validator.validate(ScheduleType.FIXED_DELAY, null, null, null))
                .isInstanceOf(ScheduleException.class);
    }

    @Test
    void nullTypeRejected() {
        assertThatThrownBy(() -> validator.validate(null, "0/5 * * * * ?", null, null))
                .isInstanceOf(ScheduleException.class);
    }

    @Test
    void retryBounds() {
        assertThatCode(() -> validator.validateRetry(0)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateRetry(10)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateRetry(-1)).isInstanceOf(ScheduleException.class);
        assertThatThrownBy(() -> validator.validateRetry(11)).isInstanceOf(ScheduleException.class);
    }
}