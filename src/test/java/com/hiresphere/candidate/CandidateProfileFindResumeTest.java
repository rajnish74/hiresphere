package com.hiresphere.candidate;

import com.hiresphere.candidate.dto.ResumeRef;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.skill.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateProfileFindResumeTest {

    @Mock
    private CandidateProfileRepository profileRepository;
    @Mock
    private SkillService skillService;

    private CandidateProfileService service;

    @BeforeEach
    void setUp() {
        service = new CandidateProfileService(profileRepository, skillService);
    }

    private CandidateProfile profileWithResume(String key, String name) {
        CandidateProfile p = new CandidateProfile();
        ReflectionTestUtils.setField(p, "resumeStorageKey", key);
        ReflectionTestUtils.setField(p, "resumeOriginalName", name);
        return p;
    }

    @Test
    void aProfileWithAResumeShouldGiveItsStorageKeyAndName() {
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(profileWithResume("key-1", "asha-cv.pdf")));

        assertThat(service.findResume(7L)).contains(new ResumeRef("key-1", "asha-cv.pdf"));
    }

    @Test
    void aProfileWithoutAResumeOrWithoutAProfileShouldGiveNothing() {
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(profileWithResume(null, null)));
        when(profileRepository.findByUserId(8L)).thenReturn(Optional.empty());

        assertThat(service.findResume(7L)).isEmpty();
        assertThat(service.findResume(8L)).isEmpty();
    }

    @Test
    void aMissingOriginalNameShouldFallBackToAPlainName() {
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(profileWithResume("key-1", "  ")));

        assertThat(service.findResume(7L).orElseThrow().originalName()).isEqualTo("resume.pdf");
    }
}