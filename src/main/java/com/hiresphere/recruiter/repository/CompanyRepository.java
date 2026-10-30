package com.hiresphere.recruiter.repository;

import com.hiresphere.recruiter.entity.Company;
import com.hiresphere.recruiter.entity.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByOwnerId(Long ownerId);

    Page<Company> findByVerificationStatus(CompanyStatus status, Pageable pageable);

    @Query("select c.verificationStatus, count(c) from Company c group by c.verificationStatus")
    List<Object[]> countGroupedByStatus();
}