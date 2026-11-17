import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/components/ui/Toast';
import { customerApi } from '@/lib/api/customer';
import CartPage from '@/app/(storefront)/cart/page';
import StoreCreditPage from '@/app/(storefront)/account/store-credit/page';
import ProfileAndAddressPage from '@/app/(storefront)/account/profile/page';
import { ReviewModal } from '@/components/storefront/ReviewModal';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => '/account',
  useSearchParams: () => new URLSearchParams(),
}));

// Mock customerApi
vi.mock('@/lib/api/customer', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api/customer')>('@/lib/api/customer');
  return {
    ...actual,
    customerApi: {
      ...actual.customerApi,
      getProfile: vi.fn().mockResolvedValue({
        data: {
          id: 'user-1',
          username: 'priyapatel',
          email: 'priya@example.com',
          role: 'CUSTOMER',
          fullName: 'Priya Patel',
          phoneNumber: '9876543210',
          addressLine1: 'B-402, Surat Textile Market',
          addressLine2: 'Ring Road',
          city: 'Surat',
          state: 'Gujarat',
          postalCode: '395002',
          country: 'India',
        },
        status: 200,
        ok: true,
      }),
      updateProfile: vi.fn().mockImplementation((payload) =>
        Promise.resolve({
          data: {
            id: 'user-1',
            username: 'priyapatel',
            email: 'priya@example.com',
            role: 'CUSTOMER',
            ...payload,
          },
          status: 200,
          ok: true,
        })
      ),
      getCart: vi.fn().mockResolvedValue({
        data: {
          id: 'cart-1',
          customerId: 'user-1',
          totalItems: 2,
          subtotal: 2500,
          priceConfirmationRequired: false,
          items: [
            {
              id: 'item-1',
              productId: 'prod-1',
              variantId: 'var-1',
              productName: 'Surat Zari Silk Saree',
              sku: 'SZS-RED-M',
              size: 'M',
              colour: 'Wine Red',
              quantity: 1,
              priceSnapshot: 1500,
              currentPrice: 1500,
              priceChanged: false,
              available: true,
              itemTotal: 1500,
            },
            {
              id: 'item-2',
              productId: 'prod-2',
              variantId: 'var-2',
              productName: 'Embroidered Kurta Set',
              sku: 'EKS-BLU-L',
              size: 'L',
              colour: 'Royal Blue',
              quantity: 1,
              priceSnapshot: 1000,
              currentPrice: 1000,
              priceChanged: false,
              available: true,
              itemTotal: 1000,
            },
          ],
        },
        status: 200,
        ok: true,
      }),
      updateCartItemQuantity: vi.fn().mockResolvedValue({
        data: {
          id: 'cart-1',
          totalItems: 3,
          subtotal: 4000,
          priceConfirmationRequired: false,
          items: [],
        },
        status: 200,
        ok: true,
      }),
      removeCartItem: vi.fn().mockResolvedValue({
        data: {
          id: 'cart-1',
          totalItems: 1,
          subtotal: 1000,
          priceConfirmationRequired: false,
          items: [],
        },
        status: 200,
        ok: true,
      }),
      getStoreCreditBalance: vi.fn().mockResolvedValue({
        data: {
          customerId: 'user-1',
          currentBalance: 1250,
          totalIssued: 2500,
          totalRedeemed: 1250,
          neverExpires: true,
        },
        status: 200,
        ok: true,
      }),
      getStoreCreditLedger: vi.fn().mockResolvedValue({
        data: {
          content: [
            {
              id: 'led-1',
              entryType: 'CREDIT',
              amount: 1250,
              balanceAfter: 1250,
              reason: 'Quality return credit issued',
              referenceId: 'RET-001',
              createdAt: '2026-11-16T10:00:00Z',
            },
          ],
          page: 0,
          size: 15,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
        status: 200,
        ok: true,
      }),
      getMyOrders: vi.fn().mockResolvedValue({
        data: {
          content: [
            {
              id: 'order-1',
              orderNumber: 'ORD-20261117-0001',
              orderStatus: 'CONFIRMED',
              paymentMethod: 'SIMULATED_ONLINE',
              totalItems: 1,
              totalPayable: 1500,
              createdAt: '2026-11-17T12:00:00Z',
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
        status: 200,
        ok: true,
      }),
      getOrderDetails: vi.fn().mockResolvedValue({
        data: {
          id: 'order-1',
          orderNumber: 'ORD-20261117-0001',
          customerId: 'user-1',
          recipientName: 'Priya Patel',
          recipientPhone: '9876543210',
          shippingAddressLine1: 'B-402, Surat Textile Market',
          shippingCity: 'Surat',
          shippingState: 'Gujarat',
          shippingPostalCode: '395002',
          shippingCountry: 'India',
          orderStatus: 'CONFIRMED',
          paymentMethod: 'SIMULATED_ONLINE',
          paymentStatus: 'PAID',
          subtotal: 1500,
          deliveryCharge: 0,
          discountAmount: 0,
          taxAmount: 75,
          storeCreditUsed: 0,
          totalPayable: 1500,
          createdAt: '2026-11-17T12:00:00Z',
          updatedAt: '2026-11-17T12:00:00Z',
          items: [
            {
              id: 'item-1',
              productId: 'prod-1',
              variantId: 'var-1',
              productName: 'Surat Zari Silk Saree',
              sku: 'SZS-RED-M',
              size: 'M',
              colour: 'Wine Red',
              quantity: 1,
              unitPrice: 1500,
              itemTotal: 1500,
            },
          ],
        },
        status: 200,
        ok: true,
      }),
      cancelOrder: vi.fn().mockResolvedValue({
        data: {
          id: 'order-1',
          orderNumber: 'ORD-20261117-0001',
          orderStatus: 'CANCELLED',
        },
        status: 200,
        ok: true,
      }),
      createProductReview: vi.fn().mockResolvedValue({
        data: {
          id: 'rev-1',
          productId: 'prod-1',
          customerId: 'user-1',
          customerName: 'Priya Patel',
          rating: 5,
          title: 'Gorgeous Silk',
          comment: 'Outstanding weave and fast delivery from Surat!',
          verifiedPurchase: true,
          status: 'PUBLISHED',
          photoUrls: [],
          createdAt: '2026-11-17T14:00:00Z',
        },
        status: 201,
        ok: true,
      }),
    },
  };
});

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <AuthProvider>
      <ToastProvider>{ui}</ToastProvider>
    </AuthProvider>
  );
}

describe('Component 13: Customer Flows', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Shopping Bag (Cart)', () => {
    it('prompts unauthenticated user to sign in', async () => {
      renderWithProviders(<CartPage />);

      await waitFor(() => {
        expect(screen.getByText(/Sign in to view your shopping bag/i)).toBeDefined();
      });
      expect(screen.getByRole('button', { name: /Sign In/i })).toBeDefined();
      expect(screen.getByRole('button', { name: /Create Account/i })).toBeDefined();
    });

    it('renders cart items and free delivery threshold when authenticated', async () => {
      // Setup authenticated user in localStorage
      localStorage.setItem('dz_auth_token', 'valid-jwt');
      localStorage.setItem(
        'dz_auth_user',
        JSON.stringify({
          id: 'user-1',
          username: 'priyapatel',
          email: 'priya@example.com',
          role: 'CUSTOMER',
        })
      );

      renderWithProviders(<CartPage />);

      await waitFor(() => {
        expect(screen.getByText(/Shopping Bag/i)).toBeDefined();
      });

      expect(screen.getByText('Surat Zari Silk Saree')).toBeDefined();
      expect(screen.getByText('Embroidered Kurta Set')).toBeDefined();
      // ₹2500 is > ₹2000, so free delivery is unlocked
      expect(screen.getByText(/Free Pan-India Delivery/i)).toBeDefined();
      expect(screen.getByRole('button', { name: /Proceed to Checkout/i })).toBeDefined();
    });

    it('blocks checkout when price shift requires confirmation', async () => {
      localStorage.setItem('dz_auth_token', 'valid-jwt');
      localStorage.setItem(
        'dz_auth_user',
        JSON.stringify({
          id: 'user-1',
          username: 'priyapatel',
          email: 'priya@example.com',
          role: 'CUSTOMER',
        })
      );

      // Return cart with price confirmation required
      vi.mocked(customerApi.getCart).mockResolvedValueOnce({
        data: {
          id: 'cart-1',
          customerId: 'user-1',
          totalItems: 1,
          subtotal: 1800,
          priceConfirmationRequired: true,
          items: [
            {
              id: 'item-1',
              productId: 'prod-1',
              variantId: 'var-1',
              productName: 'Surat Zari Silk Saree',
              sku: 'SZS-RED-M',
              size: 'M',
              colour: 'Wine Red',
              quantity: 1,
              priceSnapshot: 1500,
              currentPrice: 1800,
              priceChanged: true,
              available: true,
              itemTotal: 1800,
            },
          ],
        },
        status: 200,
        ok: true,
      });

      renderWithProviders(<CartPage />);

      await waitFor(() => {
        expect(screen.getByText(/Garment Price Shift Detected/i)).toBeDefined();
      });

      // Checkout button should be disabled until checkbox is ticked
      const checkoutBtn = screen.getByRole('button', { name: /Proceed to Checkout/i }) as HTMLButtonElement;
      expect(checkoutBtn.disabled).toBe(true);

      // Tick the confirmation checkbox
      const checkbox = screen.getByLabelText(/I acknowledge and accept the updated garment prices/i);
      fireEvent.click(checkbox);

      // Now checkout button should be enabled
      expect(checkoutBtn.disabled).toBe(false);
    });
  });

  describe('Store Credit Wallet', () => {
    it('displays non-expiring balance and transaction ledger', async () => {
      localStorage.setItem('dz_auth_token', 'valid-jwt');
      localStorage.setItem(
        'dz_auth_user',
        JSON.stringify({
          id: 'user-1',
          username: 'priyapatel',
          email: 'priya@example.com',
          role: 'CUSTOMER',
        })
      );

      renderWithProviders(<StoreCreditPage />);

      await waitFor(() => {
        expect(screen.getByText(/Store Credit Wallet/i)).toBeDefined();
      });

      expect(screen.getByText(/NEVER EXPIRES/i)).toBeDefined();
      expect(screen.getAllByText(/1,250\.00/)[0]).toBeDefined();
      expect(screen.getByText(/Quality return credit issued/i)).toBeDefined();
      expect(screen.getByText('+₹1,250.00')).toBeDefined();
    });
  });

  describe('Single Saved Delivery Address & Profile', () => {
    it('loads saved address and allows profile update', async () => {
      localStorage.setItem('dz_auth_token', 'valid-jwt');
      localStorage.setItem(
        'dz_auth_user',
        JSON.stringify({
          id: 'user-1',
          username: 'priyapatel',
          email: 'priya@example.com',
          role: 'CUSTOMER',
        })
      );

      renderWithProviders(<ProfileAndAddressPage />);

      await waitFor(() => {
        expect(screen.getByDisplayValue('Priya Patel')).toBeDefined();
      });

      expect(screen.getByDisplayValue('B-402, Surat Textile Market')).toBeDefined();
      expect(screen.getByDisplayValue('395002')).toBeDefined();

      // Submit update
      const saveBtn = screen.getByRole('button', { name: /Save Changes/i });
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(customerApi.updateProfile).toHaveBeenCalledWith(
          expect.objectContaining({
            fullName: 'Priya Patel',
            postalCode: '395002',
          })
        );
      });
    });
  });

  describe('Product Review Modal', () => {
    it('submits rating and comment for verified purchase', async () => {
      const handleSubmitted = vi.fn();
      render(
        <ToastProvider>
          <ReviewModal
            isOpen={true}
            onClose={vi.fn()}
            productId="prod-1"
            orderId="order-1"
            orderItemId="item-1"
            productName="Surat Zari Silk Saree"
            onReviewSubmitted={handleSubmitted}
          />
        </ToastProvider>
      );

      expect(screen.getByText(/Write a Verified Purchase Review/i)).toBeDefined();
      expect(screen.getByText(/Share your thoughts on Surat Zari Silk Saree/i)).toBeDefined();

      const textarea = screen.getByPlaceholderText(/Describe the fabric feel/i);
      fireEvent.change(textarea, { target: { value: 'Incredible texture and pure silk borders.' } });

      const submitBtn = screen.getByRole('button', { name: /Submit Review/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(customerApi.createProductReview).toHaveBeenCalledWith(
          'prod-1',
          expect.objectContaining({
            orderId: 'order-1',
            orderItemId: 'item-1',
            rating: 5,
            comment: 'Incredible texture and pure silk borders.',
          })
        );
      });
    });
  });
});
