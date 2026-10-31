package com.hiresphere.common;

import com.hiresphere.common.storage.FileStorageService;
import com.hiresphere.common.storage.LocalFileStorageService;
import com.hiresphere.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageSwitchTest {

    @TempDir
    Path tempDir;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withBean(StorageProperties.class, () -> new StorageProperties(tempDir.toString()))
                .withUserConfiguration(LocalFileStorageService.class);
    }

    @Test
    void localStorageShouldBeTheDefault() {
        runner().run(context -> assertThat(context).hasSingleBean(FileStorageService.class));
    }

    @Test
    void localStorageShouldBeExplicitlySelectable() {
        runner().withPropertyValues("app.storage.type=local")
                .run(context -> assertThat(context).hasSingleBean(FileStorageService.class));
    }

    @Test
    void localStorageShouldStepAsideWhenAnotherTypeIsConfigured() {
        runner().withPropertyValues("app.storage.type=s3")
                .run(context -> assertThat(context).doesNotHaveBean(FileStorageService.class));
    }
}