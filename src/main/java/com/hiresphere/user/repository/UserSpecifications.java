package com.hiresphere.user.repository;

import com.hiresphere.common.util.SearchUtil;
import com.hiresphere.user.entity.Role;
import com.hiresphere.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> search(String q, Role role, Boolean enabled) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String pattern = SearchUtil.likePattern(q);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("fullName")), pattern, '\\')));
        }
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        return spec;
    }
}