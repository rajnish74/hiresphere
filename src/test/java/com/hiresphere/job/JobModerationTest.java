package com.hiresphere.job;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.job.entity.Job;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobModerationTest {

    @Test
    void newJobShouldNotBeHidden() {
        assertThat(new Job().isHidden()).isFalse();
    }

    @Test
    void hideShouldMarkJobHiddenWithReason() {
        Job job = new Job();

        job.hideByAdmin("Misleading salary");

        assertThat(job.isHidden()).isTrue();
        assertThat(job.getModerationReason()).isEqualTo("Misleading salary");
    }

    @Test
    void hidingTwiceShouldFail() {
        Job job = new Job();
        job.hideByAdmin("x");

        assertThatThrownBy(() -> job.hideByAdmin("y")).isInstanceOf(AppException.class);
    }

    @Test
    void unhideShouldClearTheFlagAndReason() {
        Job job = new Job();
        job.hideByAdmin("x");

        job.unhideByAdmin();

        assertThat(job.isHidden()).isFalse();
        assertThat(job.getModerationReason()).isNull();
    }

    @Test
    void unhidingAVisibleJobShouldFail() {
        assertThatThrownBy(() -> new Job().unhideByAdmin()).isInstanceOf(AppException.class);
    }
}