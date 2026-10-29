package com.hiresphere.application;

import com.hiresphere.application.entity.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static com.hiresphere.application.entity.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    void happyPathShouldFollowThePipelineInOrder() {
        assertThat(APPLIED.canMoveTo(SHORTLISTED)).isTrue();
        assertThat(SHORTLISTED.canMoveTo(INTERVIEW)).isTrue();
        assertThat(INTERVIEW.canMoveTo(OFFER)).isTrue();
        assertThat(OFFER.canMoveTo(HIRED)).isTrue();
    }

    @Test
    void stagesCannotBeSkippedOrReversed() {
        assertThat(APPLIED.canMoveTo(OFFER)).isFalse();
        assertThat(APPLIED.canMoveTo(HIRED)).isFalse();
        assertThat(INTERVIEW.canMoveTo(SHORTLISTED)).isFalse();
    }

    @Test
    void everyActiveStageCanBeRejectedOrWithdrawn() {
        for (ApplicationStatus s : new ApplicationStatus[]{APPLIED, SHORTLISTED, INTERVIEW, OFFER}) {
            assertThat(s.canMoveTo(REJECTED)).as("%s -> REJECTED", s).isTrue();
            assertThat(s.canMoveTo(WITHDRAWN)).as("%s -> WITHDRAWN", s).isTrue();
        }
    }

    @Test
    void terminalStatesHaveNoWayOut() {
        assertThat(HIRED.next()).isEmpty();
        assertThat(REJECTED.next()).isEmpty();
        assertThat(WITHDRAWN.next()).isEmpty();
    }

    @Test
    void recruiterCanNeverWithdraw() {
        for (ApplicationStatus s : ApplicationStatus.values()) {
            assertThat(s.nextForRecruiter()).doesNotContain(WITHDRAWN);
        }
        assertThat(APPLIED.nextForRecruiter()).containsExactlyInAnyOrder(SHORTLISTED, REJECTED);
    }
}