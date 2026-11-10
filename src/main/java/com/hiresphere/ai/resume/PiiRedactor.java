package com.hiresphere.ai.resume;

import com.hiresphere.ai.prompt.UntrustedText;

import java.util.List;
import java.util.regex.Pattern;


public final class PiiRedactor {

    public static final String EMAIL_TOKEN = "[email]";
    public static final String PHONE_TOKEN = "[phone]";
    public static final String LINK_TOKEN = "[link]";

    private static final Pattern SCHEME_LINK = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\"]*[^\\s<>\".,;:!?)\\]']");

    private static final Pattern PROFILE_LINK = Pattern.compile(
            "(?i)\\b(?:[a-z0-9-]{1,63}\\.)?(?:linkedin|github|gitlab|bitbucket|twitter|x|facebook|instagram|medium"
                    + "|behance|dribbble|leetcode|codechef|codeforces|hackerrank|kaggle|stackoverflow)"
                    + "\\.com/[^\\s<>\"]*[^\\s<>\".,;:!?)\\]']");

    private static final Pattern EMAIL = Pattern.compile(
            "(?i)[a-z0-9._%+-]{1,64}@[a-z0-9-]{1,63}(?:\\.[a-z0-9-]{1,63}){1,10}");

    private static final List<Pattern> PHONES = List.of(
            Pattern.compile("\\+\\d{1,3}[\\s.-]?\\d{10}(?!\\d)"),                                  // +919876543210
            Pattern.compile("\\+\\d{1,3}[\\s.-]?\\d{5}[\\s.-]\\d{5}(?!\\d)"),                      // +91 98765 43210
            Pattern.compile("\\+\\d{1,3}[\\s.-]?\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}(?!\\d)"),  // +1 (555) 123-4567
            Pattern.compile("\\+\\d{1,3}[\\s.-]?\\d{2,4}[\\s.-]\\d{3,4}[\\s.-]\\d{3,4}(?!\\d)"),    // +44 20 7946 0958
            Pattern.compile("(?<!\\d)[6-9]\\d{4}[\\s-]\\d{5}(?!\\d)"),                              // 98765 43210
            Pattern.compile("(?<!\\d)\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}(?!\\d)"),             // (555) 123-4567
            Pattern.compile("(?<!\\d)\\d{10,}(?!\\d)"));                                           // 9876543210, 12-digit IDs

    public record Result(String text, int emails, int phones, int links) {
        public int total() {
            return emails + phones + links;
        }
    }

    private PiiRedactor() {
    }

    public static Result redact(String raw) {
        String text = UntrustedText.clean(raw, Integer.MAX_VALUE).text();

        int[] links = {0};
        text = replace(SCHEME_LINK, text, LINK_TOKEN, links);
        text = replace(PROFILE_LINK, text, LINK_TOKEN, links);

        int[] emails = {0};
        text = replace(EMAIL, text, EMAIL_TOKEN, emails);

        int[] phones = {0};
        for (Pattern pattern : PHONES) {
            text = replace(pattern, text, PHONE_TOKEN, phones);
        }
        return new Result(text, emails[0], phones[0], links[0]);
    }

    private static String replace(Pattern pattern, String text, String token, int[] counter) {
        return pattern.matcher(text).replaceAll(match -> {
            counter[0]++;
            return token;
        });
    }
}