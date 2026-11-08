package com.hiresphere.ai.skills;

import com.hiresphere.ai.AiResponse;

import java.util.List;

public record ExtractedSkills(List<String> skills, int droppedCount, boolean inputTruncated, AiResponse ai) {
}