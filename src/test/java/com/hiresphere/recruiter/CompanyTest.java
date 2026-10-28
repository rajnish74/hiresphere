package com.hiresphere.recruiter;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    @Test
    void newCompanyShouldBePending() {
        assertThat(new Company().getVerificationStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void approveShouldSetVerificationDetails() {
        Company c = new Company();
        c.approve(99L);

        assertThat(c.getVerificationStatus()).isEqualTo(CompanyStatus.APPROVED);
        assertThat(c.getVerifiedBy()).isEqualTo(99L);
        assertThat(c.getVerifiedAt()).isNotNull();
    }

    @Test
    void rejectShouldStoreReason() {
        Company c = new Company();
        c.reject(99L, "Website does not match company name");

        assertThat(c.getVerificationStatus()).isEqualTo(CompanyStatus.REJECTED);
        assertThat(c.getRejectionReason()).isEqualTo("Website does not match company name");
    }

    @Test
    void approvedCompanyCannotBeRejected() {
        Company c = new Company();
        c.approve(1L);

        assertThatThrownBy(() -> c.reject(1L, "x")).isInstanceOf(AppException.class);
    }

    @Test
    void onlyApprovedCompanyCanBeSuspended() {
        Company pending = new Company();
        assertThatThrownBy(() -> pending.suspend(1L, "x")).isInstanceOf(AppException.class);

        Company approved = new Company();
        approved.approve(1L);
        approved.suspend(1L, "Reported as fake");
        assertThat(approved.getVerificationStatus()).isEqualTo(CompanyStatus.SUSPENDED);
    }

    @Test
    void resubmitShouldMoveBackToPendingAndClearReason() {
        Company c = new Company();
        c.reject(1L, "Bad info");

        c.resubmit();

        assertThat(c.getVerificationStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(c.getRejectionReason()).isNull();
    }
}