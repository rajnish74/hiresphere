package com.hiresphere.notification;

import com.hiresphere.notification.entity.NotificationCategory;
import com.hiresphere.notification.service.NotificationTemplates;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplatesTest {

    @Test
    void everyEmailTemplateFileShouldHaveACategory() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:templates/email/*.html");

        assertThat(files).isNotEmpty();
        for (Resource file : files) {
            String name = file.getFilename().replace(".html", "");
            assertThat(NotificationTemplates.isMapped(name))
                    .as("Template '%s' ko NotificationTemplates mein ek category chahiye", name)
                    .isTrue();
        }
    }

    @Test
    void securityAndAccountEmailsMustNeverBeOptional() {
        for (String template : new String[]{"email-verification", "password-reset", "password-changed",
                "account-blocked", "company-status-recruiter", "job-hidden-recruiter"}) {
            assertThat(NotificationTemplates.categoryOf(template).isOptional()).as(template).isFalse();
        }
    }

    @Test
    void applicationEmailsShouldBelongToTheRightOptionalCategories() {
        assertThat(NotificationTemplates.categoryOf("application-status-candidate"))
                .isEqualTo(NotificationCategory.APPLICATION_UPDATES);
        assertThat(NotificationTemplates.categoryOf("application-received-recruiter"))
                .isEqualTo(NotificationCategory.NEW_APPLICANTS);
    }

    @Test
    void reportUpdatesShouldBeAnOptionalCategory() {
        assertThat(NotificationTemplates.categoryOf("job-report-update")).isEqualTo(NotificationCategory.REPORT_UPDATES);
        assertThat(NotificationCategory.REPORT_UPDATES.isOptional()).isTrue();
    }
}