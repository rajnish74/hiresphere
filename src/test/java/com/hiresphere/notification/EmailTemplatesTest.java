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
class EmailTemplatesTest {

    private static final String[] TEMPLATES = {
            "application-submitted-candidate", "application-received-recruiter",
            "application-withdrawn-recruiter", "application-status-candidate"};

    @Autowired
    private TemplateEngine templateEngine;

    private String render(String template, Map<String, Object> model) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariables(model);
        return templateEngine.process("email/" + template, ctx);
    }

    @Test
    void everyTemplateShouldRenderWithItsData() {
        Map<String, Object> model = Map.of("name", "Asha", "candidateName", "Asha", "jobTitle", "Java Dev",
                "companyName", "Acme", "link", "http://localhost:3000/x", "status", "SHORTLISTED",
                "message", "msg", "note", "Well done");

        for (String template : TEMPLATES) {
            assertThat(render(template, model)).as(template).contains("Java Dev");
        }
    }

    @Test
    void htmlInUserSuppliedFieldsShouldBeEscaped() {
        String evil = "<script>alert(1)</script>";
        Map<String, Object> model = Map.of("name", evil, "candidateName", evil, "jobTitle", evil,
                "companyName", evil, "link", "http://localhost:3000/x", "status", "SHORTLISTED",
                "message", "msg", "note", evil);

        for (String template : TEMPLATES) {
            String html = render(template, model);
            assertThat(html).as(template).doesNotContain("<script>");
            assertThat(html).as(template).contains("&lt;script&gt;");
        }
    }
}