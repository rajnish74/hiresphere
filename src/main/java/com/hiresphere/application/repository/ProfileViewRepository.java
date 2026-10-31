package com.hiresphere.application.repository;

import com.hiresphere.application.dto.ProfileViewerRow;
import com.hiresphere.application.entity.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO profile_views (company_id, candidate_user_id, view_date)
            VALUES (:companyId, :candidateId, :viewDate)
            ON CONFLICT (company_id, candidate_user_id, view_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("companyId") Long companyId, @Param("candidateId") Long candidateId,
                       @Param("viewDate") LocalDate viewDate);

    @Query("""
            select new com.hiresphere.application.dto.ProfileViewerRow(v.companyId, max(v.viewDate))
              from ProfileView v
             where v.candidateUserId = :candidateId and v.viewDate >= :since
             group by v.companyId
             order by max(v.viewDate) desc
            """)
    List<ProfileViewerRow> findViewers(@Param("candidateId") Long candidateId, @Param("since") LocalDate since);

    @Modifying
    @Query("delete from ProfileView v where v.viewDate < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDate cutoff);
}