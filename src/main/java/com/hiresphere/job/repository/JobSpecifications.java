package com.hiresphere.job.repository;

import com.hiresphere.job.dto.JobSearchCriteria;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobStatus;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.skill.entity.Skill;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> visibleToPublic(Instant now) {
        return (root, query, cb) -> visiblePredicate(root, query, cb, now);
    }

    public static Specification<Job> hasIdIn(Collection<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }


    public static Predicate visiblePredicate(From<?, Job> root, AbstractQuery<?> query,
                                             CriteriaBuilder cb, Instant now) {
        Subquery<Long> approvedCompany = query.subquery(Long.class);
        Root<Company> company = approvedCompany.from(Company.class);
        approvedCompany.select(company.<Long>get("id")).where(
                cb.equal(company.get("id"), root.get("companyId")),
                cb.equal(company.get("verificationStatus"), CompanyStatus.APPROVED));

        return cb.and(
                cb.equal(root.get("status"), JobStatus.PUBLISHED),
                cb.isFalse(root.<Boolean>get("hidden")),
                cb.or(cb.isNull(root.get("expiresAt")),
                        cb.greaterThan(root.<Instant>get("expiresAt"), now)),
                cb.exists(approvedCompany));
    }

    public static Specification<Job> hasId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    public static Specification<Job> postedWithin(int days, Instant now) {
        Instant cutoff = now.minus(days, ChronoUnit.DAYS);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("publishedAt"), cutoff);
    }

    public static Specification<Job> search(JobSearchCriteria c, Instant now) {
        Specification<Job> spec = visibleToPublic(now);

        if (hasText(c.keyword())) {
            spec = spec.and(keyword(c.keyword()));
        }
        if (hasText(c.location())) {
            spec = spec.and(containsIgnoreCase("location", c.location()));
        }
        if (c.jobTypes() != null && !c.jobTypes().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("jobType").in(c.jobTypes()));
        }
        if (c.workModes() != null && !c.workModes().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("workMode").in(c.workModes()));
        }
        if (c.experience() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.<Integer>get("minExperienceYears"), c.experience()));
        }
        if (c.minSalary() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<BigDecimal>get("salaryMax"), c.minSalary()));
        }
        if (c.skills() != null && !c.skills().isEmpty()) {
            spec = spec.and(anySkill(c.skills()));
        }
        if (c.postedWithinDays() != null) {
            spec = spec.and(postedWithin(c.postedWithinDays(), now));
        }
        return spec;
    }

    private static Specification<Job> keyword(String keyword) {
        String pattern = likePattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("description")), pattern, '\\'));
    }

    private static Specification<Job> containsIgnoreCase(String field, String value) {
        String pattern = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get(field)), pattern, '\\');
    }

    private static Specification<Job> anySkill(List<String> skills) {
        List<String> normalized = skills.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Job> subJob = sub.from(Job.class);
            Join<Job, Skill> skill = subJob.join("skills");
            sub.select(subJob.<Long>get("id")).where(
                    cb.equal(subJob.get("id"), root.get("id")),
                    skill.get("normalizedName").in(normalized));
            return cb.exists(sub);
        };
    }

    private static String likePattern(String input) {
        String escaped = input.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}