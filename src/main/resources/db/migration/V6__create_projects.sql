CREATE TABLE projects (
                          id           BIGSERIAL PRIMARY KEY,
                          candidate_id BIGINT       NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
                          title        VARCHAR(120) NOT NULL,
                          description  TEXT,
                          tech_stack   VARCHAR(300),
                          project_url  VARCHAR(255),
                          repo_url     VARCHAR(255)
);

CREATE INDEX idx_projects_candidate ON projects(candidate_id);