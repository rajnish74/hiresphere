package com.hiresphere.application.dto;

public record ApplyPrefill(String fullName, String email, String phone, String location, String headline,
                           boolean hasResume, String resumeName) {
}