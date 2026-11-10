package com.hiresphere.ai.resume;

import com.hiresphere.ai.prompt.UntrustedText;
import com.hiresphere.ai.resume.ResumeTextException.Kind;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PdfResumeTextExtractor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_PAGES = 10;
    public static final int MAX_CHARS = 30_000;
    public static final int MIN_USEFUL_CHARS = 40;
    public static final double MIN_READABLE_RATIO = 0.5;

    public ExtractedText extract(byte[] pdf) {
        if (pdf == null || pdf.length == 0) {
            throw new ResumeTextException(Kind.UNREADABLE, "empty file");
        }
        if (pdf.length > MAX_BYTES) {
            throw new ResumeTextException(Kind.TOO_LARGE, pdf.length + " bytes");
        }

        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.isEncrypted() && !document.getCurrentAccessPermission().canExtractContent()) {
                throw new ResumeTextException(Kind.ENCRYPTED, "text extraction is not permitted");
            }
            int total = document.getNumberOfPages();
            if (total < 1) {
                throw new ResumeTextException(Kind.UNREADABLE, "no pages");
            }
            int last = Math.min(total, MAX_PAGES);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(last);

            BoundedWriter writer = new BoundedWriter(MAX_CHARS);
            try {
                stripper.writeText(document, writer);
            } catch (BoundedWriter.LimitReached reached) {
            }

            String text = UntrustedText.clean(writer.text(), MAX_CHARS).text();
            requireUsable(text);
            log.info("Resume PDF read: {} of {} page(s), {} characters{}", last, total, text.length(),
                    writer.capped() ? " (cut at the limit)" : "");   // text kabhi log mein nahi
            return new ExtractedText(text, last, total, writer.capped());

        } catch (InvalidPasswordException e) {
            throw new ResumeTextException(Kind.ENCRYPTED, "a password is required");
        } catch (ResumeTextException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ResumeTextException(Kind.UNREADABLE, e.getClass().getSimpleName());
        }
    }

    private static void requireUsable(String text) {
        long visible = text.codePoints().filter(cp -> !Character.isWhitespace(cp)).count();
        if (visible < MIN_USEFUL_CHARS) {
            throw new ResumeTextException(Kind.NO_TEXT, visible + " visible characters");
        }
        long readable = text.codePoints().filter(Character::isLetterOrDigit).count();
        if ((double) readable / visible < MIN_READABLE_RATIO) {
            throw new ResumeTextException(Kind.UNREADABLE, "mostly symbols (" + readable + " of " + visible + ")");
        }
    }
}