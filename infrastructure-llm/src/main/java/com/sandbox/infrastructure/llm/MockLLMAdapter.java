package com.sandbox.infrastructure.llm;

import com.sandbox.application.port.LLMPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sandbox.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLLMAdapter implements LLMPort {

    @Override
    public LLMResponse complete(LLMRequest request) {
        return new LLMResponse("MOCK-RESPONSE: " + request.userPrompt(), "mock-llm");
    }
}
