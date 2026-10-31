package com.hiresphere.config;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiGroupsConfigTest {

    private final OpenApiGroupsConfig config = new OpenApiGroupsConfig();

    @Test
    void shouldCreateFourGroupsInDisplayOrder() {
        assertThat(config.publicApi().getGroup()).isEqualTo("1-public");
        assertThat(config.candidateApi().getGroup()).isEqualTo("2-candidate");
        assertThat(config.recruiterApi().getGroup()).isEqualTo("3-recruiter");
        assertThat(config.adminApi().getGroup()).isEqualTo("4-admin");
    }

    @Test
    void adminGroupShouldOnlyCoverAdminPaths() {
        GroupedOpenApi admin = config.adminApi();

        assertThat(admin.getPathsToMatch()).containsExactly("/api/v1/admin/**");
    }

    @Test
    void publicGroupShouldNotContainAnyAdminOrRecruiterPath() {
        assertThat(config.publicApi().getPathsToMatch())
                .noneMatch(p -> p.contains("/admin/") || p.contains("/recruiter/"));
    }
}