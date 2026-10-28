package com.hiresphere.recruiter;

import com.hiresphere.recruiter.dto.CompanyRequest;
import com.hiresphere.recruiter.dto.CompanyResponse;
import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import com.hiresphere.recruiter.repository.CompanyRepository;
import com.hiresphere.recruiter.service.CompanyService;
import com.hiresphere.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserService userService;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companyRepository, userService);
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
}