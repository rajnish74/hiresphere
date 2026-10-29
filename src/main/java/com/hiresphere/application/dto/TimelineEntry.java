package com.hiresphere.application.dto;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.ApplicationStatusHistory;

import java.time.Instant;

public record TimelineEntry(ApplicationStatus from, ApplicationStatus to, Instant at, String note) {

    public static TimelineEntry from(ApplicationStatusHistory h) {
        return new TimelineEntry(h.getFromStatus(), h.getToStatus(), h.getCreatedAt(), h.getNote());
    }
}