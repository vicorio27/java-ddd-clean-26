package com.sandbox.application.port;

public interface LLMPort {

    LLMResponse complete(LLMRequest request);

    record LLMRequest(String systemPrompt, String userPrompt) {
    }

    record LLMResponse(String content, String model) {
    }
}
