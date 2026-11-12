
package com.hiresphere.match.explain;

import com.hiresphere.ai.skills.KnownSkillSource;
import com.hiresphere.ai.skills.SkillText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ExplanationValidator {

    static final int MAX_SUMMARY = 320;
    static final int MAX_ITEM = 180;
    static final int MAX_TIP = 220;
    static final int MAX_ITEMS = 3;

    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final String BAD_CHARS = "<>@*#`";
    private static final List<String> FORBIDDEN = List.of("guarantee", "will get the job", "will be hired",
            "do not apply", "don't apply", "dont apply", "should not apply", "shouldn't apply", "waste of time",
            "unqualified");
    private static final Pattern SENSITIVE = Pattern.compile("(?i)\\b(age|aged|gender|male|female|woman|women|"
            + "religion|religious|caste|marital|married|nationality|pregnan\\w*)\\b");

    /** Aam angrezi shabd jo skill ke naam bhi hain: inhe check karna jhoothi reject deta hai, to skip. Known limit. */
    private static final Set<String> AMBIGUOUS = Set.of("go", "swift", "rust", "spring", "excel", "word", "ruby",
            "spark", "express", "unity", "chef", "ant", "make");

    public record Outcome(ExplanationText text, String problem) {
        public boolean ok() {
            return problem == null;
        }

        static Outcome accepted(ExplanationText text) {
            return new Outcome(text, null);
        }

        static Outcome rejected(String problem) {
            return new Outcome(null, problem);
        }
    }

    private final KnownSkillSource knownSkills;

    public Outcome validate(String summary, List<String> strengths, List<String> gaps, String tip, String factSheet) {
        String cleanSummary = tidy(summary);
        String cleanTip = tidy(tip);
        List<String> cleanStrengths = tidyAll(strengths);
        List<String> cleanGaps = tidyAll(gaps);

        if (cleanSummary.isEmpty()) {
            return Outcome.rejected("the summary is empty");
        }
        if (cleanTip.isEmpty()) {
            return Outcome.rejected("the tip is empty");
        }
        if (cleanStrengths.size() > MAX_ITEMS || cleanGaps.size() > MAX_ITEMS) {
            return Outcome.rejected("more than " + MAX_ITEMS + " items");
        }
        if (cleanSummary.length() > MAX_SUMMARY || cleanTip.length() > MAX_TIP
                || cleanStrengths.stream().anyMatch(s -> s.length() > MAX_ITEM)
                || cleanGaps.stream().anyMatch(s -> s.length() > MAX_ITEM)) {
            return Outcome.rejected("a sentence is too long");
        }

        Set<String> allowedNumbers = digitsIn(factSheet);
        List<String> all = new ArrayList<>(List.of(cleanSummary, cleanTip));
        all.addAll(cleanStrengths);
        all.addAll(cleanGaps);
        for (String text : all) {
            String problem = check(text, allowedNumbers, factSheet);
            if (problem != null) {
                return Outcome.rejected(problem);
            }
        }
        return Outcome.accepted(new ExplanationText(cleanSummary, cleanStrengths, cleanGaps, cleanTip));
    }

    private String check(String text, Set<String> allowedNumbers, String factSheet) {
        for (char c : BAD_CHARS.toCharArray()) {
            if (text.indexOf(c) >= 0) {
                return "contains the character '" + c + "'";
            }
        }
        String lower = text.toLowerCase();
        if (lower.contains("http") || lower.contains("www.")) {
            return "contains a link";
        }
        if (text.codePoints().anyMatch(cp -> Character.getType(cp) == Character.OTHER_SYMBOL)) {
            return "contains an emoji or a symbol";
        }
        if (mostlyNotEnglish(text)) {
            return "is not mostly English";
        }
        Matcher numbers = DIGITS.matcher(text);
        while (numbers.find()) {
            if (!allowedNumbers.contains(numbers.group())) {
                return "invented number " + numbers.group();
            }
        }
        for (String phrase : FORBIDDEN) {
            if (lower.contains(phrase)) {
                return "makes a promise or a decision: " + phrase;
            }
        }
        if (SENSITIVE.matcher(text).find()) {
            return "mentions a personal attribute";
        }
        return inventedSkill(text, factSheet);
    }

    private String inventedSkill(String text, String factSheet) {
        List<String> names = knownSkills.names();
        if (names == null) {
            return null;
        }
        for (String raw : names) {
            String name = raw == null ? "" : raw.strip();
            if (name.length() < 2 || AMBIGUOUS.contains(name.toLowerCase(Locale.ROOT)) || !text.contains(name)) {
                continue;
            }
            if (SkillText.appearsIn(name, text, false) && !SkillText.appearsIn(name, factSheet, true)) {
                return "mentions a skill that is not in the facts: " + name;
            }
        }
        return null;
    }

    private static boolean mostlyNotEnglish(String text) {
        long letters = text.codePoints().filter(Character::isLetter).count();
        long outside = text.codePoints().filter(cp -> Character.isLetter(cp) && cp > 127).count();
        return letters > 0 && (double) outside / letters > 0.1;
    }

    private static Set<String> digitsIn(String text) {
        Set<String> found = new HashSet<>();
        Matcher m = DIGITS.matcher(text == null ? "" : text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static String tidy(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    private static List<String> tidyAll(List<String> items) {
        return items == null ? List.of() : items.stream().map(ExplanationValidator::tidy).filter(s -> !s.isEmpty()).toList();
    }
}
