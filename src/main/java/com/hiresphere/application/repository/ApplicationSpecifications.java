package com.hiresphere.application.repository;

import com.hiresphere.application.entity.ApplicationStatus;
import com.hiresphere.application.entity.JobApplication;
import com.hiresphere.common.util.SearchUtil;
import org.springframework.data.jpa.domain.Specification;

public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    public static Specification<JobApplication> mine(Long userId, ApplicationStatus status, String q) {
        Specification<JobApplication> spec = (root, query, cb) -> cb.equal(root.get("candidateUserId"), userId);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (q != null && !q.isBlank()) {
            String pattern = SearchUtil.likePattern(q);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("jobTitle")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("companyName")), pattern, '\\')));
        }
        return spec;
    }
}