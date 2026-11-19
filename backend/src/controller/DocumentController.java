package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.DocumentResponse;
import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bidders/{bidderId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable Long bidderId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.upload(bidderId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> findByBidder(@PathVariable Long bidderId) {
        return ResponseEntity.ok(documentService.findByBidderId(bidderId));
    }
}
