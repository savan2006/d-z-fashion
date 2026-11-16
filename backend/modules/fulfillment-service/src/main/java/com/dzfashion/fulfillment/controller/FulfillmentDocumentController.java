package com.dzfashion.fulfillment.controller;

import com.dzfashion.fulfillment.dto.document.DailyPickingListDocument;
import com.dzfashion.fulfillment.dto.document.DispatchManifestDocument;
import com.dzfashion.fulfillment.dto.document.PackingSlipDocument;
import com.dzfashion.fulfillment.service.FulfillmentDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment/documents")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class FulfillmentDocumentController {

    private final FulfillmentDocumentService fulfillmentDocumentService;

    public FulfillmentDocumentController(FulfillmentDocumentService fulfillmentDocumentService) {
        this.fulfillmentDocumentService = fulfillmentDocumentService;
    }

    @GetMapping("/picking-list")
    public ResponseEntity<DailyPickingListDocument> getCombinedPickingList() {
        return ResponseEntity.ok(fulfillmentDocumentService.generateCombinedPickingList());
    }

    @GetMapping("/packing-slip/{orderId}")
    public ResponseEntity<PackingSlipDocument> getPackingSlip(@PathVariable UUID orderId) {
        return ResponseEntity.ok(fulfillmentDocumentService.generatePackingSlip(orderId));
    }

    @GetMapping("/dispatch-manifest/{batchId}")
    public ResponseEntity<DispatchManifestDocument> getDispatchManifest(@PathVariable UUID batchId) {
        return ResponseEntity.ok(fulfillmentDocumentService.generateDispatchManifest(batchId));
    }
}
