package com.Hack.AgriSaarthi.controller;

import com.Hack.AgriSaarthi.model.KnowledgeRequest;
import com.Hack.AgriSaarthi.model.KnowledgeResponse;
import com.Hack.AgriSaarthi.service.KnowledgeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/retrieve")
    public ResponseEntity<KnowledgeResponse> retrieve(
            @RequestBody(required = false) KnowledgeRequest request) {

        if (request == null ||
                request.query() == null ||
                request.query().isBlank()) {

            return ResponseEntity.badRequest().build();
        }

        if ((request.latitude() == null) !=
                (request.longitude() == null)) {
            return ResponseEntity.badRequest().build();
        }

        if (request.latitude() != null &&
                (request.latitude() < -90 || request.latitude() > 90 ||
                 request.longitude() < -180 || request.longitude() > 180)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(knowledgeService.search(request));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Agri knowledge service is running");
    }
}
