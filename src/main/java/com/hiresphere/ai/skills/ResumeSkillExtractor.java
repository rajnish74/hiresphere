
package com.hiresphere.ai.skills;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hiresphere.ai.AiCallContext;
import com.hiresphere.ai.AiException;
import com.hiresphere.ai.AiException.Kind;
import com.hiresphere.ai.cache.AiResultCache;
import com.hiresphere.ai.json.JsonSchemas;
import com.hiresphere.ai.json.StructuredAi;
import com.hiresphere.ai.metrics.AiMetrics;
import com.hiresphere.ai.prompt.PromptTemplate;
import com.hiresphere.ai.prompt.UntrustedText;
import com.hiresphere.ai.usage.AiUsageStore;
import com.hiresphere.ai.usage.UsageEntry;
import com.hiresphere.ai.usage.UsageLog;
import com.hiresphere.ai.usage.UsageStatus;
import com.hiresphere.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ResumeSkillExtractor {

    public static final int MAX_RESUME_CHARS = 12_000;
    public static final int MAX_SKILLS = 40;

    private static final Map<String, Object> SCHEMA = JsonSchemas.object(Map.of(
            "skills", JsonSchemas.array(JsonSchemas.string("One skill, common short spelling"), 60)));

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(List<String> skills) {
    }

    private final StructuredAi ai;
    private final AiResultCache cache;
    private final AiUsageStore usage;
    private final AiMetrics metrics;

    @Autowired
    public ResumeSkillExtractor(StructuredAi ai, AiResultCache cache, AiUsageStore usage, AiMetrics metrics) {
        this.ai = ai;
        this.cache = cache;
        this.usage = usage;
        this.metrics = metrics;
    }

    public ResumeSkillExtractor(StructuredAi ai, AiResultCache cache, AiUsageStore usage) {
        this(ai, cache, usage, AiMetrics.standalone());
    }

    public ExtractedSkills extract(String resumeText) {
        UntrustedText.Wrapped resume = UntrustedText.wrap("RESUME", resumeText, MAX_RESUME_CHARS);

        if (resume.text().isBlank()) {
            throw new BadRequestException("There is no text to analyse");
        }

        String system = PromptTemplate.load("skills-extract.system").render(Map.of());
        PromptTemplate userTemplate = PromptTemplate.load("skills-extract.user");
        String key = AiResultCache.keyOf("skills-extract", system,
                userTemplate.render(Map.of("resume", resume.text())), String.valueOf(MAX_SKILLS), SCHEMA.toString());

        AiResultCache.Lookup<ExtractedSkills> lookup = cache.get(key, () -> ask(resume, system, userTemplate));

        if (!lookup.hit()) {
            return lookup.value();
        }

        AiCallContext.Context context = AiCallContext.current();
        UsageLog.quietly(usage, new UsageEntry(context.userId(), context.feature(), null, UsageStatus.CACHED, null,
                0, 0, 0, 0));
        metrics.cached(context.feature());

        ExtractedSkills saved = lookup.value();
        return new ExtractedSkills(saved.skills(), saved.droppedCount(), saved.inputTruncated(), null);
    }

    private ExtractedSkills ask(UntrustedText.Wrapped resume, String system, PromptTemplate userTemplate) {
        String prompt = userTemplate.render(Map.of("resume", resume.block()));
        StructuredAi.Structured<Answer> result = ai.ask(system, prompt, SCHEMA, Answer.class);

        List<String> proposed = result.value().skills();

        if (proposed == null) {
            throw new AiException(Kind.INVALID_OUTPUT, "The 'skills' field is missing");
        }

        List<String> kept = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int dropped = 0;

        for (String candidate : proposed) {
            String skill = candidate == null ? "" : candidate.strip().replaceAll("\\s+", " ");

            if (!SkillText.isPlausible(skill) || !SkillText.appearsIn(skill, resume.text(), true)) {
                dropped++;
                continue;
            }

            if (!seen.add(skill.toLowerCase(Locale.ROOT))) {
                continue;
            }

            kept.add(skill);

            if (kept.size() == MAX_SKILLS) {
                break;
            }
        }

        if (dropped > 0) {
            log.info("Skill extraction: kept {}, dropped {} that failed the shape or grounding check",
                    kept.size(), dropped);
        }

        return new ExtractedSkills(List.copyOf(kept), dropped, resume.truncated(), result.response());
    }
}
