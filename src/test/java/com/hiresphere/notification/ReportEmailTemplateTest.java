package com.hiresphere.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportEmailTemplateTest {

    @Autowired
    private TemplateEngine templateEngine;

    private String render(boolean resolved, String jobTitle) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariables(Map.of("jobTitle", jobTitle, "resolved", resolved, "link", "http://localhost:3000/my-reports"));
        return templateEngine.process("email/job-report-update", ctx);
    }

    @Test
    void resolvedAndDismissedShouldShowDifferentMessages() {
        assertThat(render(true, "Java Dev")).contains("have taken action").doesNotContain("did not find a violation");
        assertThat(render(false, "Java Dev")).contains("did not find a violation").doesNotContain("have taken action");
    }

    @Test
    void jobTitleShouldBeEscaped() {
        assertThat(render(true, "<script>alert(1)</script>")).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}