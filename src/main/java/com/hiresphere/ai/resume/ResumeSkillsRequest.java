package com.hiresphere.ai.resume;

import jakarta.validation.constraints.AssertTrue;

public record ResumeSkillsRequest(
        @AssertTrue(message = "Please agree to the resume analysis before continuing") boolean consent) {
}