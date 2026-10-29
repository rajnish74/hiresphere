package com.hiresphere.notification;

import com.hiresphere.config.EmailProperties;
import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;
import com.hiresphere.notification.repository.EmailLogRepository;
import com.hiresphere.notification.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private EmailLogRepository logRepository;

    private EmailService service(boolean enabled) {
        return new EmailService(mailSender, templateEngine,
                new EmailProperties(enabled, "HireSphere <no-reply@hiresphere.local>",
                        "http://localhost:3000", 3, 0),   // retry delay 0: test tez chale
                logRepository);
    }

    private void stubSendingBasics() {
        when(templateEngine.process(eq("email/welcome"), any(IContext.class))).thenReturn("<p>Hello</p>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void shouldSendOnceAndLogSent() {
        stubSendingBasics();

        service(true).send("a@test.com", "Hello", "welcome", Map.of());

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        ArgumentCaptor<EmailLog> log = ArgumentCaptor.forClass(EmailLog.class);
        verify(logRepository).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(log.getValue().getAttempts()).isEqualTo(1);
    }

    @Test
    void shouldRetryAndSucceedOnThirdAttempt() {
        stubSendingBasics();
        doThrow(new MailSendException("smtp down")).doThrow(new MailSendException("smtp down"))
                .doNothing().when(mailSender).send(any(MimeMessage.class));

        service(true).send("a@test.com", "Hello", "welcome", Map.of());

        verify(mailSender, times(3)).send(any(MimeMessage.class));
        ArgumentCaptor<EmailLog> log = ArgumentCaptor.forClass(EmailLog.class);
        verify(logRepository).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(log.getValue().getAttempts()).isEqualTo(3);
    }

    @Test
    void shouldLogFailureAfterAllAttemptsWithoutThrowing() {
        stubSendingBasics();
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        service(true).send("a@test.com", "Hello", "welcome", Map.of());   // exception nahi aani chahiye

        verify(mailSender, times(3)).send(any(MimeMessage.class));
        ArgumentCaptor<EmailLog> log = ArgumentCaptor.forClass(EmailLog.class);
        verify(logRepository).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(log.getValue().getAttempts()).isEqualTo(3);
        assertThat(log.getValue().getError()).contains("smtp down");
    }

    @Test
    void disabledServiceShouldSendNothing() {
        service(false).send("a@test.com", "Hello", "welcome", Map.of());

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(logRepository, never()).save(any());
    }

    @Test
    void newlinesInSubjectShouldBeRemovedToPreventHeaderInjection() throws Exception {
        stubSendingBasics();

        service(true).send("a@test.com", "Hello\r\nBcc: evil@test.com", "welcome", Map.of());

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getSubject()).doesNotContain("\n").doesNotContain("\r");
        assertThat(message.getValue().getHeader("Bcc")).isNull();
    }
}