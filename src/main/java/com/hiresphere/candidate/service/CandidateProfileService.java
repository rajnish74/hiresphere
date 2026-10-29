package com.hiresphere.candidate.service;

import com.hiresphere.candidate.dto.ApplicantSnapshot;
import com.hiresphere.candidate.dto.ProfileResponse;
import com.hiresphere.candidate.dto.ProfileUpdateRequest;
import com.hiresphere.candidate.dto.SkillsRequest;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.common.util.TextUtil;
import com.hiresphere.skill.entity.Skill;
import com.hiresphere.skill.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final SkillService skillService;

    @Transactional
    public CandidateProfile getOrCreate(Long userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            CandidateProfile profile = new CandidateProfile();
            profile.setUserId(userId);
            return profileRepository.save(profile);
        });
    }


    @Transactional
    public ProfileResponse getMyProfile(Long userId) {
        return ProfileResponse.from(getOrCreate(userId));
    }

    @Transactional
    public ProfileResponse updateMyProfile(Long userId, ProfileUpdateRequest req) {
        CandidateProfile profile = getOrCreate(userId);
        profile.setHeadline(TextUtil.blankToNull(req.headline()));
        profile.setSummary(TextUtil.blankToNull(req.summary()));
        profile.setLocation(TextUtil.blankToNull(req.location()));
        profile.setLinkedinUrl(req.linkedinUrl());
        profile.setGithubUrl(req.githubUrl());
        profile.setPortfolioUrl(req.portfolioUrl());
        profile.setVisibility(req.visibility());
        profile.setOpenToWork(req.openToWork());
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse replaceSkills(Long userId, SkillsRequest req) {
        CandidateProfile profile = getOrCreate(userId);
        Set<Skill> resolved = skillService.resolveAll(req.skills());
        profile.getSkills().clear();
        profile.getSkills().addAll(resolved);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileCompleteness.Result getCompleteness(Long userId) {
        return ProfileCompleteness.calculate(getOrCreate(userId));
    }

    @Transactional
    public ApplicantSnapshot getSnapshot(Long userId) {
        CandidateProfile p = getOrCreate(userId);
        return new ApplicantSnapshot(p.getHeadline(), p.getResumeStorageKey(), p.getResumeOriginalName());
    }
}