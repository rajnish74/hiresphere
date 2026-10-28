package com.hiresphere.candidate.service;

import com.hiresphere.candidate.dto.*;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.entity.Education;
import com.hiresphere.candidate.entity.Experience;
import com.hiresphere.candidate.repository.EducationRepository;
import com.hiresphere.candidate.repository.ExperienceRepository;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileSectionService {

    private final CandidateProfileService profileService;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;

    // ---------- Education ----------

    @Transactional
    public EducationResponse addEducation(Long userId, EducationRequest req) {
        checkYears(req);
        Education education = new Education();
        education.setCandidate(profileService.getOrCreate(userId));
        applyEducation(education, req);
        return EducationResponse.from(educationRepository.save(education));
    }

    @Transactional
    public EducationResponse updateEducation(Long userId, Long id, EducationRequest req) {
        checkYears(req);
        Education education = findOwnedEducation(userId, id);
        applyEducation(education, req);
        return EducationResponse.from(education);
    }

    @Transactional
    public void deleteEducation(Long userId, Long id) {
        educationRepository.delete(findOwnedEducation(userId, id));
    }

    // ---------- Experience ----------

    @Transactional
    public ExperienceResponse addExperience(Long userId, ExperienceRequest req) {
        checkDates(req);
        Experience experience = new Experience();
        experience.setCandidate(profileService.getOrCreate(userId));
        applyExperience(experience, req);
        return ExperienceResponse.from(experienceRepository.save(experience));
    }

    @Transactional
    public ExperienceResponse updateExperience(Long userId, Long id, ExperienceRequest req) {
        checkDates(req);
        Experience experience = findOwnedExperience(userId, id);
        applyExperience(experience, req);
        return ExperienceResponse.from(experience);
    }

    @Transactional
    public void deleteExperience(Long userId, Long id) {
        experienceRepository.delete(findOwnedExperience(userId, id));
    }

    // ---------- helpers ----------

    private Education findOwnedEducation(Long userId, Long id) {
        CandidateProfile profile = profileService.getOrCreate(userId);
        return educationRepository.findByIdAndCandidateId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education not found"));
    }

    private Experience findOwnedExperience(Long userId, Long id) {
        CandidateProfile profile = profileService.getOrCreate(userId);
        return experienceRepository.findByIdAndCandidateId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
    }

    private void checkYears(EducationRequest r) {
        if (r.endYear() != null && r.endYear() < r.startYear()) {
            throw new BadRequestException("End year cannot be before start year");
        }
    }

    private void checkDates(ExperienceRequest r) {
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private void applyEducation(Education e, EducationRequest r) {
        e.setInstitution(r.institution().trim());
        e.setDegree(r.degree().trim());
        e.setFieldOfStudy(TextUtil.blankToNull(r.fieldOfStudy()));
        e.setStartYear(r.startYear());
        e.setEndYear(r.endYear());
        e.setGrade(TextUtil.blankToNull(r.grade()));
    }

    private void applyExperience(Experience e, ExperienceRequest r) {
        e.setCompany(r.company().trim());
        e.setTitle(r.title().trim());
        e.setLocation(TextUtil.blankToNull(r.location()));
        e.setStartDate(r.startDate());
        e.setEndDate(r.endDate());
        e.setDescription(TextUtil.blankToNull(r.description()));
    }
}