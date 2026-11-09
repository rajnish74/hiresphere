package com.hiresphere.ai.skills;

import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.prompt.UntrustedText;
import com.hiresphere.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillSuggestionService {

    private final ResumeSkillExtractor extractor;
    private final KnownSkillSource knownSkills;
    public static final String FEATURE = "SKILLS_EXTRACT";

    public SkillSuggestions suggest(String resumeText) {
        UntrustedText.Cleaned cleaned = UntrustedText.clean(resumeText, ResumeSkillExtractor.MAX_RESUME_CHARS);
        if (cleaned.text().isBlank()) {
            throw new BadRequestException("There is no text to analyse");
        }
        try {
            ExtractedSkills result = extractor.extract(resumeText);
            return new SkillSuggestions(result.skills(), SkillSuggestions.Source.AI, null,
                    result.inputTruncated(), result.droppedCount(), result.ai());
        } catch (AiException e) {
            log.warn("Skill suggestion fell back to keyword matching: {}", e.getKind());
            List<String> found = KeywordSkillMatcher.match(cleaned.text(), knownSkills.names(),
                    ResumeSkillExtractor.MAX_SKILLS);
            return new SkillSuggestions(found, SkillSuggestions.Source.KEYWORD, e.getKind().name(),
                    cleaned.truncated(), 0, null);
        }
    }

    public SkillSuggestions suggest(Long userId, String resumeText) {
        return AiCallContext.with(FEATURE, userId, () -> suggest(resumeText));
    }
}