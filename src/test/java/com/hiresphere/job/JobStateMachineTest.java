package com.hiresphere.job;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobStateMachineTest {

    @Test
    void newJobShouldBeDraft() {
        assertThat(new Job().getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void draftCanBePublished() {
        Job job = new Job();
        job.publish();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(job.getPublishedAt()).isNotNull();
    }

    @Test
    void draftCannotBeClosedDirectly() {
        assertThatThrownBy(() -> new Job().close()).isInstanceOf(AppException.class);
    }

    @Test
    void publishedJobCannotBePublishedAgain() {
        Job job = new Job();
        job.publish();

        assertThatThrownBy(job::publish).isInstanceOf(AppException.class);
    }

    @Test
    void closedJobCanBeReopenedAndKeepsOriginalPublishDate() {
        Job job = new Job();
        job.publish();
        Instant firstPublish = job.getPublishedAt();
        job.close();
        assertThat(job.getClosedAt()).isNotNull();

        job.publish();

        assertThat(job.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        assertThat(job.getClosedAt()).isNull();
        assertThat(job.getPublishedAt()).isEqualTo(firstPublish);
    }
}