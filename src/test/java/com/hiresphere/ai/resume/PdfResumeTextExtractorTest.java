package com.hiresphere.ai.resume;

import com.hiresphere.ai.resume.ResumeTextException.Kind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfResumeTextExtractorTest {

    private static final String RESUME = """
            Asha Verma - Backend Developer, Patna
            Skills: Java, Spring Boot, PostgreSQL, Docker
            Experience: Built hiring APIs at Acme with Spring Boot.""";

    private final PdfResumeTextExtractor extractor = new PdfResumeTextExtractor();

    private void assertFails(byte[] pdf, Kind kind) {
        assertThatThrownBy(() -> extractor.extract(pdf))
                .isInstanceOfSatisfying(ResumeTextException.class, e -> assertThat(e.getKind()).isEqualTo(kind));
    }

    @Test
    void shouldReadTheTextOfAnOrdinaryResume() throws Exception {
        ExtractedText result = extractor.extract(TestPdfs.withPages(RESUME));

        assertThat(result.text()).contains("Backend Developer", "Skills: Java, Spring Boot, PostgreSQL, Docker");
        assertThat(result.pagesRead()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.textCapped()).isFalse();
        assertThat(result.cutShort()).isFalse();
    }

    @Test
    void shouldReadSeveralPagesInOrder() throws Exception {
        ExtractedText result = extractor.extract(TestPdfs.withPages(RESUME, "Second page: Projects and certificates in Kubernetes."));

        assertThat(result.pagesRead()).isEqualTo(2);
        assertThat(result.text().indexOf("Backend Developer")).isLessThan(result.text().indexOf("Second page"));
    }

    @Test
    void onlyTheFirstTenPagesShouldBeRead() throws Exception {
        String[] pages = new String[12];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = "Page " + (i + 1) + " has enough readable words to count as proper text";
        }

        ExtractedText result = extractor.extract(TestPdfs.withPages(pages));

        assertThat(result.pagesRead()).isEqualTo(PdfResumeTextExtractor.MAX_PAGES);
        assertThat(result.totalPages()).isEqualTo(12);
        assertThat(result.pagesCutShort()).isTrue();
        assertThat(result.cutShort()).isTrue();
        assertThat(result.text()).contains("Page 10 has").doesNotContain("Page 11 has");
    }

    @Test
    void veryLongTextShouldBeCutAtTheLimitAndSaySo() throws Exception {
        String line = "word ".repeat(16);   // 80 akshar
        String page = (line + "\n").repeat(50);
        String[] pages = new String[10];
        java.util.Arrays.fill(pages, page);

        ExtractedText result = extractor.extract(TestPdfs.withPages(pages));

        assertThat(result.textCapped()).isTrue();
        assertThat(result.cutShort()).isTrue();
        assertThat(result.text().length()).isLessThanOrEqualTo(PdfResumeTextExtractor.MAX_CHARS);
        assertThat(result.text().length()).isGreaterThan(PdfResumeTextExtractor.MAX_CHARS * 9 / 10);
    }

    @Test
    void aPdfWithPagesButNoTextShouldBeRecognisedAsAScan() throws Exception {
        assertFails(TestPdfs.blank(2), Kind.NO_TEXT);
    }

    @Test
    void aPdfWithOnlyAFewWordsShouldCountAsNoTextToo() throws Exception {
        assertFails(TestPdfs.withPages("Resume"), Kind.NO_TEXT);
    }

    @Test
    void aPdfWhoseTextIsMostlySymbolsShouldBeUnreadable() throws Exception {
        String junk = "#$%&*+=<>~^|".repeat(7);
        assertFails(TestPdfs.withPages(String.join("\n", junk, junk, junk, junk, junk)), Kind.UNREADABLE);
    }

    @Test
    void aPasswordProtectedPdfShouldBeReportedAsProtected() throws Exception {
        assertFails(TestPdfs.encrypted("user-secret", true, RESUME), Kind.ENCRYPTED);
    }

    @Test
    void aPdfThatForbidsCopyingTextShouldNotBeReadEvenIfItOpens() throws Exception {
        assertFails(TestPdfs.encrypted("", false, RESUME), Kind.ENCRYPTED);
    }

    @Test
    void aPdfWithOnlyAnOwnerPasswordThatAllowsTextExtractionShouldBeRead() throws Exception {
        ExtractedText result = extractor.extract(TestPdfs.encrypted("", true, RESUME));

        assertThat(result.text()).contains("Spring Boot");
    }

    @Test
    void notAPdfAtAllShouldBeUnreadableNotACrash() {
        assertFails("this is not a pdf, just text".getBytes(StandardCharsets.UTF_8), Kind.UNREADABLE);
        assertFails(new byte[]{0x25, 0x50, 0x44, 0x46, 0x2d, 0x31}, Kind.UNREADABLE);   // "%PDF-1" aur kuch nahi
    }

    @Test
    void emptyOrMissingInputShouldBeUnreadable() {
        assertFails(new byte[0], Kind.UNREADABLE);
        assertFails(null, Kind.UNREADABLE);
    }

    @Test
    void aFileOverTheSizeLimitShouldBeTooLargeButOneExactlyAtTheLimitShouldBeLookedAt() {
        assertFails(new byte[PdfResumeTextExtractor.MAX_BYTES + 1], Kind.TOO_LARGE);
        assertFails(new byte[PdfResumeTextExtractor.MAX_BYTES], Kind.UNREADABLE);   // size theek, par PDF nahi
    }

    @Test
    void theExtractedTextShouldBeTidyWithNoControlCharactersOrRunsOfBlankLines() throws Exception {
        ExtractedText result = extractor.extract(TestPdfs.withPages(
                "Skills:   Java,    Docker\n\n\n\n\nExperience: Built services with Spring Boot for hiring"));

        assertThat(result.text()).doesNotContain("   ").doesNotContain("\n\n\n");
        assertThat(result.text()).contains("Java,").contains("Docker");
    }
}