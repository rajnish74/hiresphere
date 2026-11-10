package com.hiresphere.ai.resume;

import com.hiresphere.common.exception.AppException;
import org.springframework.http.HttpStatus;


public class ResumeTextException extends AppException {

    public enum Kind {
        NO_RESUME(HttpStatus.CONFLICT, "Upload your resume first, then try again."),
        ENCRYPTED(HttpStatus.UNPROCESSABLE_ENTITY,
                "Your resume PDF is password protected. Please upload an unprotected copy."),
        UNREADABLE(HttpStatus.UNPROCESSABLE_ENTITY,
                "We could not read this PDF. Try exporting it again from Word or Google Docs."),
        NO_TEXT(HttpStatus.UNPROCESSABLE_ENTITY,
                "This PDF looks like a scan or an image, so there is no text to read. "
                        + "Please upload a PDF with selectable text."),
        TOO_LARGE(HttpStatus.UNPROCESSABLE_ENTITY, "This resume is too large to analyse.");

        private final HttpStatus status;
        private final String publicMessage;

        Kind(HttpStatus status, String publicMessage) {
            this.status = status;
            this.publicMessage = publicMessage;
        }
    }

    private final Kind kind;
    private final String detail;

    public ResumeTextException(Kind kind, String detail) {
        super(kind.status, kind.publicMessage);
        this.kind = kind;
        this.detail = detail;
    }

    public Kind getKind() {
        return kind;
    }

    public String getDetail() {
        return detail;
    }
}