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
class AuthEmailTemplatesTest {

    private static final String[] TEMPLATES = {"email-verification", "password-reset", "password-changed"};

    @Autowired
    private TemplateEngine templateEngine;

    private String render(String template, Map<String, Object> model) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariables(model);
        return templateEngine.process("email/" + template, ctx);
    }

    @Test
    void everyTemplateShouldRenderTheNameAndLink() {
        Map<String, Object> model = Map.of("name", "Asha", "link", "http://localhost:3000/x?token=abc",
                "validFor", "30 minutes");

        for (String template : TEMPLATES) {
            String html = render(template, model);
            assertThat(html).as(template).contains("Asha").contains("http://localhost:3000/x?token=abc");
        }
    }

    @Test
    void htmlInTheNameShouldBeEscaped() {
        Map<String, Object> model = Map.of("name", "<script>alert(1)</script>",
                "link", "http://localhost:3000/x", "validFor", "30 minutes");

        for (String template : TEMPLATES) {
            String html = render(template, model);
            assertThat(html).as(template).doesNotContain("<script>").contains("&lt;script&gt;");
        }
    }
}