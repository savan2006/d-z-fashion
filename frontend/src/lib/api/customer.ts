import { api } from './client';
import { ApiResponse } from './types';

// Auth DTOs
export interface OtpInitiateResponse {
  email: string;
  expiresInSeconds: number;
  message: string;
}

export interface VerifyOtpResponse {
  email: string;
  otpToken: string;
  message: string;
}

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  role: 'OWNER' | 'MANAGER' | 'CUSTOMER';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface ActionResponse {
  success: boolean;
  message: string;
}

// User Profile DTOs
export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role: 'OWNER' | 'MANAGER' | 'CUSTOMER';
  fullName: string;
  phoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  phoneNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

// Cart DTOs
export interface CartItem {
  id: string;
  productId: string;
  variantId: string;
  productName: string;
  sku: string;
  size: string;
  colour: string;
  quantity: number;
  priceSnapshot: number;
  currentPrice: number;
  priceChanged: boolean;
  available: boolean;
  itemTotal: number;
}

export interface CartResponse {
  id: string;
  customerId: string;
  items: CartItem[];
  totalItems: number;
  subtotal: number;
  priceConfirmationRequired: boolean;
}

// Checkout DTOs
export type PaymentMethod = 'SIMULATED_ONLINE' | 'CASH_ON_DELIVERY';

export interface CheckoutPreviewRequest {
  paymentMethod: PaymentMethod;
  couponCode?: string;
  storeCreditToUse?: number;
  postalCode?: string;
}

export interface CheckoutPreviewResponse {
  subtotalBeforeDiscountAndTax: number;
  totalPhysicalUnits: number;
  freeDeliveryEligible: boolean;
  deliveryCharge: number;
  promotionalDiscountAmount: number;
  appliedCouponCode?: string;
  gstAmount: number;
  gstRatePercent: number;
  storeCreditUsed: number;
  finalPayableAmount: number;
  codServiceable: boolean;
  priceConfirmationRequired: boolean;
  warnings?: string[];
}

export interface ConfirmOrderRequest {
  paymentMethod: PaymentMethod;
  couponCode?: string;
  storeCreditToUse?: number;
  confirmPriceChanges: boolean;
  recipientName: string;
  recipientPhone: string;
  shippingAddressLine1: string;
  shippingAddressLine2?: string;
  shippingCity: string;
  shippingState: string;
  shippingPostalCode: string;
  shippingCountry?: string;
}

export type OrderStatus =
  | 'PENDING_APPROVAL'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'DISPATCHED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED';

export interface OrderItem {
  id: string;
  productId: string;
  variantId: string;
  productName: string;
  sku: string;
  size: string;
  colour: string;
  quantity: number;
  unitPrice: number;
  itemTotal: number;
}

export interface OrderResponse {
  id: string;
  orderNumber: string;
  customerId: string;
  recipientName: string;
  recipientPhone: string;
  shippingAddressLine1: string;
  shippingAddressLine2?: string;
  shippingCity: string;
  shippingState: string;
  shippingPostalCode: string;
  shippingCountry: string;
  items: OrderItem[];
  paymentMethod: PaymentMethod;
  paymentStatus: string;
  orderStatus: OrderStatus;
  subtotal: number;
  deliveryCharge: number;
  discountAmount: number;
  taxAmount: number;
  storeCreditUsed: number;
  totalPayable: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  orderStatus: OrderStatus;
  paymentMethod: PaymentMethod;
  totalItems: number;
  totalPayable: number;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// Return DTOs
export type ReturnReason =
  | 'DEFECTIVE_DAMAGED'
  | 'WRONG_ITEM_SENT'
  | 'QUALITY_UNSATISFACTORY'
  | 'SIZE_FIT_ISSUE';

export type ReturnResolutionType =
  | 'EXCHANGE'
  | 'STORE_CREDIT_REFUND'
  | 'ORIGINAL_PAYMENT_REFUND';

export interface CreateReturnRequest {
  orderId: string;
  orderNumber: string;
  orderItemId: string;
  sku: string;
  productName: string;
  quantity: number;
  originalUnitPrice: number;
  reason: ReturnReason;
  resolutionType: ReturnResolutionType;
  photoUrls: string[];
  customerNotes?: string;
}

export interface ReturnEligibilityResponse {
  orderId: string;
  orderNumber: string;
  eligible: boolean;
  deliveredAt?: string;
  daysRemaining?: number;
  rejectionReason?: string;
}

export interface ReturnRequestResponse {
  id: string;
  returnNumber: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  sku: string;
  productName: string;
  quantity: number;
  originalUnitPrice: number;
  reason: ReturnReason;
  resolutionType: ReturnResolutionType;
  status: string;
  photoUrls: string[];
  customerNotes?: string;
  createdAt: string;
}

// Store Credit DTOs
export interface StoreCreditBalanceResponse {
  customerId: string;
  currentBalance: number;
  totalIssued: number;
  totalRedeemed: number;
  lastActivityAt?: string;
  neverExpires: boolean;
}

export interface StoreCreditEntryResponse {
  id: string;
  entryType: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceAfter: number;
  reason: string;
  referenceId?: string;
  createdAt: string;
}

// Notification DTOs
export interface NotificationResponse {
  id: string;
  recipientId: string;
  title: string;
  content: string;
  type: string;
  read: boolean;
  actionUrl?: string;
  createdAt: string;
}

export interface UnreadNotificationCountResponse {
  unreadCount: number;
}

// Review DTOs
export interface CreateProductReviewRequest {
  orderId: string;
  orderItemId: string;
  rating: number;
  title?: string;
  comment: string;
  photoUrls?: string[];
}

export interface ProductReviewResponse {
  id: string;
  productId: string;
  customerId: string;
  customerName: string;
  rating: number;
  title?: string;
  comment: string;
  photoUrls: string[];
  verifiedPurchase: boolean;
  status: string;
  createdAt: string;
}

export interface ProductReviewSummaryResponse {
  productId: string;
  averageRating: number;
  totalReviews: number;
  starDistribution: Record<number, number>;
}

// API methods
export const customerApi = {
  // Auth
  initiateSignup: (email: string, fullName: string): Promise<ApiResponse<OtpInitiateResponse>> =>
    api.post('/api/v1/auth/signup/initiate', { email, fullName }),

  verifySignupOtp: (email: string, otp: string): Promise<ApiResponse<VerifyOtpResponse>> =>
    api.post('/api/v1/auth/signup/verify-otp', { email, otp }),

  completeSignup: (
    otpToken: string,
    password: string,
    confirmPassword: string,
    username?: string
  ): Promise<ApiResponse<AuthResponse>> => {
    const url = username
      ? `/api/v1/auth/signup/complete?username=${encodeURIComponent(username)}`
      : '/api/v1/auth/signup/complete';
    return api.post(url, { otpToken, password, confirmPassword });
  },

  login: (identifier: string, password: string): Promise<ApiResponse<AuthResponse>> =>
    api.post('/api/v1/auth/login', { identifier, password }),

  initiateForgotPassword: (email: string): Promise<ApiResponse<ActionResponse>> =>
    api.post('/api/v1/auth/forgot-password/initiate', { email }),

  verifyForgotPasswordOtp: (email: string, otp: string): Promise<ApiResponse<VerifyOtpResponse>> =>
    api.post('/api/v1/auth/forgot-password/verify-otp', { email, otp }),

  completeForgotPassword: (
    otpToken: string,
    newPassword: string,
    confirmPassword: string
  ): Promise<ApiResponse<ActionResponse>> =>
    api.post('/api/v1/auth/forgot-password/complete', { otpToken, newPassword, confirmPassword }),

  // User Profile
  getProfile: (): Promise<ApiResponse<UserProfile>> =>
    api.get('/api/v1/users/me'),

  updateProfile: (data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> =>
    api.put('/api/v1/users/me/profile', data),

  // Cart
  getCart: (): Promise<ApiResponse<CartResponse>> =>
    api.get('/api/v1/cart'),

  addItemToCart: (variantId: string, quantity = 1): Promise<ApiResponse<CartResponse>> =>
    api.post('/api/v1/cart/items', { variantId, quantity }),

  updateCartItemQuantity: (itemId: string, quantity: number): Promise<ApiResponse<CartResponse>> =>
    api.put(`/api/v1/cart/items/${itemId}`, { quantity }),

  removeCartItem: (itemId: string): Promise<ApiResponse<CartResponse>> =>
    api.delete(`/api/v1/cart/items/${itemId}`),

  clearCart: (): Promise<ApiResponse<ActionResponse>> =>
    api.delete('/api/v1/cart'),

  // Checkout
  checkCodPincode: (pincode: string): Promise<ApiResponse<{ pincode: string; serviceable: boolean }>> =>
    api.get(`/api/v1/cod-pincodes/check/${encodeURIComponent(pincode)}`),

  previewCheckout: (data: CheckoutPreviewRequest): Promise<ApiResponse<CheckoutPreviewResponse>> =>
    api.post('/api/v1/checkout/preview', data),

  confirmOrder: (
    data: ConfirmOrderRequest,
    idempotencyKey?: string
  ): Promise<ApiResponse<OrderResponse>> =>
    api.post('/api/v1/checkout/confirm', data, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
    }),

  // Orders
  getMyOrders: (page = 0, size = 10): Promise<ApiResponse<PageResponse<OrderSummary>>> =>
    api.get(`/api/v1/orders?page=${page}&size=${size}`),

  getOrderDetails: (orderNumber: string): Promise<ApiResponse<OrderResponse>> =>
    api.get(`/api/v1/orders/${encodeURIComponent(orderNumber)}`),

  cancelOrder: (orderNumber: string, reason: string): Promise<ApiResponse<OrderResponse>> =>
    api.post(`/api/v1/orders/${encodeURIComponent(orderNumber)}/cancel`, { reason }),

  // Returns
  checkReturnEligibility: (orderId: string): Promise<ApiResponse<ReturnEligibilityResponse>> =>
    api.get(`/api/v1/shipments/returns/eligibility/${orderId}`),

  createReturnRequest: (data: CreateReturnRequest): Promise<ApiResponse<ReturnRequestResponse>> =>
    api.post('/api/v1/shipments/returns', data),

  getMyReturns: (page = 0, size = 10): Promise<ApiResponse<PageResponse<ReturnRequestResponse>>> =>
    api.get(`/api/v1/shipments/returns/my-returns?page=${page}&size=${size}`),

  getReturnById: (id: string): Promise<ApiResponse<ReturnRequestResponse>> =>
    api.get(`/api/v1/shipments/returns/${id}`),

  // Store Credit
  getStoreCreditBalance: (): Promise<ApiResponse<StoreCreditBalanceResponse>> =>
    api.get('/api/v1/store-credit/my-balance'),

  getStoreCreditLedger: (page = 0, size = 20): Promise<ApiResponse<PageResponse<StoreCreditEntryResponse>>> =>
    api.get(`/api/v1/store-credit/my-ledger?page=${page}&size=${size}`),

  // Notifications
  getNotifications: (page = 0, size = 15): Promise<ApiResponse<PageResponse<NotificationResponse>>> =>
    api.get(`/api/v1/notifications?page=${page}&size=${size}`),

  getUnreadNotificationCount: (): Promise<ApiResponse<UnreadNotificationCountResponse>> =>
    api.get('/api/v1/notifications/unread-count'),

  markNotificationAsRead: (id: string): Promise<ApiResponse<NotificationResponse>> =>
    api.patch(`/api/v1/notifications/${id}/read`),

  markAllNotificationsAsRead: (): Promise<ApiResponse<ActionResponse>> =>
    api.patch('/api/v1/notifications/read-all'),

  // Product Reviews
  getProductReviews: (
    productId: string,
    page = 0,
    size = 10
  ): Promise<ApiResponse<PageResponse<ProductReviewResponse>>> =>
    api.get(`/api/v1/products/${productId}/reviews?page=${page}&size=${size}`),

  getProductReviewSummary: (productId: string): Promise<ApiResponse<ProductReviewSummaryResponse>> =>
    api.get(`/api/v1/products/${productId}/reviews/summary`),

  createProductReview: (
    productId: string,
    data: CreateProductReviewRequest
  ): Promise<ApiResponse<ProductReviewResponse>> =>
    api.post(`/api/v1/products/${productId}/reviews`, data),
};
