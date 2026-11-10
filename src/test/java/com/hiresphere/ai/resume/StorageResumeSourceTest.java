package com.hiresphere.ai.resume;

import com.hiresphere.ai.resume.ResumeTextException.Kind;
import com.hiresphere.candidate.dto.ResumeRef;
import com.hiresphere.candidate.service.CandidateProfileService;
import com.hiresphere.common.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageResumeSourceTest {

    @Mock
    private CandidateProfileService profiles;
    @Mock
    private FileStorageService storage;

    private StorageResumeSource source;

    @BeforeEach
    void setUp() {
        source = new StorageResumeSource(profiles, storage);
    }

    private void profileHasResume() {
        when(profiles.findResume(7L)).thenReturn(Optional.of(new ResumeRef("key-1", "asha-cv.pdf")));
    }

    private void assertFails(Kind kind) {
        assertThatThrownBy(() -> source.read(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(kind));
    }

    @Test
    void aProfileWithoutAResumeShouldGiveNothingAndNeverTouchStorage() {
        when(profiles.findResume(7L)).thenReturn(Optional.empty());

        assertThat(source.read(7L)).isEmpty();

        verifyNoInteractions(storage);
    }

    @Test
    void shouldReadTheBytesAndKeepTheOriginalName() {
        profileHasResume();
        when(storage.load("key-1")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        ResumeFile file = source.read(7L).orElseThrow();

        assertThat(file.name()).isEqualTo("asha-cv.pdf");
        assertThat(file.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void aFileThatIsMissingFromStorageShouldAskTheUserToUploadAgain() {
        profileHasResume();
        when(storage.load("key-1")).thenReturn(new FileSystemResource("/definitely/not/here/resume.pdf"));

        assertFails(Kind.NO_RESUME);
    }

    @Test
    void storageThatCannotLoadOrReturnsNothingShouldAlsoAskTheUserToUploadAgain() {
        profileHasResume();
        when(storage.load("key-1")).thenThrow(new IllegalStateException("bucket unavailable"));
        assertFails(Kind.NO_RESUME);
    }

    @Test
    void aNullResourceShouldAskTheUserToUploadAgain() {
        profileHasResume();
        when(storage.load("key-1")).thenReturn(null);

        assertFails(Kind.NO_RESUME);
    }

    @Test
    void aReadFailureShouldBeUnreadableAndNotLeakTheInternalMessage() {
        profileHasResume();
        when(storage.load("key-1")).thenReturn(new ByteArrayResource(new byte[1]) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("disk error at /var/data/secret-path");
            }
        });

        assertThatThrownBy(() -> source.read(7L))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> {
                    assertThat(e.getKind()).isEqualTo(Kind.UNREADABLE);
                    assertThat(e.getMessage()).doesNotContain("secret-path");
                });
    }

    @Test
    void aHugeFileShouldBeReadOnlyOneByteBeyondTheLimit() {
        profileHasResume();
        when(storage.load("key-1")).thenReturn(new ByteArrayResource(new byte[PdfResumeTextExtractor.MAX_BYTES + 5_000]));

        ResumeFile file = source.read(7L).orElseThrow();

        assertThat(file.bytes()).hasSize(PdfResumeTextExtractor.MAX_BYTES + 1);
    }
}