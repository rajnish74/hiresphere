package com.hiresphere.ai;

import com.hiresphere.common.exception.AppException;
import org.springframework.http.HttpStatus;


public class AiException extends AppException {

    public enum Kind {
        DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "AI features are not enabled right now.", false),
        TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The AI service took too long to respond. Please try again.", true),
        RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE,
                "The AI service is busy right now. Please try again in a minute.", true),
        MISCONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI is temporarily unavailable.", false),
        UPSTREAM(HttpStatus.BAD_GATEWAY, "The AI service had a problem. Please try again.", true),
        BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "The AI could not process this content.", false),
        EMPTY(HttpStatus.BAD_GATEWAY, "The AI did not return an answer. Please try again.", false),
        INVALID_OUTPUT(HttpStatus.BAD_GATEWAY, "The AI returned an answer we could not use. Please try again.", true);

        private final HttpStatus status;
        private final String publicMessage;
        private final boolean retryable;

        Kind(HttpStatus status, String publicMessage, boolean retryable) {
            this.status = status;
            this.publicMessage = publicMessage;
            this.retryable = retryable;
        }
    }

    private final Kind kind;
    private final String detail;

    public AiException(Kind kind, String detail) {
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

    public boolean isRetryable() {
        return kind.retryable;
    }
}