package com.hiresphere.match;

import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.job.dto.PublicJobDetail;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.SalaryPeriod;
import com.hiresphere.job.entity.WorkMode;
import com.hiresphere.job.service.PublicJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicJobMatchSourceTest {

    @Mock
    private PublicJobService publicJobs;

    @Test
    void shouldCarryOnlyWhatTheMatchNeeds() {
        when(publicJobs.get(10L)).thenReturn(new PublicJobDetail(10L, "Java Developer", "Long description", 5L, "Acme",
                "Pune", JobType.FULL_TIME, WorkMode.HYBRID, 2, 6, new BigDecimal("600000"), new BigDecimal("900000"),
                "INR", 3, List.of("Java", "Docker"), Instant.now(), null, 4, SalaryPeriod.YEAR));

        JobFacts facts = new PublicJobMatchSource(publicJobs).publicJob(10L);

        assertThat(facts.id()).isEqualTo(10L);
        assertThat(facts.title()).isEqualTo("Java Developer");
        assertThat(facts.companyName()).isEqualTo("Acme");
        assertThat(facts.location()).isEqualTo("Pune");
        assertThat(facts.workMode()).isEqualTo(WorkMode.HYBRID);
        assertThat(facts.minYears()).isEqualTo(2);
        assertThat(facts.maxYears()).isEqualTo(6);
        assertThat(facts.skills()).containsExactly("Java", "Docker");
    }

    @Test
    void aJobThatIsNotPublicShouldStayNotFound() {
        when(publicJobs.get(99L)).thenThrow(new ResourceNotFoundException("Job not found"));

        assertThatThrownBy(() -> new PublicJobMatchSource(publicJobs).publicJob(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}