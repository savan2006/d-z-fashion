package com.dzfashion.identity.controller;

import com.dzfashion.identity.dto.request.CreateManagerRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/managers")
@PreAuthorize("hasRole('OWNER')")
public class ManagerAdminController {

    private final UserService userService;

    public ManagerAdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createManager(@Valid @RequestBody CreateManagerRequest request) {
        UserSummaryResponse created = userService.createManager(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listManagers() {
        List<UserSummaryResponse> managers = userService.listManagers();
        return ResponseEntity.ok(managers);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ActionResponse> resetManagerPassword(@PathVariable UUID id,
                                                               @RequestParam("newPassword") String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        ActionResponse response = userService.resetManagerPassword(id, newPassword);
        return ResponseEntity.ok(response);
    }
}
