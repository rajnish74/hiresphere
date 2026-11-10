package com.hiresphere.ai.resume;

import com.hiresphere.ai.resume.ResumeTextException.Kind;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeTextExceptionTest {

    @Test
    void everyKindShouldHaveAFriendlyMessageThatNeverContainsTheInternalDetail() {
        for (Kind kind : Kind.values()) {
            ResumeTextException e = new ResumeTextException(kind, "internal detail 123");

            assertThat(e.getMessage()).as(kind.name()).isNotBlank().doesNotContain("internal detail");
            assertThat(e.getDetail()).isEqualTo("internal detail 123");
            assertThat(e.getKind()).isEqualTo(kind);
        }
    }

    @Test
    void noResumeIsAConflictAndEverythingAboutTheFileIsUnprocessable() {
        assertThat(new ResumeTextException(Kind.NO_RESUME, "").getStatus()).isEqualTo(HttpStatus.CONFLICT);
        for (Kind kind : new Kind[]{Kind.ENCRYPTED, Kind.UNREADABLE, Kind.NO_TEXT, Kind.TOO_LARGE}) {
            assertThat(new ResumeTextException(kind, "").getStatus()).as(kind.name())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}