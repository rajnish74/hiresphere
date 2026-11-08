package com.hiresphere.ai.skills;

import java.util.regex.Pattern;

final class SkillText {

    static final int MAX_LENGTH = 40;

    private static final Pattern SHAPE = Pattern.compile("^\\.?[\\p{L}\\p{N}][\\p{L}\\p{N} +#./&()-]*$");

    private SkillText() {
    }

    static boolean isPlausible(String skill) {
        return skill != null && skill.length() <= MAX_LENGTH && SHAPE.matcher(skill).matches();
    }

    static boolean appearsIn(String skill, String text, boolean ignoreCase) {
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        Pattern grounded = Pattern.compile(
                "(?<![\\p{L}\\p{N}])" + Pattern.quote(skill) + "(?![\\p{L}\\p{N}])", flags);
        return grounded.matcher(text).find();
    }
}