package com.hiresphere.candidate.service;

import com.hiresphere.candidate.entity.ProfileVisibility;

public final class ProfileAccessPolicy {

    private ProfileAccessPolicy() {
    }


    public static boolean canRecruiterView(ProfileVisibility visibility, boolean hasActiveApplication) {
        if (hasActiveApplication) {
            return true;
        }
        return visibility == ProfileVisibility.PUBLIC || visibility == ProfileVisibility.RECRUITERS_ONLY;
    }
}