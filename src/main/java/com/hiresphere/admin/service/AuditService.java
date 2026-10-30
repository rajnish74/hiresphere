package com.hiresphere.admin.service;

import com.hiresphere.admin.dto.AuditLogResponse;
import com.hiresphere.admin.entity.AuditAction;
import com.hiresphere.admin.entity.AuditLog;
import com.hiresphere.admin.entity.AuditTargetType;
import com.hiresphere.admin.repository.AuditLogRepository;
import com.hiresphere.common.dto.PageResponse;
import com.hiresphere.common.util.PageUtil;
import com.hiresphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final UserService userService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, AuditAction action, AuditTargetType targetType,
                       Long targetId, String details) {
        repository.save(AuditLog.of(actorId, action, targetType, targetId, details));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuditAction action, AuditTargetType targetType, Long targetId,
                                               Long actorId, int page, int size) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (targetType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }
        if (targetId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetId"), targetId));
        }
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), actorId));
        }
        Page<AuditLog> result = repository.findAll(spec, PageUtil.newestFirst(page, size));

        Set<Long> actorIds = result.getContent().stream().map(AuditLog::getActorId).collect(Collectors.toSet());
        Map<Long, String> emails = userService.getEmails(actorIds);   // ek hi query
        return PageResponse.of(result, a -> AuditLogResponse.from(a, emails.get(a.getActorId())));
    }
}