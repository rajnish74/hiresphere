package com.hiresphere.job.repository;

import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JobFacetQueries {

    private final EntityManager em;

    public Map<JobType, Long> countByJobType(Instant now) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Job> root = query.from(Job.class);

        query.multiselect(root.get("jobType").alias("type"), cb.count(root).alias("total"))
                .where(JobSpecifications.visiblePredicate(root, query, cb, now))
                .groupBy(root.get("jobType"));

        Map<JobType, Long> result = new EnumMap<>(JobType.class);
        for (Tuple row : em.createQuery(query).getResultList()) {
            result.put(row.get("type", JobType.class), row.get("total", Long.class));
        }
        return result;
    }
}