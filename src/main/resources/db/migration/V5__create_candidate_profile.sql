CREATE TABLE skills (
                        id              BIGSERIAL PRIMARY KEY,
                        name            VARCHAR(60) NOT NULL,
                        normalized_name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE candidate_profiles (
                                    id                   BIGSERIAL PRIMARY KEY,
                                    user_id              BIGINT       NOT NULL REFERENCES users(id),
                                    headline             VARCHAR(150),
                                    summary              TEXT,
                                    location             VARCHAR(100),
                                    linkedin_url         VARCHAR(255),
                                    github_url           VARCHAR(255),
                                    portfolio_url        VARCHAR(255),
                                    visibility           VARCHAR(20)  NOT NULL DEFAULT 'RECRUITERS_ONLY',
                                    open_to_work         BOOLEAN      NOT NULL DEFAULT TRUE,
                                    resume_storage_key   VARCHAR(100),
                                    resume_original_name VARCHAR(255),
                                    resume_size_bytes    BIGINT,
                                    resume_uploaded_at   TIMESTAMPTZ,
                                    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    version              BIGINT       NOT NULL DEFAULT 0,
                                    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
                                    deleted_at           TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_candidate_profiles_user_active
    ON candidate_profiles(user_id) WHERE deleted = FALSE;

CREATE TABLE candidate_skills (
                                  candidate_id BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
                                  skill_id     BIGINT NOT NULL REFERENCES skills(id),
                                  PRIMARY KEY (candidate_id, skill_id)
);
CREATE INDEX idx_candidate_skills_skill ON candidate_skills(skill_id);

CREATE TABLE education (
                           id             BIGSERIAL PRIMARY KEY,
                           candidate_id   BIGINT       NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
                           institution    VARCHAR(150) NOT NULL,
                           degree         VARCHAR(100) NOT NULL,
                           field_of_study VARCHAR(100),
                           start_year     INT          NOT NULL,
                           end_year       INT,
                           grade          VARCHAR(30)
);
CREATE INDEX idx_education_candidate ON education(candidate_id);

CREATE TABLE experience (
                            id           BIGSERIAL PRIMARY KEY,
                            candidate_id BIGINT       NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
                            company      VARCHAR(150) NOT NULL,
                            title        VARCHAR(100) NOT NULL,
                            location     VARCHAR(100),
                            start_date   DATE         NOT NULL,
                            end_date     DATE,
                            description  TEXT
);
CREATE INDEX idx_experience_candidate ON experience(candidate_id);