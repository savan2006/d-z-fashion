import { api } from './client';
import { ApiResponse } from './types';

// ==========================================
// DTOs & Interfaces
// ==========================================

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ActionResponse {
  success: boolean;
  message: string;
}

// Order & Shortage DTOs
export interface AdminOrderSummary {
  id: string;
  orderNumber: string;
  orderStatus: string;
  paymentMethod: string;
  paymentStatus: string;
  totalItems: number;
  totalPayable: number;
  claimedByManagerId?: string;
  claimedByManagerName?: string;
  claimedAt?: string;
  createdAt: string;
}

export interface ReassignOrderRequest {
  newManagerId?: string;
  reason: string;
}

export interface ReportShortageRequest {
  variantId: string;
  sku: string;
  missingQuantity: number;
  reason: string;
}

export interface ShortageIncidentResponse {
  id: string;
  incidentNumber: string;
  orderNumber: string;
  sku: string;
  missingQuantity: number;
  reportedBy: string;
  status: 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CANCELLED';
  resolutionNotes?: string;
  createdAt: string;
}

export interface ResolveShortageRequest {
  resolutionType: 'RESTOCKED_RESERVED' | 'CANCELLED_REFUNDED' | 'SUBSTITUTED';
  resolutionNotes: string;
}

// Catalog & Revision DTOs
export interface AdminProductSummary {
  id: string;
  title: string;
  slug: string;
  categoryName: string;
  categorySlug: string;
  status: 'DRAFT' | 'PENDING_REVISION' | 'PUBLISHED' | 'ARCHIVED';
  basePrice?: number;
  discountPrice?: number;
  fabrication?: string;
  occasion?: string;
  totalVariants: number;
  hasPendingRevision: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRevisionDiff {
  id: string;
  productId: string;
  productTitle: string;
  proposedChanges: {
    title?: string;
    description?: string;
    fabrication?: string;
    careInstructions?: string;
    occasion?: string;
  };
  liveValues: {
    title?: string;
    description?: string;
    fabrication?: string;
    careInstructions?: string;
    occasion?: string;
  };
  submittedBy: string;
  submittedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
}

export interface CreateProductDraftRequest {
  title: string;
  categoryId: string;
  description: string;
  fabrication: string;
  careInstructions?: string;
  occasion?: string;
  variants: {
    size: string;
    colour: string;
    sku: string;
  }[];
}

export interface CreateProductOwnerRequest extends CreateProductDraftRequest {
  basePrice: number;
  discountPrice?: number;
}

export interface UpdatePriceRequest {
  basePrice: number;
  discountPrice?: number;
  changeReason: string;
}

export interface UpdateVariantPriceRequest {
  priceOverride: number;
  changeReason: string;
}

// Inventory DTOs
export interface StockItemResponse {
  id: string;
  variantId: string;
  sku: string;
  productName: string;
  size: string;
  colour: string;
  physicalQty: number;
  reservedQty: number;
  availableQty: number;
  quarantinedQty: number;
  lowStockThreshold: number;
  isLowStock: boolean;
  warehouseName: string;
  updatedAt: string;
}

export interface LowStockAlert {
  id: string;
  variantId: string;
  sku: string;
  productName: string;
  availableQty: number;
  threshold: number;
  severity: 'WARNING' | 'CRITICAL';
  createdAt: string;
}

export interface StockAdjustmentRequest {
  variantId: string;
  warehouseId: string;
  quantityDelta: number;
  reason: string;
  movementType: 'MANUAL_COUNT' | 'DAMAGE_WRITEOFF' | 'WORKSHOP_RECEIPT';
}

export interface StockMovementResponse {
  id: string;
  sku: string;
  quantityChange: number;
  balanceAfter: number;
  movementType: string;
  reason: string;
  performedBy: string;
  createdAt: string;
}

// Fulfillment Documents & Dispatch
export interface PickingItem {
  orderNumber: string;
  sku: string;
  productName: string;
  size: string;
  colour: string;
  quantity: number;
  warehouseLocation: string;
}

export interface DailyPickingListDocument {
  batchDate: string;
  totalOrders: number;
  totalUnits: number;
  items: PickingItem[];
}

export interface PackingSlipDocument {
  orderNumber: string;
  recipientName: string;
  maskedPhone: string;
  shippingAddress: string;
  items: {
    sku: string;
    productName: string;
    size: string;
    colour: string;
    quantity: number;
  }[];
  orderDate: string;
}

export interface DispatchManifestDocument {
  batchNumber: string;
  dispatchDate: string;
  agencyName: string;
  totalParcels: number;
  totalCodAmount: number;
  orders: {
    orderNumber: string;
    trackingNumber: string;
    recipientName: string;
    city: string;
    paymentMethod: string;
    collectibleCodAmount: number;
  }[];
}

export interface CreateDispatchBatchRequest {
  agencyId: string;
  orderIds: string[];
  manifestNotes?: string;
}

export interface DailyDispatchBatchResponse {
  id: string;
  batchNumber: string;
  agencyName: string;
  totalShipments: number;
  status: 'PREPARED' | 'HANDED_OVER' | 'IN_TRANSIT' | 'COMPLETED';
  handedOverAt?: string;
  createdAt: string;
}

export interface RecordDeliveryAttemptRequest {
  attemptNumber: 1 | 2;
  failureReason: string;
  attemptDate: string;
  courierNotes?: string;
}

// Returns & Inspections
export interface ReturnItemStaffResponse {
  id: string;
  returnNumber: string;
  orderNumber: string;
  customerId: string;
  customerName?: string;
  sku: string;
  productName: string;
  quantity: number;
  originalUnitPrice: number;
  reason: string;
  resolutionType: 'EXCHANGE' | 'STORE_CREDIT_REFUND' | 'ORIGINAL_PAYMENT_REFUND';
  status: string;
  photoUrls: string[];
  customerNotes?: string;
  inspectionPassed?: boolean;
  inspectionNotes?: string;
  inspectorName?: string;
  refundApproved?: boolean;
  approvedByOwnerName?: string;
  refundDeadlineDate?: string;
  createdAt: string;
}

export interface PerformInspectionRequest {
  passed: boolean;
  inspectionNotes: string;
  photoUrls?: string[];
}

// COD Settlements
export interface CodSettlementBatchResponse {
  id: string;
  batchNumber: string;
  agencyName: string;
  agencyInvoiceReference: string;
  totalOrders: number;
  totalExpectedAmount: number;
  totalCollectedAmount: number;
  totalRemittedAmount: number;
  discrepancyAmount: number;
  hasDiscrepancy: boolean;
  status: 'PREPARED' | 'SUBMITTED_FOR_APPROVAL' | 'APPROVED' | 'REJECTED';
  preparedByManagerName?: string;
  approvedByOwnerName?: string;
  notes?: string;
  createdAt: string;
  approvedAt?: string;
}

export interface CreateCodSettlementBatchRequest {
  agencyName: string;
  agencyInvoiceReference: string;
  orderNumbers: string[];
  totalExpectedAmount: number;
  totalCollectedAmount: number;
  totalRemittedAmount: number;
  notes?: string;
}

// Promotions
export type PromotionType =
  | 'FLAT_AMOUNT'
  | 'PERCENTAGE'
  | 'BUY_X_GET_Y'
  | 'FREE_DELIVERY'
  | 'BUNDLE_DISCOUNT';

export interface PromotionResponse {
  id: string;
  code: string;
  name: string;
  description?: string;
  promotionType: PromotionType;
  discountValue: number;
  minimumSpend: number;
  maximumDiscountCap?: number;
  totalUsageLimit?: number;
  perCustomerLimit?: number;
  currentUsageCount: number;
  startDate: string;
  endDate: string;
  active: boolean;
  eligibleCategoryIds?: string[];
  createdAt: string;
}

export interface CreatePromotionRequest {
  code: string;
  name: string;
  description?: string;
  promotionType: PromotionType;
  discountValue: number;
  minimumSpend: number;
  maximumDiscountCap?: number;
  totalUsageLimit?: number;
  perCustomerLimit?: number;
  startDate: string;
  endDate: string;
  eligibleCategoryIds?: string[];
}

// Staff / Managers
export interface ManagerStaffResponse {
  id: string;
  username: string;
  email: string;
  fullName: string;
  active: boolean;
  role: 'MANAGER';
  createdAt: string;
}

export interface CreateManagerRequest {
  username: string;
  email: string;
  fullName: string;
  password: string;
}

// Support Tickets
export interface SupportTicketResponse {
  id: string;
  ticketNumber: string;
  customerId?: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  channel: 'WEB' | 'WHATSAPP' | 'PHONE';
  subject: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED' | 'CLOSED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  escalated: boolean;
  assignedToStaffName?: string;
  notes: {
    id: string;
    authorName: string;
    isInternalStaffNote: boolean;
    content: string;
    createdAt: string;
  }[];
  createdAt: string;
}

export interface ManualSupportIntakeRequest {
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  channel: 'WHATSAPP' | 'PHONE';
  subject: string;
  description: string;
  orderNumber?: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

// Reports
export interface ManagerOperationalReportResponse {
  totalOrders: number;
  pendingClaimOrders: number;
  claimedOrders: number;
  inPickingOrders: number;
  dispatchedOrders: number;
  deliveredOrders: number;
  openSupportTickets: number;
  escalatedSupportTickets: number;
  reportedShortages: number;
}

export interface OwnerAnalyticsReportResponse {
  totalOrders: number;
  grossMerchandiseValue: number;
  totalDiscountGiven: number;
  netSalesRevenue: number;
  onlinePaidOrders: number;
  onlinePaidAmount: number;
  codOrders: number;
  codAmount: number;
  cancelledOrders: number;
  openSupportTickets: number;
  escalatedSupportTickets: number;
  reportedShortages: number;
}

// Audit Logs
export interface AuditLogResponse {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actorId: string;
  actorUsername?: string;
  actorRole: string;
  details: string;
  ipAddress?: string;
  createdAt: string;
}

// ==========================================
// Admin & Manager API Client
// ==========================================

export const adminApi = {
  // Orders & Claim Queue
  getOrderQueue: (page = 0, size = 20): Promise<ApiResponse<PageResponse<AdminOrderSummary>>> =>
    api.get(`/api/v1/admin/orders/queue?page=${page}&size=${size}`),

  claimOrder: (orderNumber: string): Promise<ApiResponse<AdminOrderSummary>> =>
    api.post(`/api/v1/admin/orders/${encodeURIComponent(orderNumber)}/claim`),

  reassignOrder: (orderNumber: string, data: ReassignOrderRequest): Promise<ApiResponse<AdminOrderSummary>> =>
    api.post(`/api/v1/admin/orders/${encodeURIComponent(orderNumber)}/reassign`, data),

  dispatchOrder: (orderNumber: string): Promise<ApiResponse<AdminOrderSummary>> =>
    api.post(`/api/v1/admin/orders/${encodeURIComponent(orderNumber)}/dispatch`),

  deliverOrder: (orderNumber: string): Promise<ApiResponse<AdminOrderSummary>> =>
    api.post(`/api/v1/admin/orders/${encodeURIComponent(orderNumber)}/deliver`),

  reportShortage: (orderNumber: string, data: ReportShortageRequest): Promise<ApiResponse<ShortageIncidentResponse>> =>
    api.post(`/api/v1/admin/orders/${encodeURIComponent(orderNumber)}/shortage`, data),

  listShortages: (status?: string): Promise<ApiResponse<ShortageIncidentResponse[]>> =>
    api.get(`/api/v1/admin/orders/shortages${status ? `?status=${status}` : ''}`),

  resolveShortage: (incidentNumber: string, data: ResolveShortageRequest): Promise<ApiResponse<ShortageIncidentResponse>> =>
    api.post(`/api/v1/admin/orders/shortages/${encodeURIComponent(incidentNumber)}/resolve`, data),

  // Catalog & Product Approvals
  getAdminProductById: (id: string): Promise<ApiResponse<AdminProductSummary>> =>
    api.get(`/api/v1/admin/products/${id}`),

  createProductOwner: (data: CreateProductOwnerRequest): Promise<ApiResponse<AdminProductSummary>> =>
    api.post('/api/v1/admin/products', data),

  createProductDraft: (data: CreateProductDraftRequest): Promise<ApiResponse<AdminProductSummary>> =>
    api.post('/api/v1/products/draft', data),

  submitRevision: (id: string, proposedChanges: Record<string, unknown>): Promise<ApiResponse<ProductRevisionDiff>> =>
    api.post(`/api/v1/products/${id}/revisions`, { proposedChanges }),

  getPendingRevisions: (): Promise<ApiResponse<ProductRevisionDiff[]>> =>
    api.get('/api/v1/admin/products/revisions/pending'),

  reviewRevision: (
    revisionId: string,
    status: 'APPROVED' | 'REJECTED',
    rejectionReason?: string
  ): Promise<ApiResponse<ProductRevisionDiff>> =>
    api.post(`/api/v1/admin/products/revisions/${revisionId}/review`, { status, rejectionReason }),

  updateProductPrice: (id: string, data: UpdatePriceRequest): Promise<ApiResponse<AdminProductSummary>> =>
    api.put(`/api/v1/admin/products/${id}/price`, data),

  updateVariantPrice: (
    id: string,
    variantId: string,
    data: UpdateVariantPriceRequest
  ): Promise<ApiResponse<AdminProductSummary>> =>
    api.put(`/api/v1/admin/products/${id}/variants/${variantId}/price`, data),

  publishProduct: (id: string): Promise<ApiResponse<AdminProductSummary>> =>
    api.post(`/api/v1/admin/products/${id}/publish`),

  archiveProduct: (id: string): Promise<ApiResponse<ActionResponse>> =>
    api.post(`/api/v1/admin/products/${id}/archive`),

  // Categories & GST Tax
  getCategoryTree: (includeInactive = true): Promise<ApiResponse<Record<string, unknown>[]>> =>
    api.get(`/api/v1/categories?includeInactive=${includeInactive}`),

  createCategory: (data: { name: string; slug: string; description?: string; hsnCode?: string; gstRatePercent?: number }): Promise<ApiResponse<Record<string, unknown>>> =>
    api.post('/api/v1/categories', data),

  updateCategory: (id: string, data: Record<string, unknown>): Promise<ApiResponse<Record<string, unknown>>> =>
    api.put(`/api/v1/categories/${id}`, data),

  deleteCategory: (id: string): Promise<ApiResponse<ActionResponse>> =>
    api.delete(`/api/v1/categories/${id}`),

  // Inventory & Stock Adjustments
  getLowStockQueue: (page = 0, size = 20): Promise<ApiResponse<PageResponse<StockItemResponse>>> =>
    api.get(`/api/v1/admin/inventory/low-stock?page=${page}&size=${size}`),

  getActiveAlerts: (): Promise<ApiResponse<LowStockAlert[]>> =>
    api.get('/api/v1/admin/inventory/alerts'),

  adjustStock: (data: StockAdjustmentRequest): Promise<ApiResponse<StockItemResponse>> =>
    api.post('/api/v1/admin/inventory/adjust', data),

  addManufacturedStock: (data: { variantId: string; warehouseId: string; quantity: number; batchNumber: string }): Promise<ApiResponse<StockItemResponse>> =>
    api.post('/api/v1/admin/inventory/manufacture', data),

  updateThreshold: (variantId: string, lowStockThreshold: number): Promise<ApiResponse<StockItemResponse>> =>
    api.put(`/api/v1/admin/inventory/variants/${variantId}/threshold`, { lowStockThreshold }),

  getAllStockMovements: (page = 0, size = 20): Promise<ApiResponse<PageResponse<StockMovementResponse>>> =>
    api.get(`/api/v1/admin/inventory/movements?page=${page}&size=${size}`),

  // Fulfillment Documents & Dispatch Operations
  getCombinedPickingList: (): Promise<ApiResponse<DailyPickingListDocument>> =>
    api.get('/api/v1/fulfillment/documents/picking-list'),

  getPackingSlip: (orderId: string): Promise<ApiResponse<PackingSlipDocument>> =>
    api.get(`/api/v1/fulfillment/documents/packing-slip/${orderId}`),

  getDispatchManifest: (batchId: string): Promise<ApiResponse<DispatchManifestDocument>> =>
    api.get(`/api/v1/fulfillment/documents/dispatch-manifest/${batchId}`),

  createDispatchBatch: (data: CreateDispatchBatchRequest): Promise<ApiResponse<DailyDispatchBatchResponse>> =>
    api.post('/api/v1/fulfillment/dispatch/batches', data),

  recordAgencyHandover: (batchId: string, data: { vehicleNumber?: string; driverName?: string; agentSignatureRef?: string }): Promise<ApiResponse<DailyDispatchBatchResponse>> =>
    api.post(`/api/v1/fulfillment/dispatch/batches/${batchId}/handover`, data),

  recordDeliveryAttempt: (shipmentId: string, data: RecordDeliveryAttemptRequest): Promise<ApiResponse<Record<string, unknown>>> =>
    api.post(`/api/v1/fulfillment/shipments/${shipmentId}/attempts`, data),

  recordDeliverySuccess: (shipmentId: string, data: { receiverName: string; signatureConfirmed: boolean }): Promise<ApiResponse<Record<string, unknown>>> =>
    api.post(`/api/v1/fulfillment/shipments/${shipmentId}/delivery-success`, data),

  receiveReturnAtWarehouse: (shipmentId: string, data: { intakeReason: string; conditionSummary: string }): Promise<ApiResponse<Record<string, unknown>>> =>
    api.post(`/api/v1/fulfillment/shipments/${shipmentId}/warehouse-return`, data),

  // Returns & Quality Inspections
  getAllReturns: (page = 0, size = 20): Promise<ApiResponse<PageResponse<ReturnItemStaffResponse>>> =>
    api.get(`/api/v1/fulfillment/returns?page=${page}&size=${size}`),

  getReturnById: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.get(`/api/v1/fulfillment/returns/${id}`),

  schedulePickup: (id: string, data: { courierAgency: string; scheduledPickupDate: string }): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/schedule-pickup`, data),

  recordPickup: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/record-pickup`),

  recordReturnArrival: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/record-arrival`),

  performInspection: (id: string, data: PerformInspectionRequest): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/inspect`, data),

  confirmExchangePayment: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/confirm-exchange-payment`),

  dispatchExchangeReplacement: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/dispatch-exchange`),

  requestRefund: (id: string, recommendationNotes?: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/request-refund${recommendationNotes ? `?recommendationNotes=${encodeURIComponent(recommendationNotes)}` : ''}`),

  approveRefund: (id: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/approve-refund`),

  rejectRefund: (id: string, rejectionReason: string): Promise<ApiResponse<ReturnItemStaffResponse>> =>
    api.post(`/api/v1/fulfillment/returns/${id}/reject-refund`, { rejectionReason }),

  // COD Settlements
  getAllCodSettlementBatches: (page = 0, size = 20): Promise<ApiResponse<PageResponse<CodSettlementBatchResponse>>> =>
    api.get(`/api/v1/admin/cod-settlements?page=${page}&size=${size}`),

  createCodSettlementBatch: (data: CreateCodSettlementBatchRequest): Promise<ApiResponse<CodSettlementBatchResponse>> =>
    api.post('/api/v1/admin/cod-settlements', data),

  approveCodSettlementBatch: (batchId: string): Promise<ApiResponse<CodSettlementBatchResponse>> =>
    api.put(`/api/v1/admin/cod-settlements/${batchId}/approve`),

  // Promotions & Campaigns (Owner only)
  getAllPromotions: (page = 0, size = 20): Promise<ApiResponse<PageResponse<PromotionResponse>>> =>
    api.get(`/api/v1/admin/promotions?page=${page}&size=${size}`),

  createPromotion: (data: CreatePromotionRequest): Promise<ApiResponse<PromotionResponse>> =>
    api.post('/api/v1/admin/promotions', data),

  deactivatePromotion: (id: string): Promise<ApiResponse<ActionResponse>> =>
    api.delete(`/api/v1/admin/promotions/${id}`),

  // Staff Management (Owner only)
  listManagers: (): Promise<ApiResponse<ManagerStaffResponse[]>> =>
    api.get('/api/v1/admin/managers'),

  createManager: (data: CreateManagerRequest): Promise<ApiResponse<ManagerStaffResponse>> =>
    api.post('/api/v1/admin/managers', data),

  resetManagerPassword: (id: string, newPassword: string): Promise<ApiResponse<ActionResponse>> =>
    api.post(`/api/v1/admin/managers/${id}/reset-password?newPassword=${encodeURIComponent(newPassword)}`),

  // Support Tickets
  getAllSupportTickets: (
    status?: string,
    channel?: string,
    escalated?: boolean,
    page = 0,
    size = 20
  ): Promise<ApiResponse<PageResponse<SupportTicketResponse>>> => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) params.append('status', status);
    if (channel) params.append('channel', channel);
    if (escalated !== undefined) params.append('escalated', String(escalated));
    return api.get(`/api/v1/admin/support/tickets?${params.toString()}`);
  },

  getSupportTicketById: (id: string): Promise<ApiResponse<SupportTicketResponse>> =>
    api.get(`/api/v1/admin/support/tickets/${id}`),

  addSupportStaffMessage: (
    id: string,
    content: string,
    isInternalStaffNote: boolean
  ): Promise<ApiResponse<SupportTicketResponse>> =>
    api.post(`/api/v1/admin/support/tickets/${id}/messages`, { content, isInternalStaffNote }),

  recordManualSupportIntake: (data: ManualSupportIntakeRequest): Promise<ApiResponse<SupportTicketResponse>> =>
    api.post('/api/v1/admin/support/manual-intake', data),

  updateSupportTicketStatus: (
    id: string,
    status: 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED' | 'CLOSED'
  ): Promise<ApiResponse<SupportTicketResponse>> =>
    api.patch(`/api/v1/admin/support/tickets/${id}/status`, { status }),

  escalateSupportTicket: (id: string, reason: string): Promise<ApiResponse<SupportTicketResponse>> =>
    api.post(`/api/v1/admin/support/tickets/${id}/escalate`, { escalationReason: reason }),

  // Audit Logs (Owner only)
  getAuditLogs: (
    entityType?: string,
    action?: string,
    page = 0,
    size = 25
  ): Promise<ApiResponse<PageResponse<AuditLogResponse>>> => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (entityType) params.append('entityType', entityType);
    if (action) params.append('action', action);
    return api.get(`/api/v1/admin/audit-logs?${params.toString()}`);
  },

  // Operational & Analytics Reports
  getOperationalReport: (): Promise<ApiResponse<ManagerOperationalReportResponse>> =>
    api.get('/api/v1/admin/reports/operational'),

  getAnalyticsReport: (): Promise<ApiResponse<OwnerAnalyticsReportResponse>> =>
    api.get('/api/v1/admin/reports/analytics'),
};
