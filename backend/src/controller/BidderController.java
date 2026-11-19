package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.BidderRequest;
import com.sih.bidcompliance.dto.BidderResponse;
import com.sih.bidcompliance.service.BidderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bidders")
@RequiredArgsConstructor
public class BidderController {

    private final BidderService bidderService;

    @PostMapping
    public ResponseEntity<BidderResponse> create(@Valid @RequestBody BidderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bidderService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BidderResponse>> findAll() {
        return ResponseEntity.ok(bidderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BidderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bidderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BidderResponse> update(@PathVariable Long id, @Valid @RequestBody BidderRequest request) {
        return ResponseEntity.ok(bidderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bidderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
