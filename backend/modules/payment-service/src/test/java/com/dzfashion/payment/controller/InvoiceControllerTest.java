package com.dzfashion.payment.controller;

import com.dzfashion.payment.dto.request.GenerateDispatchInvoiceRequest;
import com.dzfashion.payment.dto.request.GenerateReceiptRequest;
import com.dzfashion.payment.dto.request.InvoiceItemData;
import com.dzfashion.payment.dto.response.InvoiceResponse;
import com.dzfashion.payment.enums.InvoiceType;
import com.dzfashion.payment.enums.Role;
import com.dzfashion.payment.security.JwtAuthenticationFilter;
import com.dzfashion.payment.security.JwtTokenService;
import com.dzfashion.payment.security.SecurityConfig;
import com.dzfashion.payment.security.UserPrincipal;
import com.dzfashion.payment.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private UsernamePasswordAuthenticationToken createAuth(UUID id, String username, Role role) {
        UserPrincipal principal = new UserPrincipal(id, username, username + "@example.com", role);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("POST /api/v1/invoices/receipt generates order placement receipt")
    void generateReceipt_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        GenerateReceiptRequest request = new GenerateReceiptRequest(
                orderId,
                "ORD-100",
                customerId,
                "Aarav Patel",
                "9876543210",
                "Ahmedabad",
                "Gujarat",
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1050.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(), UUID.randomUUID(), "Kurta", "SKU-1", "6204", 1,
                        new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("5.00")
                ))
        );

        InvoiceResponse response = new InvoiceResponse(
                UUID.randomUUID(),
                "RCP/2026-27/000001",
                orderId,
                "ORD-100",
                InvoiceType.RECEIPT,
                "D & Z Fashion",
                "24AAACD1234E1Z5",
                "Surat",
                "Gujarat",
                customerId,
                "Aarav Patel",
                "9876543210",
                "Ahmedabad",
                "Gujarat",
                new BigDecimal("1000.00"),
                new BigDecimal("25.00"),
                new BigDecimal("25.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1050.00"),
                "INR",
                Instant.now(),
                Collections.emptyList()
        );

        when(invoiceService.generateReceipt(any(GenerateReceiptRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/invoices/receipt")
                        .with(authentication(createAuth(customerId, "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("RCP/2026-27/000001"))
                .andExpect(jsonPath("$.invoiceType").value("RECEIPT"));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/dispatch generates final GST invoice for MANAGER")
    void generateDispatchInvoice_Manager_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        GenerateDispatchInvoiceRequest request = new GenerateDispatchInvoiceRequest(
                orderId,
                "ORD-100",
                UUID.randomUUID(),
                "Customer",
                "9876543210",
                "Surat",
                "Gujarat",
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2100.00"),
                List.of(new InvoiceItemData(
                        UUID.randomUUID(), UUID.randomUUID(), "Dress", "SKU-2", "6204", 1,
                        new BigDecimal("2000.00"), new BigDecimal("2000.00"), new BigDecimal("5.00")
                ))
        );

        InvoiceResponse response = new InvoiceResponse(
                UUID.randomUUID(),
                "INV/2026-27/000001",
                orderId,
                "ORD-100",
                InvoiceType.DISPATCH_TAX_INVOICE,
                "D & Z Fashion",
                "24AAACD1234E1Z5",
                "Surat",
                "Gujarat",
                UUID.randomUUID(),
                "Customer",
                "9876543210",
                "Surat",
                "Gujarat",
                new BigDecimal("2000.00"),
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2100.00"),
                "INR",
                Instant.now(),
                Collections.emptyList()
        );

        when(invoiceService.generateDispatchInvoice(any(GenerateDispatchInvoiceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/invoices/dispatch")
                        .with(authentication(createAuth(UUID.randomUUID(), "manager1", Role.MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV/2026-27/000001"))
                .andExpect(jsonPath("$.invoiceType").value("DISPATCH_TAX_INVOICE"));
    }

    @Test
    @DisplayName("POST /api/v1/invoices/dispatch forbidden for CUSTOMER")
    void generateDispatchInvoice_Customer_Forbidden() throws Exception {
        GenerateDispatchInvoiceRequest request = new GenerateDispatchInvoiceRequest(
                UUID.randomUUID(), "ORD-1", UUID.randomUUID(), "Cust", "999", "Addr", "Gujarat",
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("105"),
                List.of(new InvoiceItemData(UUID.randomUUID(), UUID.randomUUID(), "P", "S", "6204", 1,
                        new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("5")))
        );

        mockMvc.perform(post("/api/v1/invoices/dispatch")
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/invoices/order/{orderId} returns invoice list for order")
    void getInvoicesForOrder_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(invoiceService.getInvoicesForOrder(orderId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/invoices/order/" + orderId)
                        .with(authentication(createAuth(UUID.randomUUID(), "customer1", Role.CUSTOMER))))
                .andExpect(status().isOk());
    }
}
