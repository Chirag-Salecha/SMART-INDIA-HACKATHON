package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.DocumentQueueItemResponse;
import com.sih.bidcompliance.dto.DocumentResponse;
import com.sih.bidcompliance.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cross-bidder document operations that don't fit under the
 * /api/bidders/{bidderId}/documents nesting: the verification queue,
 * fetching a single document by id, and re-triggering AI analysis.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentQueryController {

    private final DocumentService documentService;

    @GetMapping("/queue")
    public ResponseEntity<List<DocumentQueueItemResponse>> getQueue() {
        return ResponseEntity.ok(documentService.getQueue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<DocumentResponse> reanalyze(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.reanalyze(id));
    }
}
