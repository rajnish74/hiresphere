package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;


public record JobFlags(boolean saved, ApplicationStatus applicationStatus, Integer matchPercent) {

    public JobFlags(boolean saved, ApplicationStatus applicationStatus) {
        this(saved, applicationStatus, null);
    }
}