package com.dzfashion.catalog.service;

import com.dzfashion.catalog.dto.request.CreateVariantDraftRequest;
import com.dzfashion.catalog.dto.request.ReviewRevisionRequest;
import com.dzfashion.catalog.dto.request.SubmitRevisionRequest;
import com.dzfashion.catalog.dto.response.ProductRevisionResponse;
import com.dzfashion.catalog.entity.CategoryEntity;
import com.dzfashion.catalog.entity.ProductEntity;
import com.dzfashion.catalog.entity.ProductRevisionEntity;
import com.dzfashion.catalog.enums.RevisionStatus;
import com.dzfashion.catalog.exception.InvalidOperationException;
import com.dzfashion.catalog.repository.CategoryRepository;
import com.dzfashion.catalog.repository.ProductRepository;
import com.dzfashion.catalog.repository.ProductRevisionRepository;
import com.dzfashion.catalog.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRevisionServiceTest {

    @Mock
    private ProductRevisionRepository productRevisionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SkuGeneratorService skuGeneratorService;

    @InjectMocks
    private ProductRevisionService revisionService;

    private ProductEntity liveProduct;
    private CategoryEntity category;
    private UUID productId;
    private UUID managerId;
    private UUID ownerId;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        category = new CategoryEntity("Kurtas", "kurtas", null, null, 1);
        category.setId(UUID.randomUUID());

        liveProduct = new ProductEntity(
                category,
                "Original Kurta",
                "original-kurta",
                "Original description",
                "Cotton",
                "Daily",
                new BigDecimal("999.00"),
                ownerId
        );
        liveProduct.setId(productId);
    }

    @Test
    @DisplayName("Manager submits proposed modifications: live product is NOT changed, revision is PENDING")
    void submitRevisionPreservesLiveProduct() {
        SubmitRevisionRequest request = new SubmitRevisionRequest(
                "Updated Kurta Name",
                "New description with enhanced embroidery",
                "Silk Blend",
                "Festive",
                null,
                List.of(new CreateVariantDraftRequest("L", "Navy")),
                List.of()
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(liveProduct));
        when(productRevisionRepository.save(any(ProductRevisionEntity.class))).thenAnswer(invocation -> {
            ProductRevisionEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ProductRevisionResponse response = revisionService.submitRevision(productId, managerId, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(RevisionStatus.PENDING);
        assertThat(response.submittedBy()).isEqualTo(managerId);

        // Verify live product was not mutated
        assertThat(liveProduct.getName()).isEqualTo("Original Kurta");
        assertThat(liveProduct.getDescription()).isEqualTo("Original description");
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Owner rejects revision: live product remains unchanged and rejection reason is saved")
    void rejectRevisionKeepsLiveProductUnchanged() throws Exception {
        UUID revisionId = UUID.randomUUID();
        SubmitRevisionRequest proposedChanges = new SubmitRevisionRequest(
                "Rejected Name", "Rejected desc", null, null, null, List.of(), List.of()
        );
        ProductRevisionEntity revision = new ProductRevisionEntity(
                liveProduct, managerId, objectMapper.writeValueAsString(proposedChanges)
        );
        revision.setId(revisionId);

        when(productRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(productRevisionRepository.save(any(ProductRevisionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewRevisionRequest reviewRequest = new ReviewRevisionRequest(false, "Fabric description not accurate");
        ProductRevisionResponse response = revisionService.reviewRevision(revisionId, ownerId, reviewRequest);

        assertThat(response.status()).isEqualTo(RevisionStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Fabric description not accurate");
        assertThat(response.reviewedBy()).isEqualTo(ownerId);

        // Live product remains original
        assertThat(liveProduct.getName()).isEqualTo("Original Kurta");
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Owner approves revision: live product is updated with proposed changes")
    void approveRevisionUpdatesLiveProduct() throws Exception {
        UUID revisionId = UUID.randomUUID();
        SubmitRevisionRequest proposedChanges = new SubmitRevisionRequest(
                "Approved Festive Kurta",
                "Approved description",
                "Pure Chanderi Silk",
                "Party Wear",
                null,
                List.of(new CreateVariantDraftRequest("XL", "Wine")),
                List.of()
        );
        ProductRevisionEntity revision = new ProductRevisionEntity(
                liveProduct, managerId, objectMapper.writeValueAsString(proposedChanges)
        );
        revision.setId(revisionId);

        when(productRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(productRevisionRepository.save(any(ProductRevisionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skuGeneratorService.generateUniqueSku(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("DZF-KUR-ORIG-WIN-XL-999");
        when(productVariantRepository.existsByProductIdAndSizeAndColour(any(), anyString(), anyString()))
                .thenReturn(false);

        ReviewRevisionRequest reviewRequest = new ReviewRevisionRequest(true, null);
        ProductRevisionResponse response = revisionService.reviewRevision(revisionId, ownerId, reviewRequest);

        assertThat(response.status()).isEqualTo(RevisionStatus.APPROVED);

        // Verify live product was updated
        assertThat(liveProduct.getName()).isEqualTo("Approved Festive Kurta");
        assertThat(liveProduct.getDescription()).isEqualTo("Approved description");
        assertThat(liveProduct.getFabric()).isEqualTo("Pure Chanderi Silk");
        assertThat(liveProduct.getOccasion()).isEqualTo("Party Wear");
        assertThat(liveProduct.getVariants()).hasSize(1);
        verify(productRepository).save(liveProduct);
    }

    @Test
    @DisplayName("Reviewing an already processed revision throws InvalidOperationException")
    void reviewAlreadyProcessedRevisionThrows() {
        UUID revisionId = UUID.randomUUID();
        ProductRevisionEntity revision = new ProductRevisionEntity(liveProduct, managerId, "{}");
        revision.setId(revisionId);
        revision.setStatus(RevisionStatus.APPROVED);

        when(productRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

        ReviewRevisionRequest reviewRequest = new ReviewRevisionRequest(true, null);

        assertThatThrownBy(() -> revisionService.reviewRevision(revisionId, ownerId, reviewRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been processed");
    }
}
