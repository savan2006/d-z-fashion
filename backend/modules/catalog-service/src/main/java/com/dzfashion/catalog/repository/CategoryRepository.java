package com.dzfashion.catalog.repository;

import com.dzfashion.catalog.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<CategoryEntity> findByParentIsNullOrderByDisplayOrderAsc();

    List<CategoryEntity> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();

    List<CategoryEntity> findByActiveTrueOrderByDisplayOrderAsc();
}
