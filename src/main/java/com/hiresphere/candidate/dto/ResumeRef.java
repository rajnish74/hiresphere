package com.hiresphere.candidate.dto;

public record ResumeRef(String storageKey, String originalName) {

    public ResumeRef {
        originalName = originalName == null || originalName.isBlank() ? "resume.pdf" : originalName;
    }
}