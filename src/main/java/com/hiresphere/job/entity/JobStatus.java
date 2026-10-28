package com.hiresphere.job.entity;

public enum JobStatus {
    DRAFT,
    PUBLISHED,
    CLOSED;

    public boolean canMoveTo(JobStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> target == CLOSED;
            case CLOSED -> target == PUBLISHED;   // reopen
        };
    }
}