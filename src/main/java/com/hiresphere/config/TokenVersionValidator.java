package com.hiresphere.config;

import com.hiresphere.user.service.TokenVersionService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class TokenVersionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID = new OAuth2Error("invalid_token",
            "Session is no longer valid. Please log in again.", null);

    private final TokenVersionService tokenVersionService;

    public TokenVersionValidator(TokenVersionService tokenVersionService) {
        this.tokenVersionService = tokenVersionService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            return OAuth2TokenValidatorResult.failure(INVALID);
        }
        Object claim = jwt.getClaim("ver");
        long version = (claim instanceof Number n) ? n.longValue() : 0L;

        return tokenVersionService.isCurrent(userId, version)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID);
    }
}