package com.hiresphere.ai.prompt;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.HexFormat;

public final class UntrustedText {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UntrustedText() {
    }

    public record Cleaned(String text, boolean truncated, int originalLength) {
    }

    public record Wrapped(String block, String boundary, String text, boolean truncated) {
    }

    public static Cleaned clean(String raw, int maxChars) {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        if (raw == null) {
            return new Cleaned("", false, 0);
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .replace("\r\n", "\n").replace('\r', '\n');

        StringBuilder kept = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(cp -> {
            if (cp == '\n') {
                kept.append('\n');
            } else if (cp == '\t') {
                kept.append(' ');
            } else if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) {
            } else {
                kept.appendCodePoint(cp);
            }
        });

        String text = kept.toString()
                .replaceAll(" {2,}", " ")
                .replaceAll(" ?\n ?", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();

        boolean truncated = false;
        if (text.codePointCount(0, text.length()) > maxChars) {
            text = text.substring(0, text.offsetByCodePoints(0, maxChars)).stripTrailing();
            truncated = true;
        }
        return new Cleaned(text, truncated, raw.length());
    }

    public static Wrapped wrap(String label, String raw, int maxChars) {
        return wrap(label, raw, maxChars, randomBoundary());
    }

    public static Wrapped wrap(String label, String raw, int maxChars, String boundary) {
        if (label == null || !label.matches("[A-Z_]{2,20}")) {
            throw new IllegalArgumentException("label must be upper case letters");
        }
        Cleaned cleaned = clean(raw, maxChars);
        String body = cleaned.text().replaceAll("<{3,}", "<<").replaceAll(">{3,}", ">>");
        String block = "<<<BEGIN " + label + " " + boundary + ">>>\n" + body + "\n<<<END " + label + " " + boundary + ">>>";
        return new Wrapped(block, boundary, cleaned.text(), cleaned.truncated());
    }

    static String randomBoundary() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);   // 16 hex characters
    }
}