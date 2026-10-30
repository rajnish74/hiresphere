package com.hiresphere.candidate;

import com.hiresphere.candidate.entity.ProfileVisibility;
import com.hiresphere.candidate.service.ProfileAccessPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAccessPolicyTest {

    @Test
    void privateProfileShouldBeHiddenUnlessTheCandidateAppliedToThatCompany() {
        assertThat(ProfileAccessPolicy.canRecruiterView(ProfileVisibility.PRIVATE, false)).isFalse();
        assertThat(ProfileAccessPolicy.canRecruiterView(ProfileVisibility.PRIVATE, true)).isTrue();
    }

    @Test
    void publicAndRecruitersOnlyProfilesShouldBeVisibleToApprovedRecruiters() {
        assertThat(ProfileAccessPolicy.canRecruiterView(ProfileVisibility.PUBLIC, false)).isTrue();
        assertThat(ProfileAccessPolicy.canRecruiterView(ProfileVisibility.RECRUITERS_ONLY, false)).isTrue();
    }

    @Test
    void applicationShouldGrantAccessForEveryVisibility() {
        for (ProfileVisibility v : ProfileVisibility.values()) {
            assertThat(ProfileAccessPolicy.canRecruiterView(v, true)).as(v.name()).isTrue();
        }
    }
}