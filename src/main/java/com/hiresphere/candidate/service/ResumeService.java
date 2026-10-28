package com.hiresphere.candidate.service;

import com.hiresphere.candidate.dto.ResumeInfo;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.common.exception.AppException;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.exception.ResourceNotFoundException;
import com.hiresphere.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final CandidateProfileService profileService;
    private final CandidateProfileRepository profileRepository;
    private final FileStorageService storage;

    public record ResumeDownload(Resource resource, String filename) {
    }

    @Transactional
    public ResumeInfo upload(Long userId, MultipartFile file) {
        validate(file);
        CandidateProfile profile = profileService.getOrCreate(userId);
        String oldKey = profile.getResumeStorageKey();

        String newKey;
        try (InputStream in = file.getInputStream()) {
            newKey = storage.store(in, "pdf");
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the resume");
        }

        profile.setResumeStorageKey(newKey);
        profile.setResumeOriginalName(cleanFilename(file.getOriginalFilename()));
        profile.setResumeSizeBytes(file.getSize());
        profile.setResumeUploadedAt(Instant.now());

        deleteFileAfterCommit(oldKey);
        return ResumeInfo.from(profile);
    }

    @Transactional(readOnly = true)
    public ResumeDownload download(Long userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .filter(p -> p.getResumeStorageKey() != null)
                .orElseThrow(() -> new ResourceNotFoundException("No resume uploaded"));
        return new ResumeDownload(storage.load(profile.getResumeStorageKey()), profile.getResumeOriginalName());
    }

    @Transactional
    public void delete(Long userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            String key = profile.getResumeStorageKey();
            if (key == null) {
                return;
            }
            profile.setResumeStorageKey(null);
            profile.setResumeOriginalName(null);
            profile.setResumeSizeBytes(null);
            profile.setResumeUploadedAt(null);
            deleteFileAfterCommit(key);
        });
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Resume must be 5 MB or smaller");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are allowed");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_MAGIC.length);
            if (!Arrays.equals(header, PDF_MAGIC)) {
                throw new BadRequestException("File is not a valid PDF");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
    }

    private String cleanFilename(String original) {
        String clean = StringUtils.cleanPath(original.replace('\\', '/'));
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
        if (clean.length() > 200) {
            clean = clean.substring(clean.length() - 200);
        }
        return clean;
    }

    private void deleteFileAfterCommit(String key) {
        if (key == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storage.deleteQuietly(key);
                }
            });
        } else {
            storage.deleteQuietly(key);
        }
    }
}