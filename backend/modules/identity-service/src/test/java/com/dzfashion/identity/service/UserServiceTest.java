package com.dzfashion.identity.service;

import com.dzfashion.identity.dto.request.CreateManagerRequest;
import com.dzfashion.identity.dto.request.UpdateProfileRequest;
import com.dzfashion.identity.dto.response.UserProfileResponse;
import com.dzfashion.identity.dto.response.UserSummaryResponse;
import com.dzfashion.identity.entity.CustomerProfileEntity;
import com.dzfashion.identity.entity.UserEntity;
import com.dzfashion.identity.enums.Role;
import com.dzfashion.identity.enums.UserStatus;
import com.dzfashion.identity.exception.ResourceConflictException;
import com.dzfashion.identity.repository.CustomerProfileRepository;
import com.dzfashion.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository profileRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        userService = new UserService(userRepository, profileRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Owner creates Manager account with MANAGER role")
    void testCreateManager() {
        CreateManagerRequest request = new CreateManagerRequest("mgr_surat", "mgr@dzfashion.com", "ManagerPass123", "Rajesh Kumar");

        when(userRepository.existsByUsernameIgnoreCase("mgr_surat")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("mgr@dzfashion.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserSummaryResponse response = userService.createManager(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();

        assertEquals(Role.MANAGER, saved.getRole());
        assertEquals("mgr_surat", saved.getUsername());
        assertEquals("mgr@dzfashion.com", saved.getEmail());
        assertTrue(passwordEncoder.matches("ManagerPass123", saved.getPasswordHash()));
        assertEquals(Role.MANAGER, response.role());
    }

    @Test
    @DisplayName("Reject creating manager with duplicate username")
    void testCreateManagerDuplicateUsername() {
        CreateManagerRequest request = new CreateManagerRequest("existing_mgr", "new@dzfashion.com", "ManagerPass123", "Rajesh Kumar");
        when(userRepository.existsByUsernameIgnoreCase("existing_mgr")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> userService.createManager(request));
    }

    @Test
    @DisplayName("Owner resets manager password")
    void testResetManagerPassword() {
        UUID managerId = UUID.randomUUID();
        UserEntity manager = new UserEntity("mgr1", "mgr1@dzfashion.com", passwordEncoder.encode("OldPass1"), Role.MANAGER);
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

        userService.resetManagerPassword(managerId, "BrandNewPass123");

        assertTrue(passwordEncoder.matches("BrandNewPass123", manager.getPasswordHash()));
        verify(userRepository).save(manager);
    }

    @Test
    @DisplayName("Resetting manager password fails if target user is not a Manager")
    void testResetManagerPasswordFailsForCustomer() {
        UUID customerId = UUID.randomUUID();
        UserEntity customer = new UserEntity("cust1", "cust@example.com", "hash", Role.CUSTOMER);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        assertThrows(IllegalArgumentException.class, () -> userService.resetManagerPassword(customerId, "NewPass123"));
    }

    @Test
    @DisplayName("Update customer profile address snapshot")
    void testUpdateProfile() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("radhika", "radhika@example.com", "hash", Role.CUSTOMER);
        user.setId(userId);

        CustomerProfileEntity existing = new CustomerProfileEntity(user, "Radhika Shah");
        existing.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CustomerProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Radhika Shah",
                "+91 9876543210",
                "101, Silk City Apartments",
                "Ring Road",
                "Surat",
                "Gujarat",
                "395002",
                "India"
        );

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertEquals("Surat", response.city());
        assertEquals("Gujarat", response.state());
        assertEquals("395002", response.postalCode());
        assertEquals("101, Silk City Apartments", response.addressLine1());
        assertEquals("+91 9876543210", response.phoneNumber());
    }

    @Test
    @DisplayName("Bootstrap initial owner only when no owner exists")
    void testBootstrapOwner() {
        when(userRepository.countByRole(Role.OWNER)).thenReturn(0L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity created = userService.bootstrapOwnerIfAbsent("admin_owner", "owner@dzfashion.com", "OwnerSecret123", "Super Owner");
        assertNotNull(created);
        assertEquals(Role.OWNER, created.getRole());

        // When owner already exists:
        when(userRepository.countByRole(Role.OWNER)).thenReturn(1L);
        UserEntity skipped = userService.bootstrapOwnerIfAbsent("admin_owner", "owner@dzfashion.com", "OwnerSecret123", "Super Owner");
        assertNull(skipped, "Should skip bootstrap when OWNER already exists");
    }
}
