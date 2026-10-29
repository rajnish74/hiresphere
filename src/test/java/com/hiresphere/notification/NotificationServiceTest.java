package com.hiresphere.notification;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.auth.event.EmailVerificationRequestedEvent;
import com.hiresphere.auth.event.PasswordChangedEvent;
import com.hiresphere.auth.event.PasswordResetRequestedEvent;
import com.hiresphere.config.EmailProperties;
import com.hiresphere.notification.service.EmailService;
import com.hiresphere.notification.service.NotificationService;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;
    @Mock
    private UserService userService;
    @Mock
    private CompanyService companyService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> modelCaptor;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(emailService, userService, companyService,
                new EmailProperties(true, "x", "http://localhost:3000", 3, 0));
    }

    private void stubCandidate() {
        when(userService.getEmails(Set.of(1L))).thenReturn(Map.of(1L, "cand@test.com"));
    }

    private void stubRecruiter() {
        when(companyService.getOwnerId(5L)).thenReturn(Optional.of(2L));
        when(userService.getEmails(Set.of(2L))).thenReturn(Map.of(2L, "rec@test.com"));
    }

    private ApplicationStatusChangedEvent statusEvent(ApplicationStatus to, boolean byCandidate) {
        return new ApplicationStatusChangedEvent(9L, 5L, 1L, "Asha", "Java Dev", "Acme",
                ApplicationStatus.APPLIED, to, "Strong profile", byCandidate);
    }

    // ---------- application emails ----------

    @Test
    void submittedApplicationShouldEmailBothCandidateAndRecruiter() {
        stubCandidate();
        stubRecruiter();

        service.onApplicationSubmitted(
                new ApplicationSubmittedEvent(9L, 5L, 1L, "Asha", "Java Dev", "Acme"));

        verify(emailService).send(eq("cand@test.com"), eq("Application received: Java Dev"),
                eq("application-submitted-candidate"), anyMap());
        verify(emailService).send(eq("rec@test.com"), eq("New application for Java Dev"),
                eq("application-received-recruiter"), anyMap());
    }

    @Test
    void recruiterStatusChangeShouldEmailOnlyTheCandidate() {
        stubCandidate();

        service.onStatusChanged(statusEvent(ApplicationStatus.SHORTLISTED, false));

        verify(emailService).send(eq("cand@test.com"), eq("You have been shortlisted for Java Dev"),
                eq("application-status-candidate"), anyMap());
        verify(emailService, never()).send(eq("rec@test.com"), any(), any(), any());
    }

    @Test
    void withdrawalShouldEmailOnlyTheRecruiter() {
        stubRecruiter();

        service.onStatusChanged(statusEvent(ApplicationStatus.WITHDRAWN, true));

        verify(emailService).send(eq("rec@test.com"), eq("Application withdrawn: Java Dev"),
                eq("application-withdrawn-recruiter"), anyMap());
        verify(emailService, never()).send(eq("cand@test.com"), any(), any(), any());
    }

    @Test
    void unknownEmailShouldBeSkippedWithoutError() {
        when(userService.getEmails(Set.of(1L))).thenReturn(Map.of());

        service.onStatusChanged(statusEvent(ApplicationStatus.REJECTED, false));

        verify(emailService, never()).send(any(), any(), any(), any());
    }

    // ---------- company emails ----------

    @Test
    void rejectedCompanyEmailShouldCarryTheReason() {
        when(userService.getEmails(Set.of(2L))).thenReturn(Map.of(2L, "rec@test.com"));

        service.onCompanyStatusChanged(
                new CompanyStatusChangedEvent(5L, 2L, "Acme", CompanyStatus.REJECTED, "Website mismatch"));

        verify(emailService).send(eq("rec@test.com"), eq("Verification update for Acme"),
                eq("company-status-recruiter"), modelCaptor.capture());
        assertThat(modelCaptor.getValue().get("reason")).isEqualTo("Website mismatch");
    }

    @Test
    void approvedCompanyEmailShouldGoToTheOwner() {
        when(userService.getEmails(Set.of(2L))).thenReturn(Map.of(2L, "rec@test.com"));

        service.onCompanyStatusChanged(
                new CompanyStatusChangedEvent(5L, 2L, "Acme", CompanyStatus.APPROVED, null));

        verify(emailService).send(eq("rec@test.com"), eq("Your company Acme has been approved"),
                eq("company-status-recruiter"), anyMap());
    }

    // ---------- auth emails ----------

    @Test
    void verificationEmailShouldGoToTheEventAddressWithATokenLink() {
        service.onEmailVerification(new EmailVerificationRequestedEvent("Asha", "asha@test.com", "tok123", 24));

        verify(emailService).send(eq("asha@test.com"), eq("Verify your email address"),
                eq("email-verification"), modelCaptor.capture());
        assertThat(modelCaptor.getValue().get("link"))
                .isEqualTo("http://localhost:3000/verify-email?token=tok123");
        assertThat(modelCaptor.getValue().get("validFor")).isEqualTo("24 hours");
    }

    @Test
    void resetEmailShouldContainTheResetLinkAndValidity() {
        service.onPasswordResetRequested(new PasswordResetRequestedEvent("Asha", "asha@test.com", "tok456", 30));

        verify(emailService).send(eq("asha@test.com"), eq("Reset your HireSphere password"),
                eq("password-reset"), modelCaptor.capture());
        assertThat(modelCaptor.getValue().get("link"))
                .isEqualTo("http://localhost:3000/reset-password?token=tok456");
        assertThat(modelCaptor.getValue().get("validFor")).isEqualTo("30 minutes");
    }

    @Test
    void passwordChangedAlertShouldNeverContainAToken() {
        service.onPasswordChanged(new PasswordChangedEvent("Asha", "asha@test.com"));

        verify(emailService).send(eq("asha@test.com"), eq("Your HireSphere password was changed"),
                eq("password-changed"), modelCaptor.capture());
        assertThat(modelCaptor.getValue().get("link").toString()).doesNotContain("token");
    }

    @Test
    void eventToStringShouldHideTheToken() {
        assertThat(new PasswordResetRequestedEvent("Asha", "a@test.com", "secret-token", 30).toString())
                .doesNotContain("secret-token");
        assertThat(new EmailVerificationRequestedEvent("Asha", "a@test.com", "secret-token", 24).toString())
                .doesNotContain("secret-token");
    }
}