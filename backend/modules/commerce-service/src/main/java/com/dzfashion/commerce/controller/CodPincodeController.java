package com.dzfashion.commerce.controller;

import com.dzfashion.commerce.dto.request.AddCodPincodeRequest;
import com.dzfashion.commerce.dto.response.ActionResponse;
import com.dzfashion.commerce.dto.response.CodPincodeResponse;
import com.dzfashion.commerce.security.UserPrincipal;
import com.dzfashion.commerce.service.CodPincodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class CodPincodeController {

    private final CodPincodeService codPincodeService;

    public CodPincodeController(CodPincodeService codPincodeService) {
        this.codPincodeService = codPincodeService;
    }

    @GetMapping("/api/v1/cod-pincodes/check/{pincode}")
    public ResponseEntity<Map<String, Object>> checkPincode(@PathVariable String pincode) {
        boolean serviceable = codPincodeService.isPincodeServiceable(pincode);
        return ResponseEntity.ok(Map.of(
                "pincode", pincode,
                "serviceable", serviceable
        ));
    }

    @GetMapping("/api/v1/admin/cod-pincodes")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<CodPincodeResponse>> listPincodes() {
        return ResponseEntity.ok(codPincodeService.listServiceablePincodes());
    }

    @PostMapping("/api/v1/admin/cod-pincodes")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CodPincodeResponse> addPincode(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody AddCodPincodeRequest request) {
        UUID ownerId = principal != null ? principal.getId() : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED).body(codPincodeService.addPincode(request, ownerId));
    }

    @PatchMapping("/api/v1/admin/cod-pincodes/{pincode}/toggle")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CodPincodeResponse> togglePincode(@PathVariable String pincode,
                                                           @RequestParam boolean active) {
        return ResponseEntity.ok(codPincodeService.togglePincodeActive(pincode, active));
    }

    @DeleteMapping("/api/v1/admin/cod-pincodes/{pincode}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ActionResponse> deletePincode(@PathVariable String pincode) {
        codPincodeService.deletePincode(pincode);
        return ResponseEntity.ok(ActionResponse.ok("Pincode deleted successfully"));
    }
}
