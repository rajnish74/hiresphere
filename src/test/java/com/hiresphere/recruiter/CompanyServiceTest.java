package com.hiresphere.recruiter;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.recruiter.dto.CompanyRequest;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.event.CompanyStatusChangedEvent;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserService userService;

    private CompanyService service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companyRepository, userService, eventPublisher);
    }

    private Company approvedCompany() {
        Company c = new Company();
        c.setOwnerId(1L);
        c.setName("Acme Technologies");
        c.setWebsite("https://acme.example.com");
        c.approve(99L);
        return c;
    }

    private CompanyRequest request(String name, String website, String description) {
        return new CompanyRequest(name, website, "Software", description, "Bengaluru");
    }

    @Test
    void changingNameOfApprovedCompanyShouldMoveItBackToPending() {
        Company company = approvedCompany();
        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = service.update(1L,
                request("Google", "https://acme.example.com", "desc"));

        assertThat(response.status()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void changingWebsiteOfApprovedCompanyShouldMoveItBackToPending() {
        Company company = approvedCompany();
        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = service.update(1L,
                request("Acme Technologies", "https://other.example.com", "desc"));

        assertThat(response.status()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void changingOnlyDescriptionShouldKeepCompanyApproved() {
        Company company = approvedCompany();
        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = service.update(1L,
                request("Acme Technologies", "https://acme.example.com", "A brand new description"));

        assertThat(response.status()).isEqualTo(CompanyStatus.APPROVED);
        assertThat(response.description()).isEqualTo("A brand new description");
    }

    @Test
    void extraSpacesInNameShouldNotCountAsIdentityChange() {
        Company company = approvedCompany();
        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = service.update(1L,
                request("  Acme Technologies  ", "https://acme.example.com", "desc"));

        assertThat(response.status()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void editingRejectedCompanyShouldResubmitItForReview() {
        Company company = new Company();
        company.setOwnerId(1L);
        company.setName("Acme Technologies");
        company.reject(99L, "Website does not match");
        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        CompanyResponse response = service.update(1L,
                request("Acme Technologies", "https://acme.example.com", "desc"));

        assertThat(response.status()).isEqualTo(CompanyStatus.PENDING);
        assertThat(response.rejectionReason()).isNull();
    }

    private Company pendingCompany() {
        Company c = new Company();
        c.setId(5L);
        c.setOwnerId(2L);
        c.setName("Acme");
        return c;
    }

    @Test
    void approvingShouldPublishEventForTheOwner() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(pendingCompany()));

        service.approve(5L, 99L);

        ArgumentCaptor<CompanyStatusChangedEvent> event = ArgumentCaptor.forClass(CompanyStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().ownerId()).isEqualTo(2L);
        assertThat(event.getValue().status()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void rejectingShouldCarryTheReason() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(pendingCompany()));

        service.reject(5L, 99L, "  Website mismatch ");

        ArgumentCaptor<CompanyStatusChangedEvent> event = ArgumentCaptor.forClass(CompanyStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().status()).isEqualTo(CompanyStatus.REJECTED);
        assertThat(event.getValue().reason()).isEqualTo("Website mismatch");
    }

    @Test
    void invalidTransitionShouldPublishNothing() {
        Company approved = pendingCompany();
        approved.approve(1L);
        when(companyRepository.findById(5L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approve(5L, 99L)).isInstanceOf(AppException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}