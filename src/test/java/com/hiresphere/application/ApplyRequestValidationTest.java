package com.hiresphere.application;

import com.hiresphere.application.dto.ApplyRequest;
import com.hiresphere.user.dto.UpdateMeRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private <T> Set<String> invalidFields(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void aCompleteValidRequestShouldPass() {
        assertThat(invalidFields(new ApplyRequest("Hi", "+919876543210", "Patna",
                new BigDecimal("900000"), LocalDate.now().plusDays(10)))).isEmpty();
    }

    @Test
    void allOptionalFieldsMayBeMissing() {
        assertThat(invalidFields(new ApplyRequest(null, null, null, null, null))).isEmpty();
    }

    @Test
    void aBadPhoneShouldBeRejected() {
        assertThat(invalidFields(new ApplyRequest(null, "12ab", null, null, null))).containsExactly("phone");
        assertThat(invalidFields(new ApplyRequest(null, "123", null, null, null))).containsExactly("phone");
    }

    @Test
    void aNegativeSalaryShouldBeRejected() {
        assertThat(invalidFields(new ApplyRequest(null, null, null, new BigDecimal("-1"), null)))
                .containsExactly("expectedSalary");
    }

    @Test
    void anAvailabilityDateInThePastShouldBeRejected() {
        assertThat(invalidFields(new ApplyRequest(null, null, null, null, LocalDate.now().minusDays(1))))
                .containsExactly("availableFrom");
    }

    @Test
    void accountUpdateShouldNeedANameAndAValidPhone() {
        assertThat(invalidFields(new UpdateMeRequest(" ", null))).contains("fullName");
        assertThat(invalidFields(new UpdateMeRequest("Asha", "abc"))).containsExactly("phone");
        assertThat(invalidFields(new UpdateMeRequest("Asha Verma", "+919876543210"))).isEmpty();
        assertThat(invalidFields(new UpdateMeRequest("Asha Verma", null))).isEmpty();
    }
}