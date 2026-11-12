package com.hiresphere.match.explain;

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
import com.hiresphere.match.MatchBreakdown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class MatchExplainer {

    public static final String FEATURE = "MATCH_EXPLAIN";

    private static final Map<String, Object> SCHEMA = JsonSchemas.object(Map.of(
            "summary", JsonSchemas.string("One or two sentences about the overall fit"),
            "strengths", JsonSchemas.array(JsonSchemas.string("One short sentence about what fits"), 3),
            "gaps", JsonSchemas.array(JsonSchemas.string("One short sentence about what is missing"), 3),
            "tip", JsonSchemas.string("One short practical sentence")));

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Answer(String summary, List<String> strengths, List<String> gaps, String tip) {
    }

    private final StructuredAi ai;
    private final AiResultCache cache;
    private final AiUsageStore usage;
    private final AiMetrics metrics;
    private final ExplanationValidator validator;

    public MatchExplainer(StructuredAi ai, AiResultCache cache, AiUsageStore usage, AiMetrics metrics,
                          ExplanationValidator validator) {
        this.ai = ai;
        this.cache = cache;
        this.usage = usage;
        this.metrics = metrics;
        this.validator = validator;
    }

    public MatchExplanation explain(Long userId, MatchBreakdown breakdown, String jobTitle, String company) {
        if (breakdown.fitScore() == null) {
            return MatchExplanation.template(TemplateExplainer.explain(breakdown), null);
        }
        String sheet = FactSheet.build(breakdown, jobTitle, company);
        return AiCallContext.with(FEATURE, userId, () -> viaAi(breakdown, sheet));
    }

    public MatchExplanation templateOnly(MatchBreakdown breakdown) {
        return MatchExplanation.template(TemplateExplainer.explain(breakdown), null);
    }

    private MatchExplanation viaAi(MatchBreakdown breakdown, String sheet) {
        String system = PromptTemplate.load("match-explain.system").render(Map.of());
        PromptTemplate userTemplate = PromptTemplate.load("match-explain.user");
        UntrustedText.Wrapped facts = UntrustedText.wrap("FACTS", sheet, 4000);
        String key = AiResultCache.keyOf("match-explain", system,
                userTemplate.render(Map.of("facts", facts.text())), SCHEMA.toString());
        try {
            AiResultCache.Lookup<ExplanationText> lookup = cache.get(key, () -> ask(system, userTemplate, facts));
            if (lookup.hit()) {
                AiCallContext.Context context = AiCallContext.current();
                UsageLog.quietly(usage, new UsageEntry(context.userId(), context.feature(), null, UsageStatus.CACHED,
                        null, 0, 0, 0, 0));
                metrics.cached(context.feature());
            }
            return MatchExplanation.ai(lookup.value(), lookup.hit());
        } catch (AiException e) {
            log.warn("Match explanation fell back to the template: {}", e.getKind());
            return MatchExplanation.template(TemplateExplainer.explain(breakdown), e.getKind().name());
        }
    }

    private ExplanationText ask(String system, PromptTemplate userTemplate, UntrustedText.Wrapped facts) {
        String prompt = userTemplate.render(Map.of("facts", facts.block()));
        Answer answer = ai.ask(system, prompt, SCHEMA, Answer.class).value();

        ExplanationValidator.Outcome outcome =
                validator.validate(answer.summary(), answer.strengths(), answer.gaps(), answer.tip(), facts.text());
        if (!outcome.ok()) {
            log.warn("Match explanation rejected: {}", outcome.problem());
            throw new AiException(Kind.INVALID_OUTPUT, "explanation rejected: " + outcome.problem());
        }
        return outcome.text();
    }
}