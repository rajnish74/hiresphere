CREATE TABLE profile_views (
                               id                BIGSERIAL PRIMARY KEY,
                               company_id        BIGINT NOT NULL REFERENCES companies(id),
                               candidate_user_id BIGINT NOT NULL REFERENCES users(id),
                               view_date         DATE   NOT NULL,
                               CONSTRAINT uk_profile_views UNIQUE (company_id, candidate_user_id, view_date)
);

CREATE INDEX idx_profile_views_candidate ON profile_views (candidate_user_id, view_date DESC);