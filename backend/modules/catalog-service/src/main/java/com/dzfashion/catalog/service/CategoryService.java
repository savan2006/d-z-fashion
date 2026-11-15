package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateCategoryRequest;
import com.dzfashion.catalog.dto.request.UpdateCategoryRequest;
import com.dzfashion.catalog.dto.response.CategoryResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceConflictException;
import com.dzfashion.catalog.exception.ResourceNotFoundException;
import com.dzfashion.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new ResourceConflictException("Category slug already exists: " + request.slug());
        }

        CategoryEntity parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.parentId()));
        }

        CategoryEntity entity = new CategoryEntity(
                request.name().trim(),
                request.slug().trim().toLowerCase(),
                request.description(),
                parent,
                request.displayOrder()
        );

        CategoryEntity saved = categoryRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResourceConflictException("Category slug already in use by another category: " + request.slug());
        }

        CategoryEntity parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new InvalidOperationException("A category cannot be its own parent");
            }
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.parentId()));
        }

        entity.setName(request.name().trim());
        entity.setSlug(request.slug().trim().toLowerCase());
        entity.setDescription(request.description());
        entity.setParent(parent);
        entity.setDisplayOrder(request.displayOrder());
        entity.setActive(request.active());

        CategoryEntity updated = categoryRepository.save(entity);
        return mapToResponse(updated);
    }

    @Transactional
    public void deactivateCategory(UUID id) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        entity.setActive(false);
        deactivateChildrenRecursively(entity);
        categoryRepository.save(entity);
    }

    private void deactivateChildrenRecursively(CategoryEntity parent) {
        if (parent.getChildren() != null) {
            for (CategoryEntity child : parent.getChildren()) {
                child.setActive(false);
                deactivateChildrenRecursively(child);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree(boolean includeInactive) {
        List<CategoryEntity> rootEntities = includeInactive
                ? categoryRepository.findByParentIsNullOrderByDisplayOrderAsc()
                : categoryRepository.findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();

        return rootEntities.stream()
                .map(e -> mapTreeToResponse(e, includeInactive))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        CategoryEntity entity = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return mapToResponse(entity);
    }

    private CategoryResponse mapToResponse(CategoryEntity entity) {
        UUID parentId = entity.getParent() != null ? entity.getParent().getId() : null;
        return new CategoryResponse(
                entity.getId(),
                parentId,
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getDisplayOrder(),
                Collections.emptyList()
        );
    }

    private CategoryResponse mapTreeToResponse(CategoryEntity entity, boolean includeInactive) {
        UUID parentId = entity.getParent() != null ? entity.getParent().getId() : null;
        List<CategoryResponse> children = Collections.emptyList();
        if (entity.getChildren() != null) {
            children = entity.getChildren().stream()
                    .filter(c -> includeInactive || c.isActive())
                    .map(c -> mapTreeToResponse(c, includeInactive))
                    .collect(Collectors.toList());
        }

        return new CategoryResponse(
                entity.getId(),
                parentId,
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getDisplayOrder(),
                children
        );
    }
}
