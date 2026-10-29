package com.hiresphere.application.dto;

import jakarta.validation.constraints.Size;

public record ApplyRequest(@Size(max = 3000) String coverLetter) {
}