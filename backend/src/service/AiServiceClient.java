package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.AiAnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Thin HTTP client for the FastAPI ai-service. Isolated in its own class so
 * DocumentService/AiVerificationService never deal with HTTP/multipart
 * plumbing directly, and so a future swap (different transport, retries,
 * etc.) stays contained here.
 */
@Component
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(
            @Value("${ai.service.base-url}") String baseUrl,
            @Value("${ai.service.timeout-seconds}") int timeoutSeconds
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Calls POST /api/ai/analyze on the AI service. Throws on any failure
     * (connection refused, timeout, non-2xx, bad JSON) - the caller
     * (AiVerificationService) is responsible for catching and degrading
     * gracefully so a document upload never fails because the AI service
     * is unavailable.
     */
    public AiAnalysisResponse analyze(
            MultipartFile file,
            String documentType,
            String companyName,
            String tenderRequirements
    ) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("document_type", documentType);
        if (companyName != null && !companyName.isBlank()) {
            parts.add("company_name", companyName);
        }
        if (tenderRequirements != null && !tenderRequirements.isBlank()) {
            parts.add("tender_requirements", tenderRequirements);
        }
        parts.add("file", toResource(file));

        return restClient.post()
                .uri("/api/ai/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(AiAnalysisResponse.class);
    }

    private Resource toResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Could not read uploaded file for AI analysis", e);
        }
    }
}
