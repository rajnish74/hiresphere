package com.hiresphere.match;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.FixedWindowLimiter;
import com.hiresphere.match.explain.MatchExplainer;
import com.hiresphere.match.explain.MatchExplanation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;


@Service
@RequiredArgsConstructor
public class JobMatchService {

    static final int PER_MINUTE = 30;

    private final MatchJobSource jobs;
    private final MatchProfileSource profiles;
    private final MatchExplainer explainer;

    private final FixedWindowLimiter limiter = new FixedWindowLimiter(PER_MINUTE, Duration.ofMinutes(1));

    public JobMatchResponse match(Long userId, Long jobId, boolean withAi) {
        if (!limiter.tryAcquire("user:" + userId)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down.");
        }

        JobFacts job = jobs.publicJob(jobId);
        CandidateFacts me = profiles.factsOf(userId);

        MatchBreakdown breakdown = MatchCalculator.calculate(new MatchInput(new ArrayList<>(me.skillKeys()),
                job.skills(), me.experienceYears(), job.minYears(), job.maxYears(), me.location(), job.location(),
                job.workMode()));

        MatchExplanation explanation = withAi
                ? explainer.explain(userId, breakdown, job.title(), job.companyName())
                : explainer.templateOnly(breakdown);
        return JobMatchResponse.of(job, breakdown, explanation);
    }
}