package com.hiresphere.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplyRequest(
        @Size(max = 3000) String coverLetter,
        @Pattern(regexp = "^\\+?[0-9]{8,14}$", message = "Phone must be 8-14 digits, optionally starting with +")
        String phone,
        @Size(max = 100) String currentLocation,
        @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal expectedSalary,
        @FutureOrPresent LocalDate availableFrom) {
}