package com.hiresphere.job.repository;

import com.hiresphere.job.dto.CompanyJobCount;
import com.hiresphere.job.dto.SkillCount;
import com.hiresphere.job.entity.Job;
import com.hiresphere.job.entity.JobType;
import com.hiresphere.skill.entity.Skill;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
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

    public <E extends Enum<E>> Map<E, Long> countBy(Specification<Job> spec, String field, Class<E> type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Job> root = query.from(Job.class);

        Predicate where = spec.toPredicate(root, query, cb);
        query.multiselect(root.get(field).alias("value"), cb.count(root).alias("total"));
        if (where != null) {
            query.where(where);
        }
        query.groupBy(root.get(field));

        Map<E, Long> result = new EnumMap<>(type);
        for (E constant : type.getEnumConstants()) {
            result.put(constant, 0L);
        }
        for (Tuple row : em.createQuery(query).getResultList()) {
            result.put(row.get("value", type), row.get("total", Long.class));
        }
        return result;
    }

    public List<CompanyJobCount> topCompanies(Instant now, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Job> root = query.from(Job.class);
        Expression<Long> total = cb.count(root);

        query.multiselect(root.get("companyId").alias("companyId"), total.alias("total"))
                .where(JobSpecifications.visiblePredicate(root, query, cb, now))
                .groupBy(root.get("companyId"))
                .orderBy(cb.desc(total), cb.asc(root.get("companyId")));

        return em.createQuery(query).setMaxResults(limit).getResultList().stream()
                .map(t -> new CompanyJobCount(t.get("companyId", Long.class), t.get("total", Long.class)))
                .toList();
    }

    public List<SkillCount> popularSkills(Instant now, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Job> root = query.from(Job.class);
        Join<Job, Skill> skill = root.join("skills");
        Expression<Long> total = cb.countDistinct(root);

        query.multiselect(skill.get("name").alias("name"), total.alias("total"))
                .where(JobSpecifications.visiblePredicate(root, query, cb, now))
                .groupBy(skill.get("normalizedName"), skill.get("name"))
                .orderBy(cb.desc(total), cb.asc(skill.get("name")));

        return em.createQuery(query).setMaxResults(limit).getResultList().stream()
                .map(t -> new SkillCount(t.get("name", String.class), t.get("total", Long.class)))
                .toList();
    }
}