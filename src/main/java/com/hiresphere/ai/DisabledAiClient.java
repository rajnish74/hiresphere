package com.hiresphere.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledAiClient implements AiClient {

    @Override
    public AiResponse generate(AiRequest request) {
        throw new AiException(AiException.Kind.DISABLED, "app.ai.enabled is false");
    }
}