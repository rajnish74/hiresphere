package com.hiresphere.notification.service;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.event.ApplicationStatusChangedEvent;
import com.hiresphere.application.event.ApplicationSubmittedEvent;
import com.hiresphere.auth.event.EmailVerificationRequestedEvent;
import com.hiresphere.auth.event.PasswordChangedEvent;
import com.hiresphere.auth.event.PasswordResetRequestedEvent;
import com.hiresphere.config.EmailProperties;
import com.hiresphere.job.event.JobHiddenEvent;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.dto.UserResponse;
import com.hiresphere.user.event.UserBlockedEvent;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final UserService userService;
    private final CompanyService companyService;
    private final EmailProperties props;
    private final NotificationPreferenceService preferences;


    public void onApplicationSubmitted(ApplicationSubmittedEvent e) {
        sendToUser(e.candidateUserId(), "Application received: " + e.jobTitle(),
                "application-submitted-candidate",
                model(e.candidateName(), e.jobTitle(), e.companyName(), candidateLink(e.applicationId())));

        sendToCompanyOwner(e.companyId(), "New application for " + e.jobTitle(),
                "application-received-recruiter",
                model(e.candidateName(), e.jobTitle(), e.companyName(), recruiterLink(e.applicationId())));
    }

    public void onStatusChanged(ApplicationStatusChangedEvent e) {
        if (e.byCandidate()) {
            sendToCompanyOwner(e.companyId(), "Application withdrawn: " + e.jobTitle(),
                    "application-withdrawn-recruiter",
                    model(e.candidateName(), e.jobTitle(), e.companyName(), recruiterLink(e.applicationId())));
            return;
        }
        Map<String, Object> model = model(e.candidateName(), e.jobTitle(), e.companyName(),
                candidateLink(e.applicationId()));
        model.put("status", e.to().name());
        model.put("message", messageFor(e.to()));
        model.put("note", e.note());
        sendToUser(e.candidateUserId(), subjectFor(e.to(), e.jobTitle()), "application-status-candidate", model);
    }


    public void onCompanyStatusChanged(CompanyStatusChangedEvent e) {
        emailOf(e.ownerId()).ifPresent(to -> {
            Map<String, Object> model = new HashMap<>();
            model.put("companyName", e.companyName());
            model.put("status", e.status().name());
            model.put("message", companyMessage(e.status()));
            model.put("reason", e.reason());
            model.put("link", props.frontendUrl() + "/recruiter/company");
            emailService.send(to, companySubject(e.status(), e.companyName()), "company-status-recruiter", model);
        });
    }

    public void onEmailVerification(EmailVerificationRequestedEvent e) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", e.fullName());
        model.put("link", props.frontendUrl() + "/verify-email?token=" + e.token());
        model.put("validFor", e.validHours() + " hours");
        emailService.send(e.email(), "Verify your email address", "email-verification", model);
    }

    public void onPasswordResetRequested(PasswordResetRequestedEvent e) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", e.fullName());
        model.put("link", props.frontendUrl() + "/reset-password?token=" + e.token());
        model.put("validFor", e.validMinutes() + " minutes");
        emailService.send(e.email(), "Reset your HireSphere password", "password-reset", model);
    }

    public void onPasswordChanged(PasswordChangedEvent e) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", e.fullName());
        model.put("link", props.frontendUrl() + "/forgot-password");
        emailService.send(e.email(), "Your HireSphere password was changed", "password-changed", model);
    }

    public void onUserBlocked(UserBlockedEvent e) {
        UserResponse user = userService.getById(e.userId());
        Map<String, Object> model = new HashMap<>();
        model.put("name", user.fullName());
        model.put("reason", e.reason());
        emailService.send(user.email(), "Your HireSphere account has been suspended", "account-blocked", model);
    }

    public void onJobHidden(JobHiddenEvent e) {
        recruiterEmail(e.companyId()).ifPresent(to -> {
            Map<String, Object> model = new HashMap<>();
            model.put("jobTitle", e.jobTitle());
            model.put("reason", e.reason());
            model.put("link", props.frontendUrl() + "/recruiter/jobs/" + e.jobId());
            emailService.send(to, "Your job has been hidden: " + e.jobTitle(), "job-hidden-recruiter", model);
        });
    }

    // =====================  helpers  =====================

    private void sendToUser(Long userId, String subject, String template, Map<String, Object> model) {
        String to = userService.getEmails(Set.of(userId)).get(userId);
        if (to == null) {
            return;
        }
        if (!preferences.shouldSend(userId, template)) {
            emailService.logSkipped(to, subject, template);
            return;
        }
        emailService.send(to, subject, template, model);
    }

    private void sendToCompanyOwner(Long companyId, String subject, String template, Map<String, Object> model) {
        companyService.getOwnerId(companyId).ifPresent(ownerId -> sendToUser(ownerId, subject, template, model));
    }

    private Optional<String> emailOf(Long userId) {
        return Optional.ofNullable(userService.getEmails(Set.of(userId)).get(userId));
    }

    private Optional<String> recruiterEmail(Long companyId) {
        return companyService.getOwnerId(companyId).flatMap(this::emailOf);
    }

    private Map<String, Object> model(String name, String jobTitle, String companyName, String link) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("candidateName", name);
        model.put("jobTitle", jobTitle);
        model.put("companyName", companyName);
        model.put("link", link);
        return model;
    }

    private String candidateLink(Long applicationId) {
        return props.frontendUrl() + "/applications/" + applicationId;
    }

    private String recruiterLink(Long applicationId) {
        return props.frontendUrl() + "/recruiter/applications/" + applicationId;
    }

    private String subjectFor(ApplicationStatus to, String jobTitle) {
        return switch (to) {
            case SHORTLISTED -> "You have been shortlisted for " + jobTitle;
            case INTERVIEW -> "Interview stage: " + jobTitle;
            case OFFER -> "Good news about your application: " + jobTitle;
            case HIRED -> "Congratulations! You are selected for " + jobTitle;
            case REJECTED -> "Update on your application for " + jobTitle;
            default -> "Application update: " + jobTitle;
        };
    }

    private String messageFor(ApplicationStatus to) {
        return switch (to) {
            case SHORTLISTED -> "Your profile stood out and you have been shortlisted.";
            case INTERVIEW -> "You have moved to the interview stage. The recruiter may contact you with the schedule.";
            case OFFER -> "The company is preparing an offer for you.";
            case HIRED -> "Congratulations, you have been selected for this role.";
            case REJECTED -> "After careful consideration, the company has decided not to move forward with your application.";
            default -> "Your application status has been updated.";
        };
    }

    private String companySubject(CompanyStatus status, String name) {
        return switch (status) {
            case APPROVED -> "Your company " + name + " has been approved";
            case REJECTED -> "Verification update for " + name;
            case SUSPENDED -> "Your company " + name + " has been suspended";
            default -> "Company update: " + name;
        };
    }

    private String companyMessage(CompanyStatus status) {
        return switch (status) {
            case APPROVED -> "Your company is verified. You can now publish jobs and manage applications.";
            case REJECTED -> "We could not verify your company. Please update your details and submit again.";
            case SUSPENDED -> "Your company has been suspended. Its jobs are hidden and you cannot publish or manage applications.";
            default -> "Your company status has been updated.";
        };
    }
}