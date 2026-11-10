package com.hiresphere.ai.resume;

import com.hiresphere.candidate.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileSkillsAdapter implements ProfileSkills {

    private final CandidateProfileService profiles;

    @Override
    public Set<String> keysOf(Long userId) {
        return profiles.findSkillKeys(userId);
    }
}