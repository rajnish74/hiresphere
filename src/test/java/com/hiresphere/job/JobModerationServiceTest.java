package com.hiresphere.job;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.event.JobHiddenEvent;
import com.hiresphere.job.repository.JobRepository;
import com.hiresphere.job.service.JobModerationService;
import com.hiresphere.recruiter.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobModerationServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private CompanyService companyService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JobModerationService service;

    @BeforeEach
    void setUp() {
        service = new JobModerationService(jobRepository, companyService, eventPublisher);
    }

    private Job job() {
        Job j = new Job();
        j.setId(20L);
        j.setCompanyId(5L);
        j.setTitle("Java Dev");
        return j;
    }

    @Test
    void hideIfNotHiddenShouldHideTheJobAndTellTheRecruiter() {
        Job job = job();
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));

        boolean hidden = service.hideIfNotHidden(20L, "  Misleading salary ");

        assertThat(hidden).isTrue();
        assertThat(job.isHidden()).isTrue();
        verify(eventPublisher).publishEvent(new JobHiddenEvent(20L, 5L, "Java Dev", "Misleading salary"));
    }

    @Test
    void hideIfNotHiddenShouldDoNothingForAnAlreadyHiddenJob() {
        Job job = job();
        job.hideByAdmin("Earlier reason");
        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));

        boolean hidden = service.hideIfNotHidden(20L, "Another reason");

        assertThat(hidden).isFalse();
        assertThat(job.getModerationReason()).isEqualTo("Earlier reason");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}