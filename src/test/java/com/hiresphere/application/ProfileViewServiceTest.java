package com.hiresphere.application;

import com.hiresphere.application.dto.ProfileViewSummary;
import com.hiresphere.application.dto.ProfileViewerRow;
import com.hiresphere.application.repository.ProfileViewRepository;
import com.hiresphere.application.service.ProfileViewService;
import com.hiresphere.recruiter.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileViewServiceTest {

    @Mock
    private ProfileViewRepository repository;
    @Mock
    private CompanyService companyService;

    private ProfileViewService service;

    @BeforeEach
    void setUp() {
        service = new ProfileViewService(repository, companyService);
    }

    @Test
    void recordShouldInsertOneRowForTodayInUtc() {
        service.record(5L, 9L);

        ArgumentCaptor<LocalDate> date = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).insertIfAbsent(eq(5L), eq(9L), date.capture());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(date.getValue()).isBetween(today.minusDays(1), today.plusDays(1));
    }

    @Test
    void summaryShouldCountDistinctCompaniesAndFallBackWhenACompanyIsGone() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(repository.findViewers(eq(9L), any(LocalDate.class))).thenReturn(List.of(
                new ProfileViewerRow(5L, today), new ProfileViewerRow(6L, today.minusDays(3))));
        when(companyService.getNames(Set.of(5L, 6L))).thenReturn(Map.of(5L, "Acme"));   // 6 ab nahi hai

        ProfileViewSummary summary = service.summary(9L);

        assertThat(summary.days()).isEqualTo(30);
        assertThat(summary.companiesCount()).isEqualTo(2);
        assertThat(summary.viewers().get(0).companyName()).isEqualTo("Acme");
        assertThat(summary.viewers().get(1).companyName()).isEqualTo("A company");
    }

    @Test
    void summaryWithNoViewersShouldBeEmpty() {
        when(repository.findViewers(eq(9L), any(LocalDate.class))).thenReturn(List.of());
        when(companyService.getNames(Set.of())).thenReturn(Map.of());

        assertThat(service.summary(9L).companiesCount()).isZero();
    }
}