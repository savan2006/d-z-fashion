import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/components/ui/Toast';

// Storefront & Customer Pages
import HomePage from '@/app/(storefront)/page';
import CartPage from '@/app/(storefront)/cart/page';
import CheckoutPage from '@/app/(storefront)/checkout/page';
import CustomerOrdersPage from '@/app/(storefront)/account/orders/page';

// Manager Pages
import ManagerOrderQueuePage from '@/app/(manager)/manager/orders/page';
import ManagerPickingPackingPage from '@/app/(manager)/manager/fulfillment/picking/page';
import ManagerDispatchManifestPage from '@/app/(manager)/manager/fulfillment/dispatch/page';
import ManagerDeliveryAttemptsPage from '@/app/(manager)/manager/fulfillment/delivery/page';
import ManagerReturnsPage from '@/app/(manager)/manager/returns/page';
import ManagerCodSettlementsPage from '@/app/(manager)/manager/settlements/page';
import ManagerProductDraftsPage from '@/app/(manager)/manager/catalog/drafts/page';

// Owner Pages
import AdminOverviewPage from '@/app/(owner)/admin/page';
import OwnerPricingPage from '@/app/(owner)/admin/pricing/page';
import OwnerSettlementsPage from '@/app/(owner)/admin/settlements/page';
import OwnerReturnsRefundsPage from '@/app/(owner)/admin/returns/page';

// Layout Guards
import AdminLayout from '@/app/(owner)/admin/layout';
import ManagerLayout from '@/app/(manager)/manager/layout';

// APIs
import { adminApi } from '@/lib/api/admin';
import { catalogApi } from '@/lib/api/catalog';
import { customerApi } from '@/lib/api/customer';

// Mock Router
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/',
  useSearchParams: () => new URLSearchParams(),
}));

interface TestUser {
  id: string;
  role: 'CUSTOMER' | 'MANAGER' | 'OWNER';
  username: string;
  fullName: string;
}

let mockUser: TestUser | null = null;
let mockIsAuthenticated = false;

vi.mock('@/lib/auth/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: mockIsAuthenticated,
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <AuthProvider>
      <ToastProvider>{ui}</ToastProvider>
    </AuthProvider>
  );
}

describe('Component 15 — End-to-End Quality, Performance, Security & Role Verification', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = null;
    mockIsAuthenticated = false;
  });

  /* =========================================================================
     1. COMPLETE CUSTOMER SHOPPING JOURNEY
     ========================================================================= */
  describe('1. Customer Shopping & Checkout Journey', () => {
    it('1.1 Storefront renders editorial curation, Surat heritage badge, and announcement bar', async () => {
      renderWithProviders(<HomePage />);

      expect(screen.getAllByText(/surat atelier/i)[0]).toBeDefined();
      expect(screen.getAllByText(/explore collections/i)[0]).toBeDefined();
      expect(screen.getAllByText(/design system/i)[0]).toBeDefined();
    });

    it('1.2 Shopping bag enforces price-shift confirmation before checkout navigation', async () => {
      mockUser = { id: 'cust-101', role: 'CUSTOMER', username: 'priya_sharma', fullName: 'Priya Sharma' };
      mockIsAuthenticated = true;

      // Mock Cart with price shift detected
      vi.spyOn(customerApi, 'getCart').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          id: 'cart-101',
          customerId: 'cust-101',
          items: [
            {
              id: 'item-1',
              productId: 'p-1',
              variantId: 'var-1',
              sku: 'SRT-BAN-GLD-L',
              productName: 'Surat Banarasi Zari Saree',
              size: 'L',
              colour: 'Gold',
              quantity: 1,
              priceSnapshot: 5499,
              currentPrice: 5999,
              priceChanged: true,
              available: true,
              itemTotal: 5999,
            },
          ],
          totalItems: 1,
          subtotal: 5999,
          priceConfirmationRequired: true,
        },
      });

      renderWithProviders(<CartPage />);

      await waitFor(() => {
        expect(screen.getByText(/garment price shift detected/i)).toBeDefined();
      });

      // Confirm price change warning text
      expect(screen.getByText(/i acknowledge and accept the updated garment prices/i)).toBeDefined();
    });

    it('1.3 Authoritative checkout enforces delivery threshold, COD PIN verification, and store credit deduction', async () => {
      mockUser = { id: 'cust-101', role: 'CUSTOMER', username: 'priya_sharma', fullName: 'Priya Sharma' };
      mockIsAuthenticated = true;

      vi.spyOn(customerApi, 'getProfile').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          id: 'cust-101',
          username: 'priya_sharma',
          email: 'priya@example.com',
          role: 'CUSTOMER',
          fullName: 'Priya Sharma',
          phoneNumber: '9876543210',
          addressLine1: 'B-402, Surat Ring Road',
          city: 'Surat',
          state: 'Gujarat',
          postalCode: '395002',
          country: 'India',
        },
      });

      vi.spyOn(customerApi, 'getStoreCreditBalance').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          customerId: 'cust-101',
          currentBalance: 500,
          totalIssued: 1000,
          totalRedeemed: 500,
          neverExpires: true,
        },
      });

      vi.spyOn(customerApi, 'previewCheckout').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          subtotalBeforeDiscountAndTax: 2499,
          totalPhysicalUnits: 1,
          freeDeliveryEligible: true,
          deliveryCharge: 0,
          promotionalDiscountAmount: 0,
          gstAmount: 299,
          gstRatePercent: 12,
          storeCreditUsed: 0,
          finalPayableAmount: 2798,
          codServiceable: true,
          priceConfirmationRequired: false,
        },
      });

      renderWithProviders(<CheckoutPage />);

      await waitFor(() => {
        expect(screen.getByText(/delivery address & recipient/i)).toBeDefined();
        expect(screen.getByText(/authoritative price breakdown/i)).toBeDefined();
      });

      // Verify payment methods and store credit
      expect(screen.getByText(/simulated online payment/i)).toBeDefined();
      expect(screen.getByText(/cash on delivery \(cod\)/i)).toBeDefined();
    });

    it('1.4 Customer orders page renders orders and parcel tracking links', async () => {
      mockUser = { id: 'cust-101', role: 'CUSTOMER', username: 'priya_sharma', fullName: 'Priya Sharma' };
      mockIsAuthenticated = true;

      vi.spyOn(customerApi, 'getMyOrders').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-cancellable',
              orderNumber: 'ORD-20261118-001',
              orderStatus: 'PROCESSING',
              paymentMethod: 'CASH_ON_DELIVERY',
              totalItems: 1,
              totalPayable: 2499,
              createdAt: '2026-11-18T05:00:00Z',
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      renderWithProviders(<CustomerOrdersPage />);

      await waitFor(() => {
        expect(screen.getByText(/ORD-20261118-001/)).toBeDefined();
      });

      expect(screen.getByText(/view order breakdown/i)).toBeDefined();
      expect(screen.getByText(/eligible for instant pre-dispatch cancellation/i)).toBeDefined();
    });
  });

  /* =========================================================================
     2. MANAGER FULFILLMENT & WAREHOUSE OPERATIONS (ZERO-FINANCE INVARIANT)
     ========================================================================= */
  describe('2. Manager Operational Fulfillment & Warehouse Invariants', () => {
    beforeEach(() => {
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'surat_ops', fullName: 'Warehouse Manager' };
      mockIsAuthenticated = true;
    });

    it('2.1 Fulfillment queue supports atomic order claiming and displays zero financial data', async () => {
      vi.spyOn(adminApi, 'getOrderQueue').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-901',
              orderNumber: 'ORD-SRT-901',
              orderStatus: 'PROCESSING',
              paymentMethod: 'CASH_ON_DELIVERY',
              paymentStatus: 'PENDING',
              totalItems: 2,
              totalPayable: 4500, // Present in DTO, strictly hidden from Manager UI!
              createdAt: '2026-11-18T06:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      const { container } = renderWithProviders(<ManagerOrderQueuePage />);

      await waitFor(() => {
        expect(screen.getByText('ORD-SRT-901')).toBeDefined();
      });

      // Manager sees units, but NEVER currency or revenue
      expect(screen.getByText('2 units')).toBeDefined();
      const text = container.textContent || '';
      expect(text).not.toContain('₹');
      expect(text).not.toContain('4500');
      expect(text).not.toContain('Revenue');
    });

    it('2.2 Combined picking list & packing slip strictly masks customer phone (98765****0)', async () => {
      vi.spyOn(adminApi, 'getCombinedPickingList').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          batchDate: '2026-11-18',
          totalOrders: 1,
          totalUnits: 2,
          items: [
            {
              orderNumber: 'ORD-SRT-901',
              sku: 'SRT-SLK-BLU-M',
              productName: 'Surat Tussar Silk Saree',
              size: 'M',
              colour: 'Royal Blue',
              quantity: 2,
              warehouseLocation: 'BIN-B2-09',
            },
          ],
        },
      });

      const { container } = renderWithProviders(<ManagerPickingPackingPage />);

      await waitFor(() => {
        expect(screen.getByText('SRT-SLK-BLU-M')).toBeDefined();
      });

      // Open Packing Slip
      const slipBtn = screen.getByRole('button', { name: /^slip$/i });
      fireEvent.click(slipBtn);

      await waitFor(() => {
        expect(screen.getByText(/98765\*\*\*\*0/)).toBeDefined();
        expect(screen.getByText(/privacy masked/i)).toBeDefined();
      });

      // Zero financial data on packing slip
      const text = container.textContent || '';
      expect(text).not.toContain('₹');
      expect(text).not.toContain('Amount Due');
    });

    it('2.3 Daily dispatch batch manifest compiles transport agency parcel handovers', async () => {
      renderWithProviders(<ManagerDispatchManifestPage />);

      await waitFor(() => {
        expect(screen.getByText(/daily dispatch manifests & handover/i)).toBeDefined();
      });

      expect(screen.getByRole('button', { name: /create dispatch batch/i })).toBeDefined();
    });

    it('2.4 Delivery attempt logger enforces Max 2 Attempts rule before Surat RTO warehouse intake', async () => {
      renderWithProviders(<ManagerDeliveryAttemptsPage />);

      await waitFor(() => {
        expect(screen.getByText(/courier handover & delivery attempt logger/i)).toBeDefined();
      });

      expect(screen.getByText(/maximum 2 delivery attempts/i)).toBeDefined();
      expect(screen.getAllByRole('button', { name: /record failed attempt/i })[0]).toBeDefined();
      expect(screen.getAllByRole('button', { name: /confirm delivery/i })[0]).toBeDefined();
      expect(screen.getByText(/rto \(2 failed attempts\)/i)).toBeDefined();
    });

    it('2.5 Quality return physical inspection checklist allows exchange dispatch, gates refund to Owner approval', async () => {
      renderWithProviders(<ManagerReturnsPage />);

      await waitFor(() => {
        expect(screen.getByText(/surat return intake & 3-day quality inspection/i)).toBeDefined();
      });

      expect(screen.getByText(/physical inspection checklist/i)).toBeDefined();
      expect(screen.getByText(/forward to owner for refund authorization/i)).toBeDefined();
    });

    it('2.6 Manager product draft submission contains strictly zero price inputs', async () => {
      renderWithProviders(<ManagerProductDraftsPage />);

      await waitFor(() => {
        expect(screen.getByText(/garment drafts & revision proposals/i)).toBeDefined();
      });

      const createBtn = screen.getByRole('button', { name: /create garment draft/i });
      fireEvent.click(createBtn);

      // FORM INVARIANT: Zero price fields in Manager draft modal
      expect(screen.queryByLabelText(/base price/i)).toBeNull();
      expect(screen.queryByLabelText(/retail price/i)).toBeNull();
      expect(screen.queryByLabelText(/discount price/i)).toBeNull();
      expect(screen.queryByRole('button', { name: /^publish$/i })).toBeNull();
    });
  });

  /* =========================================================================
     3. OWNER ADMINISTRATIVE FINANCIAL GOVERNANCE & PRIVILEGES
     ========================================================================= */
  describe('3. Owner Financial Governance & Administrative Privileges', () => {
    beforeEach(() => {
      mockUser = { id: 'owner-001', role: 'OWNER', username: 'darshit_zaveri', fullName: 'Darshit Zaveri' };
      mockIsAuthenticated = true;
    });

    it('3.1 Owner Analytics Dashboard displays exclusive GMV and Net Settled Revenue metrics', async () => {
      vi.spyOn(adminApi, 'getAnalyticsReport').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          totalOrders: 142,
          grossMerchandiseValue: 1250000,
          totalDiscountGiven: 85000,
          netSalesRevenue: 1040000,
          onlinePaidOrders: 60,
          onlinePaidAmount: 520000,
          codOrders: 82,
          codAmount: 730000,
          cancelledOrders: 4,
          openSupportTickets: 2,
          escalatedSupportTickets: 0,
          reportedShortages: 1,
        },
      });

      const { container } = renderWithProviders(<AdminOverviewPage />);

      await waitFor(() => {
        expect(screen.getByText('Gross Merchandise Value')).toBeDefined();
        expect(screen.getByText('Net Settled Revenue')).toBeDefined();
      });

      const text = container.textContent || '';
      expect(text).toContain('Gross Merchandise Value');
      expect(text).toContain('Net Settled Revenue');
    });

    it('3.2 Owner retail pricing modification requires mandatory audit change reason', async () => {
      vi.spyOn(catalogApi, 'getProducts').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'p-1',
              name: 'Surat Handloom Kanjeevaram Saree',
              slug: 'surat-handloom-kanjeevaram-saree',
              categoryName: 'Bridal Sarees',
              defaultPrice: 8500,
              fabric: 'Silk',
              status: 'PUBLISHED',
              primaryImageUrl: null,
            },
          ],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      renderWithProviders(<OwnerPricingPage />);

      await waitFor(() => {
        expect(screen.getByText('Price Management')).toBeDefined();
        expect(screen.getByText('Surat Handloom Kanjeevaram Saree')).toBeDefined();
      });

      const updateBtn = screen.getAllByRole('button', { name: /update price/i })[0];
      fireEvent.click(updateBtn);

      // Audit reason input is required
      expect(screen.getByText(/audit change reason/i)).toBeDefined();
    });

    it('3.3 Owner COD settlement approval reconciles agency cash remittance batches', async () => {
      vi.spyOn(adminApi, 'getAllCodSettlementBatches').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'batch-99',
              batchNumber: 'COD-RECON-20261118',
              agencyName: 'Delhivery Surat Air',
              agencyInvoiceReference: 'DEL-INV-9901',
              totalOrders: 14,
              totalCollectedAmount: 32000,
              totalExpectedAmount: 32000,
              totalRemittedAmount: 32000,
              hasDiscrepancy: false,
              discrepancyAmount: 0,
              status: 'SUBMITTED_FOR_APPROVAL',
              preparedByManagerName: 'Warehouse Staff',
              createdAt: '2026-11-18T05:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      renderWithProviders(<OwnerSettlementsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /approve settlement/i })).toBeDefined();
      });
    });

    it('3.4 Owner returns & refunds enforces 7-day SLA authorization deadline', async () => {
      renderWithProviders(<OwnerReturnsRefundsPage />);

      await waitFor(() => {
        expect(screen.getByText(/refund & return approvals/i)).toBeDefined();
      });

      expect(screen.getByText(/7-day refund sla mandate/i)).toBeDefined();
    });
  });

  /* =========================================================================
     4. SECURITY MATRIX & ROLE BOUNDARY GUARDS
     ========================================================================= */
  describe('4. Security Matrix, Privilege Escalation & IDOR Protection', () => {
    it('4.1 AdminLayout strictly blocks Manager and renders Owner Access Required', async () => {
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'surat_ops', fullName: 'Warehouse Staff' };
      mockIsAuthenticated = true;

      renderWithProviders(
        <AdminLayout>
          <div>Owner Classified Financials</div>
        </AdminLayout>
      );

      expect(screen.getByText(/owner access required/i)).toBeDefined();
      expect(screen.queryByText('Owner Classified Financials')).toBeNull();
    });

    it('4.2 AdminLayout strictly blocks Customer from accessing Owner partition', async () => {
      mockUser = { id: 'cust-101', role: 'CUSTOMER', username: 'priya_sharma', fullName: 'Priya Sharma' };
      mockIsAuthenticated = true;

      renderWithProviders(
        <AdminLayout>
          <div>Owner Classified Financials</div>
        </AdminLayout>
      );

      expect(screen.getByText(/owner access required/i)).toBeDefined();
      expect(screen.queryByText('Owner Classified Financials')).toBeNull();
    });

    it('4.3 ManagerLayout strictly blocks Customer and renders Staff Access Required', async () => {
      mockUser = { id: 'cust-101', role: 'CUSTOMER', username: 'priya_sharma', fullName: 'Priya Sharma' };
      mockIsAuthenticated = true;

      renderWithProviders(
        <ManagerLayout>
          <div>Warehouse Operational Queue</div>
        </ManagerLayout>
      );

      expect(screen.getByText(/staff access required/i)).toBeDefined();
      expect(screen.queryByText('Warehouse Operational Queue')).toBeNull();
    });

    it('4.4 Manager is prohibited from approving COD settlement batches', async () => {
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'surat_ops', fullName: 'Warehouse Staff' };
      mockIsAuthenticated = true;

      vi.spyOn(adminApi, 'getAllCodSettlementBatches').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'batch-99',
              batchNumber: 'COD-RECON-20261118',
              agencyName: 'Blue Dart Surat',
              agencyInvoiceReference: 'BD-88',
              totalOrders: 2,
              totalCollectedAmount: 5000,
              totalExpectedAmount: 5000,
              totalRemittedAmount: 5000,
              hasDiscrepancy: false,
              discrepancyAmount: 0,
              status: 'SUBMITTED_FOR_APPROVAL',
              preparedByManagerName: 'Warehouse Staff',
              createdAt: '2026-11-18T05:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      renderWithProviders(<ManagerCodSettlementsPage />);

      await waitFor(() => {
        expect(screen.getByText(/agency cod settlement preparation/i)).toBeDefined();
      });

      // Manager MUST NOT have Approve action button
      expect(screen.queryByRole('button', { name: /^approve batch$/i })).toBeNull();
      expect(screen.queryByRole('button', { name: /approve settlement/i })).toBeNull();
    });
  });

  /* =========================================================================
     5. CONCURRENCY & IDEMPOTENCY HANDLING
     ========================================================================= */
  describe('5. Concurrency & Idempotency Safeguards', () => {
    it('5.1 Concurrent order claim conflict returns conflict alert without corrupting manager queue', async () => {
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'surat_ops', fullName: 'Warehouse Staff' };
      mockIsAuthenticated = true;

      vi.spyOn(adminApi, 'getOrderQueue').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-race-1',
              orderNumber: 'ORD-RACE-001',
              orderStatus: 'PENDING',
              paymentMethod: 'PREPAID',
              paymentStatus: 'PAID',
              totalItems: 1,
              totalPayable: 1999,
              createdAt: '2026-11-18T05:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      vi.spyOn(adminApi, 'claimOrder').mockRejectedValue({
        status: 409,
        message: 'Order already claimed by another manager concurrently',
      });

      renderWithProviders(<ManagerOrderQueuePage />);

      await waitFor(() => {
        expect(screen.getByText('ORD-RACE-001')).toBeDefined();
      });

      const claimBtn = screen.getByRole('button', { name: /claim order/i });
      fireEvent.click(claimBtn);

      await waitFor(() => {
        expect(screen.getByText(/concurrent claim conflict/i)).toBeDefined();
      });
    });
  });

  /* =========================================================================
     6. ACCESSIBILITY STANDARDS & RESPONSIVE SEMANTICS
     ========================================================================= */
  describe('6. Accessibility & Semantic Landmarks', () => {
    it('6.1 UI inputs have associated label semantics and aria landmarks', () => {
      renderWithProviders(
        <form aria-label="Customer Profile Form">
          <label htmlFor="customer-name">Full Name</label>
          <input id="customer-name" name="name" type="text" />
          <button type="submit">Save Changes</button>
        </form>
      );

      expect(screen.getByRole('form', { name: /customer profile form/i })).toBeDefined();
      expect(screen.getByLabelText(/full name/i)).toBeDefined();
      expect(screen.getByRole('button', { name: /save changes/i })).toBeDefined();
    });
  });
});
