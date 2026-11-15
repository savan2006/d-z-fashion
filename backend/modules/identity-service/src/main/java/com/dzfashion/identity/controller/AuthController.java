package com.dzfashion.identity.controller;

import com.dzfashion.identity.dto.request.ForgotPasswordInitiateRequest;
import com.dzfashion.identity.dto.request.LoginRequest;
import com.dzfashion.identity.dto.request.RefreshTokenRequest;
import com.dzfashion.identity.dto.request.ResetPasswordRequest;
import com.dzfashion.identity.dto.request.SignupCompleteRequest;
import com.dzfashion.identity.dto.request.SignupInitiateRequest;
import com.dzfashion.identity.dto.request.VerifyOtpRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.AuthResponse;
import com.dzfashion.identity.dto.response.OtpInitiateResponse;
import com.dzfashion.identity.dto.response.VerifyOtpResponse;
import com.dzfashion.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup/initiate")
    public ResponseEntity<OtpInitiateResponse> initiateSignup(@Valid @RequestBody SignupInitiateRequest request) {
        OtpInitiateResponse response = authService.initiateSignup(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/signup/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifySignupOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifySignupOtp(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<AuthResponse> completeSignup(@Valid @RequestBody SignupCompleteRequest request,
                                                       @RequestParam(value = "username", required = false) String username) {
        AuthResponse response = authService.completeSignup(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/forgot-password/initiate")
    public ResponseEntity<ActionResponse> initiateForgotPassword(@Valid @RequestBody ForgotPasswordInitiateRequest request) {
        ActionResponse response = authService.initiateForgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/forgot-password/complete")
    public ResponseEntity<ActionResponse> completeForgotPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ActionResponse response = authService.completeForgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
