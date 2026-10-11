
package com.agrisaarthi.agrichat_service.service;

import com.agrisaarthi.agrichat_service.dto.KnowledgeRequest;
import com.agrisaarthi.agrichat_service.dto.KnowledgeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KnowledgeClient {
    private final RestClient restClient;

    public KnowledgeClient(RestClient.Builder builder,
            @Value("${agrisaarthi.knowledge-service-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public KnowledgeResponse retrieve(KnowledgeRequest request) {
        // Do not hide retrieval failures: the caller records an explicit error rather than
        // pretending that a real retrieval succeeded.
        return restClient.post().uri("/api/knowledge/retrieve")
                .body(request)
                .retrieve()
                .body(KnowledgeResponse.class);
    }
}
