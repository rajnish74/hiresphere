package com.hiresphere.ai.resume;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Tests ke liye asli PDF banata hai. Har argument ek page hai, aur uske andar \n se alag lines. */
public final class TestPdfs {

    @FunctionalInterface
    interface Step {
        void apply(PDDocument document) throws IOException;
    }

    private TestPdfs() {
    }

    public static byte[] withPages(String... pageTexts) throws IOException {
        return build(null, pageTexts);
    }

    /** Khali pages: scan ki hui PDF jaisi (koi text nahi). */
    public static byte[] blank(int pages) throws IOException {
        String[] empty = new String[pages];
        java.util.Arrays.fill(empty, "");
        return build(null, empty);
    }

    public static byte[] encrypted(String userPassword, boolean allowTextExtraction, String... pageTexts)
            throws IOException {
        AccessPermission permission = new AccessPermission();
        permission.setCanExtractContent(allowTextExtraction);
        StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-secret", userPassword, permission);
        policy.setEncryptionKeyLength(128);
        return build(document -> document.protect(policy), pageTexts);
    }

    private static byte[] build(Step beforeSave, String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (pageText.isEmpty()) {
                    continue;
                }
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 10);
                    content.setLeading(14);
                    content.newLineAtOffset(40, 750);
                    for (String line : pageText.split("\n")) {
                        content.showText(line);
                        content.newLine();
                    }
                    content.endText();
                }
            }
            if (beforeSave != null) {
                beforeSave.apply(document);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}