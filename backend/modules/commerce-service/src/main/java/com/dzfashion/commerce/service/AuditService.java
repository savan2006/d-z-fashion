package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.response.AuditLogResponse;
import com.dzfashion.commerce.dto.response.PageResponse;
import com.dzfashion.commerce.entity.AuditLogEntity;
import com.dzfashion.commerce.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "(?i)[\"']?(password|otp|secret|token|authorization|apikey|access_token|refresh_token)[\"']?\\s*[:=]\\s*(?:\"[^\"]*\"|'[^']*'|[^\"',\\s}]+)"
    );

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[^\"',\\s}]+");

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLogResponse recordAudit(UUID actorId, String actorRole, String actorEmail, String action,
                                        String entityType, String entityId, String details, String ipAddress) {
        String sanitizedDetails = maskSensitiveData(details);

        AuditLogEntity entity = new AuditLogEntity(
                actorId,
                actorRole,
                actorEmail,
                action,
                entityType,
                entityId,
                sanitizedDetails,
                ipAddress
        );

        AuditLogEntity saved = auditLogRepository.save(entity);
        log.info("Audit log recorded: id={}, actorRole={}, action={}, entityType={}, entityId={}",
                saved.getId(), actorRole, action, entityType, entityId);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(String entityType, String action, UUID actorId, Pageable pageable) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> cb.conjunction();
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType.trim()));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), actorId));
        }

        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, pageable);
        List<AuditLogResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public String maskSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String masked = BEARER_PATTERN.matcher(input).replaceAll("Bearer ***MASKED***");
        masked = SENSITIVE_KEY_PATTERN.matcher(masked).replaceAll("$1=***MASKED***");
        return masked;
    }

    private AuditLogResponse mapToResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getActorRole(),
                entity.getActorEmail(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getDetails(),
                entity.getIpAddress(),
                entity.getCreatedAt()
        );
    }
}
