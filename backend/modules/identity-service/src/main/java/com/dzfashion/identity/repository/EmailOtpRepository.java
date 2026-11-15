package com.dzfashion.identity.repository;

import com.dzfashion.identity.entity.EmailOtpEntity;
import com.dzfashion.identity.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtpEntity, UUID> {

    Optional<EmailOtpEntity> findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}
