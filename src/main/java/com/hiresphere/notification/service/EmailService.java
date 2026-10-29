package com.hiresphere.notification.service;

import com.hiresphere.config.EmailProperties;
import com.hiresphere.notification.entity.EmailLog;
import com.hiresphere.notification.entity.EmailStatus;
import com.hiresphere.notification.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailProperties props;
    private final EmailLogRepository logRepository;

    public void send(String to, String subject, String template, Map<String, Object> model) {
        if (!props.enabled()) {
            log.info("Email disabled, skipping template '{}'", template);
            return;
        }
        String cleanSubject = subject.replaceAll("[\\r\\n]+", " ").trim();

        String html;
        try {
            html = render(template, model);
        } catch (Exception e) {
            log.error("Could not render email template '{}'", template, e);
            record(to, cleanSubject, template, EmailStatus.FAILED, 0, e);
            return;
        }

        Exception last = null;
        int attempts = 0;
        while (attempts < props.maxAttempts()) {
            attempts++;
            try {
                deliver(to, cleanSubject, html);
                record(to, cleanSubject, template, EmailStatus.SENT, attempts, null);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("Email '{}' failed (attempt {}/{}): {}", template, attempts,
                        props.maxAttempts(), e.getMessage());
                if (attempts < props.maxAttempts() && !pause(props.retryDelayMs() * attempts)) {
                    break;
                }
            }
        }
        record(to, cleanSubject, template, EmailStatus.FAILED, attempts, last);
    }

    private String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(model);
        return templateEngine.process("email/" + template, context);
    }

    private void deliver(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(props.from());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private boolean pause(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void record(String to, String subject, String template, EmailStatus status,
                        int attempts, Exception error) {
        try {
            logRepository.save(EmailLog.of(to, subject, template, status, attempts,
                    error == null ? null : error.getClass().getSimpleName() + ": " + error.getMessage()));
        } catch (Exception e) {
            log.error("Could not write email log", e);
        }
    }
}