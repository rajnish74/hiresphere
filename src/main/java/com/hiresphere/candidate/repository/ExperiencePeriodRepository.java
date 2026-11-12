package com.hiresphere.candidate.repository;

import com.hiresphere.candidate.entity.Experience;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExperiencePeriodRepository extends Repository<Experience, Long> {

    interface Dates {
        LocalDate getStartDate();

        LocalDate getEndDate();
    }

    @Query("select e.startDate as startDate, e.endDate as endDate from Experience e where e.candidate.userId = :userId")
    List<Dates> findDatesByUserId(@Param("userId") Long userId);
}