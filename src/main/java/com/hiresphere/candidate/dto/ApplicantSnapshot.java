package com.hiresphere.candidate.dto;

public record ApplicantSnapshot(String headline, String resumeStorageKey, String resumeOriginalName,
                                String location) {

    public ApplicantSnapshot(String headline, String resumeStorageKey, String resumeOriginalName) {
        this(headline, resumeStorageKey, resumeOriginalName, null);
    }
}