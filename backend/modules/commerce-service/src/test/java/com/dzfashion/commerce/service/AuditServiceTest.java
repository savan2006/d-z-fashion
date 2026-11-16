package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.response.AuditLogResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.entity.AuditLogEntity;
import com.dzfashion.commerce.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID actorId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Audit logging strictly sanitizes secrets, passwords, OTPs, and Bearer tokens")
    void testRecordAudit_SanitizesSensitiveData() {
        String sensitiveDetails = "{\"action\":\"login\", \"password\":\"Secret123!\", \"otp\":\"994821\", \"token\":\"xyz-token\", \"auth\":\"Bearer eyJhbGciOiJIUzI1NiJ9.test\"}";

        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        auditService.recordAudit(
                actorId, "MANAGER", "manager@dzfashion.com", "LOGIN_ATTEMPT",
                "Auth", actorId.toString(), sensitiveDetails, "192.168.1.1"
        );

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity logged = captor.getValue();

        assertThat(logged.getDetails()).doesNotContain("Secret123!");
        assertThat(logged.getDetails()).doesNotContain("994821");
        assertThat(logged.getDetails()).doesNotContain("eyJhbGci");
        assertThat(logged.getDetails()).contains("password=***MASKED***");
        assertThat(logged.getDetails()).contains("otp=***MASKED***");
        assertThat(logged.getDetails()).contains("Bearer ***MASKED***");
    }

    @Test
    @DisplayName("Get audit logs returns paginated results")
    void testGetAuditLogs_ReturnsPaginated() {
        AuditLogEntity log = new AuditLogEntity(
                actorId, "OWNER", "owner@dzfashion.com", "UPDATE_PRICE",
                "Product", "KURTI-01", "Updated price to ₹1499", "127.0.0.1"
        );
        log.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        PageResponse<AuditLogResponse> response = auditService.getAuditLogs("Product", "UPDATE_PRICE", null, pageable);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).action()).isEqualTo("UPDATE_PRICE");
        assertThat(response.content().get(0).entityType()).isEqualTo("Product");
    }
}
