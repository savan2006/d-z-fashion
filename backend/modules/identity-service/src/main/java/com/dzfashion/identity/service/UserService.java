package com.dzfashion.identity.service;

import com.dzfashion.identity.dto.request.CreateManagerRequest;
import com.dzfashion.identity.dto.request.UpdateProfileRequest;
import com.dzfashion.identity.dto.response.ActionResponse;
import com.dzfashion.identity.dto.response.UserProfileResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.entity.CustomerProfileEntity;
import com.dzfashion.identity.entity.UserEntity;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.exception.ResourceConflictException;
import com.dzfashion.identity.exception.ResourceNotFoundException;
import com.dzfashion.identity.repository.CustomerProfileRepository;
import com.dzfashion.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CustomerProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        CustomerProfileEntity profile = profileRepository.findById(userId)
                .orElse(null);

        return mapToProfileResponse(user, profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        CustomerProfileEntity profile = profileRepository.findById(userId)
                .orElseGet(() -> new CustomerProfileEntity(user, request.fullName()));

        profile.setFullName(request.fullName().trim());
        if (request.phoneNumber() != null) {
            profile.setPhoneNumber(request.phoneNumber().trim());
        }
        if (request.addressLine1() != null) {
            profile.setAddressLine1(request.addressLine1().trim());
        }
        if (request.addressLine2() != null) {
            profile.setAddressLine2(request.addressLine2().trim());
        }
        if (request.city() != null) {
            profile.setCity(request.city().trim());
        }
        if (request.state() != null) {
            profile.setState(request.state().trim());
        }
        if (request.postalCode() != null) {
            profile.setPostalCode(request.postalCode().trim());
        }
        if (request.country() != null && !request.country().isBlank()) {
            profile.setCountry(request.country().trim());
        }

        CustomerProfileEntity saved = profileRepository.save(profile);
        return mapToProfileResponse(user, saved);
    }

    @Transactional
    public UserSummaryResponse createManager(CreateManagerRequest request) {
        String normalizedUsername = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResourceConflictException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        UserEntity manager = new UserEntity(normalizedUsername, normalizedEmail, passwordHash, Role.MANAGER);
        CustomerProfileEntity profile = new CustomerProfileEntity(manager, request.fullName().trim());
        manager.setProfile(profile);

        UserEntity saved = userRepository.save(manager);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole(),
                saved.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listManagers() {
        return userRepository.findByRole(Role.MANAGER).stream()
                .map(u -> new UserSummaryResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStatus()))
                .toList();
    }

    @Transactional
    public ActionResponse resetManagerPassword(UUID managerId, String newPassword) {
        UserEntity manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerId));

        if (manager.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("User is not a Manager");
        }

        manager.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(manager);

        return new ActionResponse("Manager password has been reset successfully");
    }

    @Transactional
    public UserEntity bootstrapOwnerIfAbsent(String username, String email, String rawPassword, String fullName) {
        if (userRepository.countByRole(Role.OWNER) > 0) {
            return null;
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        UserEntity owner = new UserEntity(username.trim(), email.trim().toLowerCase(), passwordHash, Role.OWNER);
        CustomerProfileEntity profile = new CustomerProfileEntity(owner, fullName.trim());
        owner.setProfile(profile);

        return userRepository.save(owner);
    }

    private UserProfileResponse mapToProfileResponse(UserEntity user, CustomerProfileEntity profile) {
        String fullName = profile != null ? profile.getFullName() : user.getUsername();
        String phone = profile != null ? profile.getPhoneNumber() : null;
        String addressLine1 = profile != null ? profile.getAddressLine1() : null;
        String addressLine2 = profile != null ? profile.getAddressLine2() : null;
        String city = profile != null ? profile.getCity() : null;
        String state = profile != null ? profile.getState() : null;
        String postalCode = profile != null ? profile.getPostalCode() : null;
        String country = profile != null ? profile.getCountry() : "India";

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                fullName,
                phone,
                addressLine1,
                addressLine2,
                city,
                state,
                postalCode,
                country
        );
    }
}
