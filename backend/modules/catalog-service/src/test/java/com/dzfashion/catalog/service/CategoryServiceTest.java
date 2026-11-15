package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateCategoryRequest;
import com.dzfashion.catalog.dto.request.UpdateCategoryRequest;
import com.dzfashion.catalog.dto.response.CategoryResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.exception.ResourceConflictException;
import com.dzfashion.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Create category succeeds when slug is unique")
    void createCategorySuccess() {
        CreateCategoryRequest request = new CreateCategoryRequest("Kurtas", "kurtas", "Women kurtas", null, 1);
        when(categoryRepository.existsBySlug("kurtas")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> {
            CategoryEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Kurtas");
        assertThat(response.slug()).isEqualTo("kurtas");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("Create category throws ResourceConflictException on duplicate slug")
    void createCategoryDuplicateSlug() {
        CreateCategoryRequest request = new CreateCategoryRequest("Kurtas", "kurtas", null, null, 1);
        when(categoryRepository.existsBySlug("kurtas")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Category slug already exists");
    }

    @Test
    @DisplayName("Update category rejects self-parenting")
    void updateCategorySelfParentingRejected() {
        UUID categoryId = UUID.randomUUID();
        CategoryEntity entity = new CategoryEntity("Kurtas", "kurtas", null, null, 1);
        entity.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsBySlugAndIdNot("kurtas", categoryId)).thenReturn(false);

        UpdateCategoryRequest request = new UpdateCategoryRequest("Kurtas", "kurtas", null, categoryId, 1, true);

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    @DisplayName("Deactivate category cascades active=false to children")
    void deactivateCategoryCascades() {
        UUID parentId = UUID.randomUUID();
        CategoryEntity parent = new CategoryEntity("Ethnic Wear", "ethnic-wear", null, null, 1);
        parent.setId(parentId);

        CategoryEntity child = new CategoryEntity("Kurtas", "kurtas", null, parent, 1);
        parent.setChildren(List.of(child));

        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));

        categoryService.deactivateCategory(parentId);

        assertThat(parent.isActive()).isFalse();
        assertThat(child.isActive()).isFalse();
        verify(categoryRepository).save(parent);
    }
}
