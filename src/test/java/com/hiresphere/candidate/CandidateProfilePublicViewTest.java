package com.hiresphere.candidate;

import com.hiresphere.candidate.dto.PublicCandidateView;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.ProfileVisibility;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.skill.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateProfilePublicViewTest {

    @Mock
    private CandidateProfileRepository profileRepository;
    @Mock
    private SkillService skillService;

    private CandidateProfileService service;

    @BeforeEach
    void setUp() {
        service = new CandidateProfileService(profileRepository, skillService);
    }

    private CandidateProfile profile(ProfileVisibility visibility) {
        CandidateProfile p = new CandidateProfile();
        p.setUserId(9L);
        p.setHeadline("Java Dev");
        p.setLocation("Patna");
        p.setLinkedinUrl("https://linkedin.com/in/asha");
        p.setVisibility(visibility);
        return p;
    }

    @Test
    void onlyAPublicProfileShouldProduceAPublicView() {
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile(ProfileVisibility.PUBLIC)));

        Optional<PublicCandidateView> view = service.findPublicView(9L, "Asha");

        assertThat(view).isPresent();
        assertThat(view.get().fullName()).isEqualTo("Asha");
        assertThat(view.get().headline()).isEqualTo("Java Dev");
    }

    @Test
    void recruitersOnlyAndPrivateProfilesShouldNeverBePublic() {
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile(ProfileVisibility.RECRUITERS_ONLY)));
        assertThat(service.findPublicView(9L, "Asha")).isEmpty();

        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile(ProfileVisibility.PRIVATE)));
        assertThat(service.findPublicView(9L, "Asha")).isEmpty();
    }

    @Test
    void aMissingProfileShouldGiveNothing() {
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThat(service.findPublicView(9L, "Asha")).isEmpty();
    }

    @Test
    void thePublicViewShouldNotCarryLinkedinOrAnyContactDetail() {
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile(ProfileVisibility.PUBLIC)));

        PublicCandidateView view = service.findPublicView(9L, "Asha").orElseThrow();

        assertThat(view.toString()).doesNotContain("linkedin.com");
    }
}