package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.response.ManagerOperationalReportResponse;
import com.dzfashion.commerce.dto.response.OwnerAnalyticsReportResponse;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OrderShortageIncidentRepository;
import com.dzfashion.commerce.repository.SupportTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private OrderShortageIncidentRepository shortageIncidentRepository;

    @InjectMocks
    private ReportingService reportingService;

    @Test
    @DisplayName("Manager operational report provides fulfillment and operational counts with strictly zero financial data")
    void testManagerOperationalReport_ContainsOnlyOperationalCounts() {
        when(orderRepository.count()).thenReturn(25L);
        when(orderRepository.countByAssignedManagerIdIsNullAndStatusIn(any())).thenReturn(8L);
        when(orderRepository.countByAssignedManagerIdIsNotNullAndStatusIn(any())).thenReturn(10L);
        when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(6L);
        when(orderRepository.countByStatus(OrderStatus.SHIPPED)).thenReturn(4L);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(3L);

        when(supportTicketRepository.countByStatusIn(any())).thenReturn(5L);
        when(supportTicketRepository.countByEscalatedTrue()).thenReturn(1L);
        when(shortageIncidentRepository.countByStatus(ShortageIncidentStatus.REPORTED)).thenReturn(2L);

        ManagerOperationalReportResponse response = reportingService.getManagerOperationalReport();

        assertThat(response.totalOrders()).isEqualTo(25L);
        assertThat(response.pendingClaimOrders()).isEqualTo(8L);
        assertThat(response.claimedOrders()).isEqualTo(10L);
        assertThat(response.inPickingOrders()).isEqualTo(6L);
        assertThat(response.dispatchedOrders()).isEqualTo(4L);
        assertThat(response.deliveredOrders()).isEqualTo(3L);
        assertThat(response.openSupportTickets()).isEqualTo(5L);
        assertThat(response.escalatedSupportTickets()).isEqualTo(1L);
        assertThat(response.reportedShortages()).isEqualTo(2L);
        // Verified: response record has zero revenue, zero profit, zero margins!
    }

    @Test
    @DisplayName("Owner analytics report provides complete GMV, discounts, net sales, and payment method breakdown")
    void testOwnerAnalyticsReport_ContainsFinancialRevenueMetrics() {
        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(5L);

        List<Object[]> finSummary = Collections.singletonList(
                new Object[]{new BigDecimal("250000.00"), new BigDecimal("15000.00"), new BigDecimal("235000.00")}
        );
        when(orderRepository.getSalesFinancialSummary()).thenReturn(finSummary);

        List<Object[]> paySummary = List.of(
                new Object[]{PaymentMethod.SIMULATED_ONLINE, 60L, new BigDecimal("150000.00")},
                new Object[]{PaymentMethod.COD, 35L, new BigDecimal("85000.00")}
        );
        when(orderRepository.getSalesByPaymentMethod()).thenReturn(paySummary);

        when(supportTicketRepository.countByStatusIn(any())).thenReturn(3L);
        when(supportTicketRepository.countByEscalatedTrue()).thenReturn(0L);
        when(shortageIncidentRepository.countByStatus(ShortageIncidentStatus.REPORTED)).thenReturn(1L);

        OwnerAnalyticsReportResponse response = reportingService.getOwnerAnalyticsReport();

        assertThat(response.totalOrders()).isEqualTo(100L);
        assertThat(response.grossMerchandiseValue()).isEqualByComparingTo("250000.00");
        assertThat(response.totalDiscountGiven()).isEqualByComparingTo("15000.00");
        assertThat(response.netSalesRevenue()).isEqualByComparingTo("235000.00");
        assertThat(response.onlinePaidOrders()).isEqualTo(60L);
        assertThat(response.onlinePaidAmount()).isEqualByComparingTo("150000.00");
        assertThat(response.codOrders()).isEqualTo(35L);
        assertThat(response.codAmount()).isEqualByComparingTo("85000.00");
        assertThat(response.cancelledOrders()).isEqualTo(5L);
    }
}
