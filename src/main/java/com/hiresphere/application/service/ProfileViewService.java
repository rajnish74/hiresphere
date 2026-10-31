package com.hiresphere.application.service;

import com.hiresphere.application.dto.ProfileViewSummary;
import com.hiresphere.application.dto.ProfileViewerResponse;
import com.hiresphere.application.dto.ProfileViewerRow;
import com.hiresphere.application.repository.ProfileViewRepository;
import com.hiresphere.recruiter.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileViewService {

    static final int WINDOW_DAYS = 30;

    private final ProfileViewRepository repository;
    private final CompanyService companyService;

    @Transactional
    public void record(Long companyId, Long candidateUserId) {
        repository.insertIfAbsent(companyId, candidateUserId, LocalDate.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public ProfileViewSummary summary(Long candidateUserId) {
        LocalDate since = LocalDate.now(ZoneOffset.UTC).minusDays(WINDOW_DAYS);
        List<ProfileViewerRow> rows = repository.findViewers(candidateUserId, since);

        Set<Long> companyIds = rows.stream().map(ProfileViewerRow::companyId).collect(Collectors.toSet());
        Map<Long, String> names = companyService.getNames(companyIds);   // ek hi query

        List<ProfileViewerResponse> viewers = rows.stream()
                .map(r -> new ProfileViewerResponse(r.companyId(),
                        names.getOrDefault(r.companyId(), "A company"), r.lastViewedOn()))
                .toList();
        return new ProfileViewSummary(WINDOW_DAYS, viewers.size(), viewers);
    }
}