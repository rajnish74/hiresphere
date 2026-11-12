package com.hiresphere.candidate;

import com.hiresphere.candidate.entity.CandidateProfile;
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
class CandidateProfileFindLocationTest {

    @Mock
    private CandidateProfileRepository profileRepository;
    @Mock
    private SkillService skillService;

    private CandidateProfileService service;

    @BeforeEach
    void setUp() {
        service = new CandidateProfileService(profileRepository, skillService);
    }

    private CandidateProfile withLocation(String location) {
        CandidateProfile p = new CandidateProfile();
        p.setLocation(location);
        return p;
    }

    @Test
    void shouldGiveTheProfilesLocation() {
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(withLocation("Patna, Bihar")));

        assertThat(service.findLocation(7L)).contains("Patna, Bihar");
    }

    @Test
    void aMissingBlankOrAbsentProfileShouldGiveNothing() {
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(withLocation(null)));
        when(profileRepository.findByUserId(8L)).thenReturn(Optional.of(withLocation("   ")));
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThat(service.findLocation(7L)).isEmpty();
        assertThat(service.findLocation(8L)).isEmpty();
        assertThat(service.findLocation(9L)).isEmpty();
    }
}