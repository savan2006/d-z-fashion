package com.dzfashion.commerce.repository;

import com.dzfashion.commerce.entity.CodServiceablePincodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodServiceablePincodeRepository extends JpaRepository<CodServiceablePincodeEntity, UUID> {
    Optional<CodServiceablePincodeEntity> findByPincode(String pincode);
    boolean existsByPincodeAndActiveTrue(String pincode);
    List<CodServiceablePincodeEntity> findByActiveTrue();
}
