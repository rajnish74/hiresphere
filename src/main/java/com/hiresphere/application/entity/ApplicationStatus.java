package com.hiresphere.application.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED,
    WITHDRAWN;

    public Set<ApplicationStatus> next() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SHORTLISTED, REJECTED, WITHDRAWN);
            case SHORTLISTED -> EnumSet.of(INTERVIEW, REJECTED, WITHDRAWN);
            case INTERVIEW -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(HIRED, REJECTED, WITHDRAWN);
            case HIRED, REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canMoveTo(ApplicationStatus target) {
        return next().contains(target);
    }

    public Set<ApplicationStatus> nextForRecruiter() {
        EnumSet<ApplicationStatus> allowed = EnumSet.noneOf(ApplicationStatus.class);
        allowed.addAll(next());
        allowed.remove(WITHDRAWN);
        return allowed;
    }
}