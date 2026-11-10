package com.hiresphere.ai.resume;

import com.hiresphere.ai.resume.ResumeTextException.Kind;
import com.hiresphere.candidate.dto.ResumeRef;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StorageResumeSource implements ResumeSource {

    private final CandidateProfileService profiles;
    private final FileStorageService storage;

    @Override
    public Optional<ResumeFile> read(Long userId) {
        Optional<ResumeRef> ref = profiles.findResume(userId);
        if (ref.isEmpty()) {
            return Optional.empty();
        }

        Resource resource;
        try {
            resource = storage.load(ref.get().storageKey());
        } catch (RuntimeException e) {
            throw new ResumeTextException(Kind.NO_RESUME, "storage could not load the file: " + e.getClass().getSimpleName());
        }
        if (resource == null || !resource.exists()) {
            throw new ResumeTextException(Kind.NO_RESUME, "the resume file is missing from storage");
        }

        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readNBytes(PdfResumeTextExtractor.MAX_BYTES + 1);
            return Optional.of(new ResumeFile(ref.get().originalName(), bytes));
        } catch (IOException e) {
            throw new ResumeTextException(Kind.UNREADABLE, "could not read the file: " + e.getClass().getSimpleName());
        }
    }
}