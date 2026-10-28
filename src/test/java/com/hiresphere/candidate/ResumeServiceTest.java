package com.hiresphere.candidate;

import com.hiresphere.candidate.dto.ResumeInfo;
import com.hiresphere.candidate.entity.CandidateProfile;
import com.hiresphere.candidate.repository.CandidateProfileRepository;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.candidate.service.ResumeService;
import com.hiresphere.common.exception.BadRequestException;
import com.hiresphere.common.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private CandidateProfileService profileService;
    @Mock
    private CandidateProfileRepository profileRepository;
    @Mock
    private FileStorageService storage;

    private ResumeService service;

    @BeforeEach
    void setUp() {
        service = new ResumeService(profileService, profileRepository, storage);
    }

    @Test
    void shouldRejectNonPdfExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/pdf",
                "%PDF-1.7 content".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.upload(1L, file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectFakePdfWithWrongContent() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "this is just plain text".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.upload(1L, file)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldAcceptValidPdfAndStoreIt() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "%PDF-1.7 fake but valid header".getBytes(StandardCharsets.US_ASCII));
        when(profileService.getOrCreate(1L)).thenReturn(new CandidateProfile());
        when(storage.store(any(InputStream.class), eq("pdf"))).thenReturn("abc-123.pdf");

        ResumeInfo info = service.upload(1L, file);

        assertThat(info.originalName()).isEqualTo("cv.pdf");
        verify(storage).store(any(InputStream.class), eq("pdf"));
    }
}