package com.hiresphere.ai;

public interface AiClient {

    /** @throws AiException  */
    AiResponse generate(AiRequest request);
}