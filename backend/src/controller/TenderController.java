package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.BidderResponse;
import com.sih.bidcompliance.dto.TenderDocumentRequirementDto;
import com.sih.bidcompliance.dto.TenderRequest;
import com.sih.bidcompliance.dto.TenderResponse;
import com.sih.bidcompliance.service.BidderService;
import com.sih.bidcompliance.service.TenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenders")
@RequiredArgsConstructor
public class TenderController {

    private final TenderService tenderService;
    private final BidderService bidderService;

    @PostMapping
    public ResponseEntity<TenderResponse> create(@Valid @RequestBody TenderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenderService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TenderResponse>> findAll() {
        return ResponseEntity.ok(tenderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tenderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenderResponse> update(@PathVariable Long id, @Valid @RequestBody TenderRequest request) {
        return ResponseEntity.ok(tenderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenderId}/bidders")
    public ResponseEntity<List<BidderResponse>> findBiddersForTender(@PathVariable Long tenderId) {
        return ResponseEntity.ok(bidderService.findByTenderId(tenderId));
    }

    @GetMapping("/{id}/document-requirements")
    public ResponseEntity<List<TenderDocumentRequirementDto>> getDocumentRequirements(@PathVariable Long id) {
        return ResponseEntity.ok(tenderService.getDocumentRequirements(id));
    }

    @PutMapping("/{id}/document-requirements")
    public ResponseEntity<List<TenderDocumentRequirementDto>> setDocumentRequirements(
            @PathVariable Long id, @Valid @RequestBody List<TenderDocumentRequirementDto> requirements
    ) {
        return ResponseEntity.ok(tenderService.setDocumentRequirements(id, requirements));
    }
}
