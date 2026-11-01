package com.hiresphere.job;

import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.job.entity.WorkMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobSearchCriteriaTest {

    private final JobSearchCriteria criteria = new JobSearchCriteria("java", "Pune",
            List.of(JobType.FULL_TIME), List.of(WorkMode.REMOTE), 2, null, List.of("java"), null, 7, 5L);

    @Test
    void withoutJobTypesShouldDropOnlyTheJobTypeFilter() {
        JobSearchCriteria c = criteria.withoutJobTypes();

        assertThat(c.jobTypes()).isNull();
        assertThat(c.workModes()).containsExactly(WorkMode.REMOTE);
        assertThat(c.keyword()).isEqualTo("java");
        assertThat(c.companyId()).isEqualTo(5L);
    }

    @Test
    void withoutWorkModesShouldDropOnlyTheWorkModeFilter() {
        JobSearchCriteria c = criteria.withoutWorkModes();

        assertThat(c.workModes()).isNull();
        assertThat(c.jobTypes()).containsExactly(JobType.FULL_TIME);
        assertThat(c.postedWithinDays()).isEqualTo(7);
    }

    @Test
    void olderConstructorsShouldLeaveNewFiltersEmpty() {
        assertThat(new JobSearchCriteria("x", null, null, null, null, null, null, null).companyId()).isNull();
        assertThat(new JobSearchCriteria("x", null, null, null, null, null, null, null, 7).companyId()).isNull();
    }
}