package com.hiresphere.ai.resume;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeSkillsRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalid(ResumeSkillsRequest request) {
        return validator.validate(request).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void consentMustBeExplicitlyTrue() {
        assertThat(invalid(new ResumeSkillsRequest(true))).isEmpty();
        assertThat(invalid(new ResumeSkillsRequest(false))).containsExactly("consent");
    }

    @Test
    void theMessageShouldTellTheUserWhatToDo() {
        ConstraintViolation<ResumeSkillsRequest> violation =
                validator.validate(new ResumeSkillsRequest(false)).iterator().next();

        assertThat(violation.getMessage()).contains("agree");
    }
}