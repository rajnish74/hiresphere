package com.hiresphere.ai.resume;

import java.util.Optional;

public interface ResumeSource {

    Optional<ResumeFile> read(Long userId);
}