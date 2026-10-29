package com.hiresphere.application;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.ApplicationStatusHistory;
import com.hiresphere.application.entity.JobApplication;
import com.hiresphere.common.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationTest {

    private JobApplication submitted() {
        JobApplication a = new JobApplication();
        a.recordSubmitted(1L);
        return a;
    }

    @Test
    void newApplicationShouldStartAsAppliedWithOneTimelineEntry() {
        JobApplication a = submitted();

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(a.getHistory()).hasSize(1);
        assertThat(a.getHistory().get(0).getFromStatus()).isNull();
    }

    @Test
    void transitionShouldChangeStatusAndRecordWhoAndWhy() {
        JobApplication a = submitted();

        a.transitionTo(ApplicationStatus.SHORTLISTED, 2L, "Strong profile");

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        assertThat(a.getHistory()).hasSize(2);
        ApplicationStatusHistory last = a.getHistory().get(1);
        assertThat(last.getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(last.getToStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        assertThat(last.getChangedBy()).isEqualTo(2L);
        assertThat(last.getNote()).isEqualTo("Strong profile");
    }

    @Test
    void invalidTransitionShouldFailAndLeaveNoTrace() {
        JobApplication a = submitted();

        assertThatThrownBy(() -> a.transitionTo(ApplicationStatus.OFFER, 2L, null))
                .isInstanceOf(AppException.class);

        assertThat(a.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(a.getHistory()).hasSize(1);
    }

    @Test
    void hiredApplicationCannotBeWithdrawnOrRejected() {
        JobApplication a = submitted();
        a.transitionTo(ApplicationStatus.SHORTLISTED, 2L, null);
        a.transitionTo(ApplicationStatus.INTERVIEW, 2L, null);
        a.transitionTo(ApplicationStatus.OFFER, 2L, null);
        a.transitionTo(ApplicationStatus.HIRED, 2L, null);

        assertThatThrownBy(() -> a.transitionTo(ApplicationStatus.WITHDRAWN, 1L, null))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> a.transitionTo(ApplicationStatus.REJECTED, 2L, null))
                .isInstanceOf(AppException.class);
    }
}