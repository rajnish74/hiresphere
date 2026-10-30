package com.hiresphere.candidate;

import com.hiresphere.candidate.dto.SavedJobResponse;
import com.hiresphere.candidate.entity.SavedJob;
import com.hiresphere.candidate.repository.SavedJobRepository;
import com.hiresphere.candidate.service.SavedJobService;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.JobRef;
import com.hiresphere.job.dto.PublicJobSummary;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.JobQueryService;
import com.hiresphere.job.service.PublicJobService;
import com.hiresphere.recruiter.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository repository;
    @Mock
    private JobQueryService jobQueryService;
    @Mock
    private PublicJobService publicJobService;
    @Mock
    private CompanyService companyService;

    private SavedJobService service;

    @BeforeEach
    void setUp() {
        service = new SavedJobService(repository, jobQueryService, publicJobService, companyService);
    }

    private SavedJob saved(long jobId, String title, String company) {
        SavedJob s = new SavedJob();
        ReflectionTestUtils.setField(s, "candidateUserId", 1L);
        ReflectionTestUtils.setField(s, "jobId", jobId);
        ReflectionTestUtils.setField(s, "jobTitle", title);
        ReflectionTestUtils.setField(s, "companyName", company);
        ReflectionTestUtils.setField(s, "createdAt", Instant.parse("2026-10-30T10:00:00Z"));
        return s;
    }

    @Test
    void saveShouldStoreASnapshotOfTheJob() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(new JobRef(10L, "Java Dev", 5L));
        when(repository.existsByCandidateUserIdAndJobId(1L, 10L)).thenReturn(false);
        when(repository.countByCandidateUserId(1L)).thenReturn(0L);
        when(companyService.getNames(Set.of(5L))).thenReturn(Map.of(5L, "Acme"));

        service.save(1L, 10L);

        verify(repository).insertIfAbsent(1L, 10L, "Java Dev", "Acme");
    }

    @Test
    void savingAnAlreadySavedJobShouldBeIdempotent() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(new JobRef(10L, "Java Dev", 5L));
        when(repository.existsByCandidateUserIdAndJobId(1L, 10L)).thenReturn(true);

        service.save(1L, 10L);

        verify(repository, never()).insertIfAbsent(any(), any(), any(), any());
        verify(repository, never()).countByCandidateUserId(any());
    }

    @Test
    void cannotSaveBeyondTheLimit() {
        when(jobQueryService.getPublicJob(10L)).thenReturn(new JobRef(10L, "Java Dev", 5L));
        when(repository.existsByCandidateUserIdAndJobId(1L, 10L)).thenReturn(false);
        when(repository.countByCandidateUserId(1L)).thenReturn(200L);

        assertThatThrownBy(() -> service.save(1L, 10L)).isInstanceOf(AppException.class);

        verify(repository, never()).insertIfAbsent(any(), any(), any(), any());
    }

    @Test
    void savingAJobThatIsNotPublicShouldFailWithoutTouchingTheRepository() {
        when(jobQueryService.getPublicJob(10L)).thenThrow(new ResourceNotFoundException("Job not found"));

        assertThatThrownBy(() -> service.save(1L, 10L)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void removeShouldDeleteTheRow() {
        service.remove(1L, 10L);

        verify(repository).deleteOne(1L, 10L);
    }

    @Test
    void listShouldShowLiveDataForAvailableJobsAndTheSnapshotForTheRest() {
        SavedJob live = saved(10L, "Old title", "Old Co");
        SavedJob gone = saved(11L, "Closed Dev", "Acme");
        when(repository.findByCandidateUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(live, gone)));
        when(publicJobService.findVisibleSummaries(Set.of(10L, 11L))).thenReturn(Map.of(10L,
                new PublicJobSummary(10L, "Java Dev", 5L, "Acme", "Bengaluru", JobType.FULL_TIME,
                        WorkMode.HYBRID, 0, 2, null, null, "INR", null, null)));

        PageResponse<SavedJobResponse> page = service.list(1L, 0, 20);

        SavedJobResponse first = page.content().get(0);
        assertThat(first.available()).isTrue();
        assertThat(first.title()).isEqualTo("Java Dev");   // snapshot nahi, taaza data
        assertThat(first.location()).isEqualTo("Bengaluru");

        SavedJobResponse second = page.content().get(1);
        assertThat(second.available()).isFalse();
        assertThat(second.title()).isEqualTo("Closed Dev");   // snapshot
        assertThat(second.companyName()).isEqualTo("Acme");
        assertThat(second.location()).isNull();
    }
}