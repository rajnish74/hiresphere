package com.hiresphere.application.service;

import com.hiresphere.application.dto.JobFlags;
import com.hiresphere.application.dto.JobStatusRow;
import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.repository.JobApplicationRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.SavedJobService;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.job.service.JobQueryService;
import com.hiresphere.job.service.SkillMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobFlagsService {

    static final int MAX_IDS = 50;

    private final JobApplicationRepository applicationRepository;
    private final SavedJobService savedJobService;
    private final CandidateProfileService candidateProfileService;
    private final JobQueryService jobQueryService;

    @Transactional(readOnly = true)
    public Map<Long, JobFlags> flags(Long userId, Collection<Long> jobIds) {
        Set<Long> ids = new LinkedHashSet<>(jobIds);
        if (ids.size() > MAX_IDS) {
            throw new BadRequestException("At most " + MAX_IDS + " job ids per request");
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<Long> saved = savedJobService.savedAmong(userId, ids);
        Map<Long, ApplicationStatus> applied = applicationRepository.findStatusesByJobIds(userId, ids).stream()
                .collect(Collectors.toMap(JobStatusRow::jobId, JobStatusRow::status));

        Set<String> mine = candidateProfileService.findSkillKeys(userId);
        Map<Long, Set<String>> jobSkills = mine.isEmpty() ? Map.of() : jobQueryService.publicJobSkillKeys(ids);

        Map<Long, JobFlags> result = new LinkedHashMap<>();
        for (Long id : ids) {
            result.put(id, new JobFlags(saved.contains(id), applied.get(id),
                    SkillMatch.percent(mine, jobSkills.get(id))));
        }
        return result;
    }
}