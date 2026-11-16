package com.dzfashion.fulfillment.service;

import com.dzfashion.fulfillment.config.FulfillmentProperties;
import com.dzfashion.fulfillment.dto.document.DailyPickingListDocument;
import com.dzfashion.fulfillment.dto.document.DispatchManifestDocument;
import com.dzfashion.fulfillment.dto.document.PackingSlipDocument;
import com.dzfashion.fulfillment.entity.DailyDispatchBatchEntity;
import com.dzfashion.fulfillment.entity.ShipmentEntity;
import com.dzfashion.fulfillment.entity.ShipmentItemEntity;
import com.dzfashion.fulfillment.entity.TransportAgencyEntity;
import com.dzfashion.fulfillment.enums.ShipmentStatus;
import com.dzfashion.fulfillment.repository.DailyDispatchBatchRepository;
import com.dzfashion.fulfillment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentDocumentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private DailyDispatchBatchRepository dailyDispatchBatchRepository;

    @Mock
    private FulfillmentProperties fulfillmentProperties;

    @InjectMocks
    private FulfillmentDocumentService fulfillmentDocumentService;

    private ShipmentEntity shipment1;
    private ShipmentEntity shipment2;

    @BeforeEach
    void setUp() {
        when(fulfillmentProperties.getWarehouseName()).thenReturn("Surat Central Warehouse");

        shipment1 = new ShipmentEntity(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), "Fatima Sheikh",
                "9876543210", "Ring Road", "Surat", "Gujarat", "395002", true, new BigDecimal("1500.00")
        );
        shipment1.addItem(new ShipmentItemEntity(UUID.randomUUID(), "SKU-KURTI-01", "Cotton Kurti", "M", "Navy", "BIN-A-01", 2));
        shipment1.addItem(new ShipmentItemEntity(UUID.randomUUID(), "SKU-DUPATTA-01", "Chiffon Dupatta", "Free", "Gold", "BIN-B-05", 1));

        shipment2 = new ShipmentEntity(
                UUID.randomUUID(), "ORD-002", UUID.randomUUID(), "Priya Sharma",
                "9123456789", "MG Road", "Ahmedabad", "Gujarat", "380001", false, BigDecimal.ZERO
        );
        shipment2.addItem(new ShipmentItemEntity(UUID.randomUUID(), "SKU-KURTI-01", "Cotton Kurti", "M", "Navy", "BIN-A-01", 3));
    }

    @Test
    @DisplayName("Should generate combined daily picking list aggregating items across orders by SKU")
    void testGenerateCombinedPickingList() {
        when(shipmentRepository.findByStatus(ShipmentStatus.PROCESSING)).thenReturn(List.of(shipment1, shipment2));

        DailyPickingListDocument doc = fulfillmentDocumentService.generateCombinedPickingList();

        assertThat(doc).isNotNull();
        assertThat(doc.warehouseName()).isEqualTo("Surat Central Warehouse");
        assertThat(doc.totalOrders()).isEqualTo(2);
        assertThat(doc.totalUnitsToPick()).isEqualTo(6); // 2 + 1 + 3

        assertThat(doc.items()).hasSize(2); // SKU-KURTI-01 and SKU-DUPATTA-01

        DailyPickingListDocument.SkuPickSummary kurtiSummary = doc.items().stream()
                .filter(i -> i.sku().equals("SKU-KURTI-01"))
                .findFirst().orElseThrow();
        assertThat(kurtiSummary.totalQuantity()).isEqualTo(5); // 2 from ord1 + 3 from ord2
        assertThat(kurtiSummary.allocations()).hasSize(2);
    }

    @Test
    @DisplayName("Should generate packing slip with masked phone number for PII privacy")
    void testGeneratePackingSlip_MaskedPhone() {
        UUID orderId = shipment1.getOrderId();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment1));

        PackingSlipDocument doc = fulfillmentDocumentService.generatePackingSlip(orderId);

        assertThat(doc).isNotNull();
        assertThat(doc.orderNumber()).isEqualTo("ORD-001");
        assertThat(doc.recipientName()).isEqualTo("Fatima Sheikh");
        assertThat(doc.maskedPhone()).isEqualTo("98765****0");
        assertThat(doc.items()).hasSize(2);
        assertThat(doc.cod()).isTrue();
        assertThat(doc.codAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("Should generate dispatch manifest with parcel list and COD totals")
    void testGenerateDispatchManifest() {
        UUID batchId = UUID.randomUUID();
        TransportAgencyEntity agency = new TransportAgencyEntity("BlueDart", "Lead", "9876500000");
        DailyDispatchBatchEntity batch = new DailyDispatchBatchEntity(
                "DISPATCH-20261116-0001",
                LocalDate.of(2026, 11, 16),
                agency,
                UUID.randomUUID(),
                2,
                "BLUEDART-MANIFEST-101",
                "Surat hub morning dispatch"
        );
        batch.setId(batchId);

        when(dailyDispatchBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(shipmentRepository.findByDailyDispatchBatchId(batchId)).thenReturn(List.of(shipment1, shipment2));

        DispatchManifestDocument doc = fulfillmentDocumentService.generateDispatchManifest(batchId);

        assertThat(doc).isNotNull();
        assertThat(doc.batchNumber()).isEqualTo("DISPATCH-20261116-0001");
        assertThat(doc.transportAgencyName()).isEqualTo("BlueDart");
        assertThat(doc.parcelCount()).isEqualTo(2);
        assertThat(doc.totalCodAmount()).isEqualByComparingTo("1500.00"); // Only shipment1 is COD
        assertThat(doc.parcels()).hasSize(2);
        assertThat(doc.parcels().get(0).destinationCity()).isEqualTo("Surat");
        assertThat(doc.parcels().get(1).destinationCity()).isEqualTo("Ahmedabad");
    }
}
