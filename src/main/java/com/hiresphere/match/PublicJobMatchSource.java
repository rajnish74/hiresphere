package com.hiresphere.match;

import com.hiresphere.job.dto.PublicJobDetail;
import com.hiresphere.job.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicJobMatchSource implements MatchJobSource {

    private final PublicJobService publicJobs;

    @Override
    public JobFacts publicJob(Long jobId) {
        PublicJobDetail job = publicJobs.get(jobId);
        return new JobFacts(job.id(), job.title(), job.companyName(), job.location(), job.workMode(),
                job.minExperienceYears(), job.maxExperienceYears(), job.skills());
    }
}