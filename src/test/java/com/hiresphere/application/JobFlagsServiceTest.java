package com.hiresphere.application;

import com.hiresphere.application.dto.JobFlags;
import com.hiresphere.application.dto.JobStatusRow;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.application.service.JobFlagsService;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.SavedJobService;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.job.service.JobQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobFlagsServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private SavedJobService savedJobService;
    @Mock
    private CandidateProfileService candidateProfileService;
    @Mock
    private JobQueryService jobQueryService;

    private JobFlagsService service;

    @BeforeEach
    void setUp() {
        service = new JobFlagsService(applicationRepository, savedJobService, candidateProfileService,
                jobQueryService);
    }

    @Test
    void shouldCombineSavedAndApplicationStatusPerJob() {
        when(savedJobService.savedAmong(1L, Set.of(10L, 11L, 12L))).thenReturn(Set.of(10L));
        when(applicationRepository.findStatusesByJobIds(1L, Set.of(10L, 11L, 12L)))
                .thenReturn(List.of(new JobStatusRow(11L, ApplicationStatus.SHORTLISTED)));

        Map<Long, JobFlags> flags = service.flags(1L, List.of(10L, 11L, 12L));

        assertThat(flags.get(10L)).isEqualTo(new JobFlags(true, null));
        assertThat(flags.get(11L)).isEqualTo(new JobFlags(false, ApplicationStatus.SHORTLISTED));
        assertThat(flags.get(12L)).isEqualTo(new JobFlags(false, null));
    }

    @Test
    void duplicateIdsShouldBeAnsweredOnce() {
        when(savedJobService.savedAmong(1L, Set.of(10L))).thenReturn(Set.of());
        when(applicationRepository.findStatusesByJobIds(1L, Set.of(10L))).thenReturn(List.of());

        assertThat(service.flags(1L, List.of(10L, 10L, 10L))).hasSize(1);
    }

    @Test
    void noIdsShouldReturnNothingWithoutTouchingAnything() {
        assertThat(service.flags(1L, List.of())).isEmpty();

        verifyNoInteractions(applicationRepository, savedJobService, candidateProfileService, jobQueryService);
    }

    @Test
    void tooManyIdsShouldBeRejected() {
        List<Long> ids = LongStream.rangeClosed(1, 51).boxed().toList();

        assertThatThrownBy(() -> service.flags(1L, ids)).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(applicationRepository, savedJobService, candidateProfileService, jobQueryService);
    }

    @Test
    void matchPercentShouldComeFromTheSkillOverlap() {
        when(candidateProfileService.findSkillKeys(1L)).thenReturn(Set.of("java", "spring boot", "sql"));
        when(jobQueryService.publicJobSkillKeys(Set.of(10L, 11L, 12L))).thenReturn(Map.of(
                10L, Set.of("java", "spring boot", "postgresql"),
                11L, Set.of()));   // 12 map mein nahi: job ab public nahi

        Map<Long, JobFlags> flags = service.flags(1L, List.of(10L, 11L, 12L));

        assertThat(flags.get(10L).matchPercent()).isEqualTo(67);
        assertThat(flags.get(11L).matchPercent()).isNull();   // job mein koi skill nahi
        assertThat(flags.get(12L).matchPercent()).isNull();   // hidden ya draft: skills leak nahi
    }

    @Test
    void aCandidateWithoutSkillsShouldNotTriggerAnyJobSkillLookup() {
        when(candidateProfileService.findSkillKeys(1L)).thenReturn(Set.of());

        Map<Long, JobFlags> flags = service.flags(1L, List.of(10L));

        assertThat(flags.get(10L).matchPercent()).isNull();
        verifyNoInteractions(jobQueryService);
    }
}