package com.hiresphere.job.dto;

import com.hiresphere.job.entity.Job;

public record JobRef(Long id,
                     String title,
                     Long companyId,
                     int openings) {

    public JobRef(Long id, String title, Long companyId) {
        this(id, title, companyId, 1);
    }

    public static JobRef from(Job j) {
        return new JobRef(j.getId(), j.getTitle(), j.getCompanyId(), j.getOpenings());
    }
}