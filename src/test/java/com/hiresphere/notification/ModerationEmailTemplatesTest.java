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
class ModerationEmailTemplatesTest {

    @Autowired
    private TemplateEngine templateEngine;

    private String render(String template, Map<String, Object> model) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariables(model);
        return templateEngine.process("email/" + template, ctx);
    }

    @Test
    void blockedAndHiddenTemplatesShouldRenderTheirData() {
        String blocked = render("account-blocked", Map.of("name", "Vik", "reason", "Spam applications"));
        String hidden = render("job-hidden-recruiter", Map.of("jobTitle", "Java Dev",
                "reason", "Misleading salary", "link", "http://localhost:3000/recruiter/jobs/20"));

        assertThat(blocked).contains("Vik").contains("Spam applications");
        assertThat(hidden).contains("Java Dev").contains("Misleading salary")
                .contains("http://localhost:3000/recruiter/jobs/20");
    }

    @Test
    void adminWrittenReasonShouldBeEscaped() {
        String evil = "<script>alert(1)</script>";

        String blocked = render("account-blocked", Map.of("name", "Vik", "reason", evil));
        String hidden = render("job-hidden-recruiter", Map.of("jobTitle", "Java Dev",
                "reason", evil, "link", "http://localhost:3000/x"));

        assertThat(blocked).doesNotContain("<script>").contains("&lt;script&gt;");
        assertThat(hidden).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}