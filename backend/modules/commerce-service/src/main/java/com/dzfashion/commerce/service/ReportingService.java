package com.dzfashion.commerce.service;

import com.dzfashion.commerce.dto.response.ManagerOperationalReportResponse;
import com.dzfashion.commerce.dto.response.OwnerAnalyticsReportResponse;
import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.ShortageIncidentStatus;
import com.dzfashion.commerce.enums.SupportTicketStatus;
import com.dzfashion.commerce.repository.OrderRepository;
import com.dzfashion.commerce.repository.OrderShortageIncidentRepository;
import com.dzfashion.commerce.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final OrderRepository orderRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final OrderShortageIncidentRepository shortageIncidentRepository;

    public ReportingService(OrderRepository orderRepository,
                            SupportTicketRepository supportTicketRepository,
                            OrderShortageIncidentRepository shortageIncidentRepository) {
        this.orderRepository = orderRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.shortageIncidentRepository = shortageIncidentRepository;
    }

    public ManagerOperationalReportResponse getManagerOperationalReport() {
        long totalOrders = orderRepository.count();
        long pendingClaim = orderRepository.countByAssignedManagerIdIsNullAndStatusIn(
                List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING)
        );
        long claimed = orderRepository.countByAssignedManagerIdIsNotNullAndStatusIn(
                List.of(OrderStatus.PROCESSING)
        );
        long inPicking = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long dispatched = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);

        long openTickets = supportTicketRepository.countByStatusIn(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.WAITING_ON_CUSTOMER)
        );
        long escalatedTickets = supportTicketRepository.countByEscalatedTrue();
        long reportedShortages = shortageIncidentRepository.countByStatus(ShortageIncidentStatus.REPORTED);

        return new ManagerOperationalReportResponse(
                totalOrders,
                pendingClaim,
                claimed,
                inPicking,
                dispatched,
                delivered,
                openTickets,
                escalatedTickets,
                reportedShortages
        );
    }

    public OwnerAnalyticsReportResponse getOwnerAnalyticsReport() {
        long totalOrders = orderRepository.count();
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        BigDecimal gmv = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;

        List<Object[]> financialRows = orderRepository.getSalesFinancialSummary();
        if (financialRows != null && !financialRows.isEmpty() && financialRows.get(0) != null) {
            Object[] row = financialRows.get(0);
            if (row.length > 0 && row[0] instanceof BigDecimal val) {
                gmv = val;
            } else if (row.length > 0 && row[0] instanceof Number val) {
                gmv = BigDecimal.valueOf(val.doubleValue());
            }

            if (row.length > 1 && row[1] instanceof BigDecimal val) {
                discounts = val;
            } else if (row.length > 1 && row[1] instanceof Number val) {
                discounts = BigDecimal.valueOf(val.doubleValue());
            }

            if (row.length > 2 && row[2] instanceof BigDecimal val) {
                netRevenue = val;
            } else if (row.length > 2 && row[2] instanceof Number val) {
                netRevenue = BigDecimal.valueOf(val.doubleValue());
            }
        }

        long onlineCount = 0;
        BigDecimal onlineAmount = BigDecimal.ZERO;
        long codCount = 0;
        BigDecimal codAmount = BigDecimal.ZERO;

        List<Object[]> paymentRows = orderRepository.getSalesByPaymentMethod();
        if (paymentRows != null) {
            for (Object[] prow : paymentRows) {
                if (prow.length >= 3 && prow[0] instanceof PaymentMethod method) {
                    long cnt = prow[1] instanceof Number n ? n.longValue() : 0;
                    BigDecimal amt = prow[2] instanceof BigDecimal b ? b : (prow[2] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
                    if (method == PaymentMethod.SIMULATED_ONLINE) {
                        onlineCount = cnt;
                        onlineAmount = amt;
                    } else if (method == PaymentMethod.COD) {
                        codCount = cnt;
                        codAmount = amt;
                    }
                }
            }
        }

        long openTickets = supportTicketRepository.countByStatusIn(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.WAITING_ON_CUSTOMER)
        );
        long escalatedTickets = supportTicketRepository.countByEscalatedTrue();
        long reportedShortages = shortageIncidentRepository.countByStatus(ShortageIncidentStatus.REPORTED);

        return new OwnerAnalyticsReportResponse(
                totalOrders,
                gmv,
                discounts,
                netRevenue,
                onlineCount,
                onlineAmount,
                codCount,
                codAmount,
                cancelledOrders,
                openTickets,
                escalatedTickets,
                reportedShortages
        );
    }
}
