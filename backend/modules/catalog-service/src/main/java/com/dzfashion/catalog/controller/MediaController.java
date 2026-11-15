package com.dzfashion.catalog.controller;

import com.dzfashion.catalog.dto.request.RegisterMediaRequest;
import com.dzfashion.catalog.dto.response.ActionResponse;
import com.dzfashion.catalog.dto.response.MediaAssetResponse;
import com.dzfashion.catalog.dto.response.MediaSignatureResponse;
import com.dzfashion.catalog.service.CloudinaryMediaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/media")
public class MediaController {

    private final CloudinaryMediaService mediaService;

    public MediaController(CloudinaryMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/signature")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<MediaSignatureResponse> getUploadSignature() {
        MediaSignatureResponse signature = mediaService.generateUploadSignature();
        return ResponseEntity.ok(signature);
    }

    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<MediaAssetResponse> registerMedia(
            @PathVariable UUID productId,
            @Valid @RequestBody RegisterMediaRequest request) {
        MediaAssetResponse response = mediaService.registerMedia(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<MediaAssetResponse>> getProductMedia(@PathVariable UUID productId) {
        List<MediaAssetResponse> media = mediaService.getProductMedia(productId);
        return ResponseEntity.ok(media);
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ActionResponse> deleteMedia(@PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(new ActionResponse("Media asset deleted successfully"));
    }
}
