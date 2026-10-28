package com.hiresphere.common.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface FileStorageService {


    String store(InputStream content, String fileExtension);

    Resource load(String storageKey);

    void deleteQuietly(String storageKey);
}