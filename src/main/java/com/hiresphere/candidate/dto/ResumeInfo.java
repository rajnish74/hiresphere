package com.hiresphere.candidate.dto;

import com.hiresphere.candidate.entity.CandidateProfile;

import java.time.Instant;

public record ResumeInfo(String originalName, Long sizeBytes, Instant uploadedAt) {


    public static ResumeInfo from(CandidateProfile p) {
        if (p.getResumeStorageKey() == null) {
            return null;
        }
        return new ResumeInfo(
                p.getResumeOriginalName(),
                p.getResumeSizeBytes(),
                p.getResumeUploadedAt());
    }
}