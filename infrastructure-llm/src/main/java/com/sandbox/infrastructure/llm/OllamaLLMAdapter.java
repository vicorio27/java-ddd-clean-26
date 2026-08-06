package com.sandbox.infrastructure.llm;

import com.sandbox.application.port.LLMPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sandbox.llm.provider", havingValue = "ollama")
public class OllamaLLMAdapter implements LLMPort {

    private final RestClient restClient;
    private final String model;

    public OllamaLLMAdapter(@Value("${sandbox.llm.ollama.base-url:http://localhost:11434}") String baseUrl,
                            @Value("${sandbox.llm.ollama.model:llama3}") String model) {
        this.restClient = RestClient.create(baseUrl);
        this.model = model;
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        var response = restClient.post()
                .uri("/api/generate")
                .body(new OllamaGenerateRequest(model, request.systemPrompt() + "\n" + request.userPrompt(), true))
                .retrieve()
                .body(OllamaGenerateResponse.class);
        return new LLMResponse(response != null ? response.response() : "", model);
    }

    record OllamaGenerateRequest(String model, String prompt, boolean stream) {
    }

    record OllamaGenerateResponse(String response) {
    }
}
