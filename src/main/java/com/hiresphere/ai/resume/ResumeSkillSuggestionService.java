package com.hiresphere.ai.resume;

import com.hiresphere.ai.resume.ResumeTextException.Kind;
import com.hiresphere.ai.skills.SkillSuggestionService;
import com.hiresphere.ai.skills.SkillSuggestions;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.util.FixedWindowLimiter;
import com.hiresphere.job.service.SkillMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSkillSuggestionService {

    static final int PER_MINUTE = 10;

    private final ResumeSource source;
    private final PdfResumeTextExtractor extractor;
    private final SkillSuggestionService suggestions;
    private final ProfileSkills profileSkills;

    private final FixedWindowLimiter limiter = new FixedWindowLimiter(PER_MINUTE, Duration.ofMinutes(1));

    public ResumeSkillSuggestions suggest(Long userId) {
        if (!limiter.tryAcquire("user:" + userId)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please slow down.");
        }

        ResumeFile file = source.read(userId)
                .orElseThrow(() -> new ResumeTextException(Kind.NO_RESUME, "no resume on the profile"));
        ExtractedText extracted = extractor.extract(file.bytes());
        PiiRedactor.Result clean = PiiRedactor.redact(extracted.text());

        SkillSuggestions result = suggestions.suggest(userId, clean.text());
        Set<String> onProfile = profileSkills.keysOf(userId);

        List<ResumeSkillSuggestions.SuggestedSkill> skills = result.skills().stream()
                .map(name -> new ResumeSkillSuggestions.SuggestedSkill(name, onProfile.contains(SkillMatch.key(name))))
                .toList();

        log.info("Resume skill suggestions for user {}: {} skill(s), source {}, {} contact detail(s) removed",
                userId, skills.size(), result.source(), clean.total());

        return new ResumeSkillSuggestions(file.name(), skills, result.source().name(), result.degraded(),
                result.degradedReason(), result.cached(), extracted.cutShort() || result.inputTruncated(),
                extracted.pagesRead(), extracted.totalPages(), clean.total());
    }
}