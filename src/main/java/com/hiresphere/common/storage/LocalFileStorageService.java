package com.hiresphere.common.storage;

import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(StorageProperties props) {
        this.root = Path.of(props.baseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + root, e);
        }
    }

    @Override
    public String store(InputStream content, String fileExtension) {
        if (!fileExtension.matches("[a-z0-9]{1,8}")) {
            throw new IllegalArgumentException("Invalid file extension");
        }
        String key = UUID.randomUUID() + "." + fileExtension;
        try {
            Files.copy(content, resolve(key));
        } catch (IOException e) {
            log.error("Failed to store file {}", key, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file");
        }
        return key;
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("File not found");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (Exception e) {
            log.warn("Could not delete file {}", storageKey, e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}