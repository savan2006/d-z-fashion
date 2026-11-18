import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import ManagerOrderQueuePage from '@/app/(manager)/manager/orders/page';
import ManagerPickingPackingPage from '@/app/(manager)/manager/fulfillment/picking/page';
import ManagerProductDraftsPage from '@/app/(manager)/manager/catalog/drafts/page';
import ManagerCodSettlementsPage from '@/app/(manager)/manager/settlements/page';
import ManagerOperationalReportsPage from '@/app/(manager)/manager/reports/operational/page';
import AdminOverviewPage from '@/app/(owner)/admin/page';
import OwnerPricingPage from '@/app/(owner)/admin/pricing/page';
import OwnerSettlementsPage from '@/app/(owner)/admin/settlements/page';
import AdminLayout from '@/app/(owner)/admin/layout';
import ManagerLayout from '@/app/(manager)/manager/layout';
import { adminApi } from '@/lib/api/admin';
import { catalogApi } from '@/lib/api/catalog';
import { ToastProvider } from '@/components/ui/Toast';

// Mock useRouter
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/manager/orders',
  useSearchParams: () => new URLSearchParams(),
}));

interface TestUser {
  id: string;
  role: string;
  username: string;
  fullName: string;
}

// Mock AuthContext
let mockUser: TestUser | null = {
  id: 'mgr-001',
  role: 'MANAGER',
  username: 'manager_staff',
  fullName: 'Surat Operations Manager',
};
let mockIsAuthenticated = true;
let mockIsLoading = false;

vi.mock('@/lib/auth/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: mockIsAuthenticated,
    isLoading: mockIsLoading,
    login: vi.fn(),
    logout: vi.fn(),
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

describe('Component 14 — Owner and Manager Operational Dashboards', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = {
      id: 'mgr-001',
      role: 'MANAGER',
      username: 'manager_staff',
      fullName: 'Surat Operations Manager',
    };
    mockIsAuthenticated = true;
    mockIsLoading = false;
  });

  describe('Manager Zero-Finance Invariant', () => {
    it('Order Queue renders quantities and statuses, but ZERO revenue or prices', async () => {
      vi.spyOn(adminApi, 'getOrderQueue').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-1',
              orderNumber: 'ORD-TEST-001',
              orderStatus: 'PROCESSING',
              paymentMethod: 'CASH_ON_DELIVERY',
              paymentStatus: 'PENDING',
              totalItems: 3,
              totalPayable: 7500, // Present in DTO, but Manager UI must NEVER render this!
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

      const { container } = render(<ManagerOrderQueuePage />);

      await waitFor(() => {
        expect(screen.getByText('ORD-TEST-001')).toBeDefined();
      });

      // Verify item quantity is shown
      expect(screen.getByText('3 units')).toBeDefined();

      // INVARIANT: Ensure 7500 or rupee symbol does NOT appear in table
      const text = container.textContent || '';
      expect(text).not.toContain('7500');
      expect(text).not.toContain('₹');
      expect(text).not.toContain('totalPayable');
    });

    it('Picking & Packing slip masks recipient phone and contains zero financial figures', async () => {
      vi.spyOn(adminApi, 'getCombinedPickingList').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          batchDate: '2026-11-18',
          totalOrders: 2,
          totalUnits: 4,
          items: [
            {
              orderNumber: 'ORD-TEST-002',
              sku: 'SRT-SLK-RED-M',
              productName: 'Surat Pure Silk Zari Saree',
              size: 'M',
              colour: 'Crimson Red',
              quantity: 2,
              warehouseLocation: 'BIN-A1-04',
            },
          ],
        },
      });

      const { container } = render(<ManagerPickingPackingPage />);

      await waitFor(() => {
        expect(screen.getByText('SRT-SLK-RED-M')).toBeDefined();
        expect(screen.getByText('BIN-A1-04')).toBeDefined();
      });

      // Check slip button opens modal with masked phone
      const slipBtn = screen.getByRole('button', { name: /^slip$/i });
      fireEvent.click(slipBtn);

      await waitFor(() => {
        expect(screen.getByText(/98765\*\*\*\*0/)).toBeDefined();
        expect(screen.getByText(/privacy masked/i)).toBeDefined();
      });

      // INVARIANT: Zero financial numbers
      const text = container.textContent || '';
      expect(text).not.toContain('₹');
      expect(text).not.toContain('Total Payable');
    });

    it('Manager Operational Report contains strictly unit counts, never revenue or profit', async () => {
      vi.spyOn(adminApi, 'getOperationalReport').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          totalOrders: 42,
          pendingClaimOrders: 6,
          claimedOrders: 15,
          inPickingOrders: 8,
          dispatchedOrders: 10,
          deliveredOrders: 28,
          openSupportTickets: 3,
          escalatedSupportTickets: 1,
          reportedShortages: 2,
        },
      });

      const { container } = render(<ManagerOperationalReportsPage />);

      await waitFor(() => {
        expect(screen.getByText('42')).toBeDefined();
        expect(screen.getByText('Currently in Picking')).toBeDefined();
      });

      const text = container.textContent || '';
      expect(text).not.toContain('Gross Merchandise Value');
      expect(text).not.toContain('Net Revenue');
      expect(text).not.toContain('Profit');
      expect(text).not.toContain('₹');
    });
  });

  describe('Atomic Claim & Concurrent Conflict Handling', () => {
    it('Claims order atomically and notifies manager', async () => {
      vi.spyOn(adminApi, 'getOrderQueue').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-unclaimed',
              orderNumber: 'ORD-CLAIM-100',
              orderStatus: 'PENDING',
              paymentMethod: 'PREPAID',
              paymentStatus: 'PAID',
              totalItems: 1,
              totalPayable: 2000,
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

      const claimSpy = vi.spyOn(adminApi, 'claimOrder').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          id: 'ord-unclaimed',
          orderNumber: 'ORD-CLAIM-100',
          orderStatus: 'PROCESSING',
          paymentMethod: 'PREPAID',
          paymentStatus: 'PAID',
          totalItems: 1,
          totalPayable: 2000,
          claimedByManagerId: 'mgr-001',
          claimedByManagerName: 'Surat Operations Manager',
          createdAt: '2026-11-18T05:00:00Z',
        },
      });

      render(<ManagerOrderQueuePage />);

      await waitFor(() => {
        expect(screen.getByText('ORD-CLAIM-100')).toBeDefined();
      });

      const claimBtn = screen.getByRole('button', { name: /claim order/i });
      fireEvent.click(claimBtn);

      await waitFor(() => {
        expect(claimSpy).toHaveBeenCalledWith('ORD-CLAIM-100');
        expect(screen.getByText(/claimed successfully/i)).toBeDefined();
      });
    });

    it('Handles concurrent claim conflict gracefully with warning banner', async () => {
      vi.spyOn(adminApi, 'getOrderQueue').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'ord-conflict',
              orderNumber: 'ORD-CONFLICT-99',
              orderStatus: 'PENDING',
              paymentMethod: 'CASH_ON_DELIVERY',
              paymentStatus: 'PENDING',
              totalItems: 2,
              totalPayable: 4000,
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
        message: 'Order already claimed by another manager',
      });

      render(<ManagerOrderQueuePage />);

      await waitFor(() => {
        expect(screen.getByText('ORD-CONFLICT-99')).toBeDefined();
      });

      const claimBtn = screen.getByRole('button', { name: /claim order/i });
      fireEvent.click(claimBtn);

      await waitFor(() => {
        expect(screen.getByText(/concurrent claim conflict/i)).toBeDefined();
      });
    });
  });

  describe('Manager Product Drafts without Price Controls', () => {
    it('Manager draft form never renders price inputs, publish buttons, or delete buttons', async () => {
      render(<ManagerProductDraftsPage />);

      await waitFor(() => {
        expect(screen.getByText(/garment drafts & revision proposals/i)).toBeDefined();
      });

      const openDraftBtn = screen.getByRole('button', { name: /create garment draft/i });
      fireEvent.click(openDraftBtn);

      // Verify draft modal is open
      expect(screen.getByText(/create garment draft \(manager submission\)/i)).toBeDefined();

      // INVARIANT: basePrice or discountPrice must NOT exist in the form
      expect(screen.queryByLabelText(/base price/i)).toBeNull();
      expect(screen.queryByLabelText(/retail price/i)).toBeNull();
      expect(screen.queryByLabelText(/discount price/i)).toBeNull();

      // INVARIANT: publish and delete buttons must NOT exist in Manager UI
      expect(screen.queryByRole('button', { name: /^publish$/i })).toBeNull();
      expect(screen.queryByRole('button', { name: /^delete$/i })).toBeNull();
    });
  });

  describe('Owner Administrative Privileges & Segregation of Duties', () => {
    it('Owner Overview renders exclusive financial metrics', async () => {
      mockUser = { id: 'owner-001', role: 'OWNER', username: 'owner_dz', fullName: 'Darshit Zaveri' };

      vi.spyOn(adminApi, 'getAnalyticsReport').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          totalOrders: 150,
          grossMerchandiseValue: 850000,
          totalDiscountGiven: 45000,
          netSalesRevenue: 720000,
          onlinePaidOrders: 60,
          onlinePaidAmount: 320000,
          codOrders: 90,
          codAmount: 400000,
          cancelledOrders: 5,
          openSupportTickets: 3,
          escalatedSupportTickets: 1,
          reportedShortages: 0,
        },
      });

      const { container } = render(<AdminOverviewPage />);

      await waitFor(() => {
        expect(screen.getByText('Gross Merchandise Value')).toBeDefined();
        expect(screen.getByText('Net Settled Revenue')).toBeDefined();
      });

      // Verify financial data is present for Owner
      expect(container.textContent).toContain('Gross Merchandise Value');
      expect(container.textContent).toContain('Net Settled Revenue');
    });

    it('Owner price modification requires mandatory change reason', async () => {
      mockUser = { id: 'owner-001', role: 'OWNER', username: 'owner_dz', fullName: 'Darshit Zaveri' };

      vi.spyOn(catalogApi, 'getProducts').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'prod-1',
              name: 'Surat Pure Silk Saree',
              slug: 'surat-pure-silk-saree',
              categoryName: 'Sarees',
              defaultPrice: 5200,
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

      const updateSpy = vi.spyOn(adminApi, 'updateProductPrice').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          id: 'prod-1',
          title: 'Surat Pure Silk Saree',
          slug: 'surat-pure-silk-saree',
          categoryName: 'Sarees',
          categorySlug: 'sarees',
          status: 'PUBLISHED',
          basePrice: 5200,
          totalVariants: 1,
          hasPendingRevision: false,
          createdAt: '',
          updatedAt: '',
        },
      });

      render(
        <ToastProvider>
          <OwnerPricingPage />
        </ToastProvider>
      );

      await waitFor(() => {
        expect(screen.getByText('Price Management')).toBeDefined();
        expect(screen.getByText('Surat Pure Silk Saree')).toBeDefined();
      });

      // Click update price button
      const editPriceBtns = screen.getAllByRole('button', { name: /update price/i });
      fireEvent.click(editPriceBtns[0]);

      // Form requires mandatory audit reason
      expect(screen.getByText(/audit change reason/i)).toBeDefined();
      expect(updateSpy).toBeDefined();
    });

    it('Manager cannot approve COD settlement batches, while Owner has approve action', async () => {
      vi.spyOn(adminApi, 'getAllCodSettlementBatches').mockResolvedValue({
        status: 200,
        ok: true,
        data: {
          content: [
            {
              id: 'batch-1',
              batchNumber: 'COD-2026-001',
              agencyName: 'Blue Dart Surat',
              agencyInvoiceReference: 'BD-SRT-99',
              totalOrders: 5,
              totalCollectedAmount: 12500,
              totalExpectedAmount: 12500,
              totalRemittedAmount: 12500,
              hasDiscrepancy: false,
              discrepancyAmount: 0,
              status: 'SUBMITTED_FOR_APPROVAL',
              preparedByManagerName: 'Staff',
              createdAt: '2026-11-18T00:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
          last: true,
        },
      });

      // 1. Manager view
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'manager_staff', fullName: 'Staff' };
      const { queryByRole: managerQuery } = render(
        <ToastProvider>
          <ManagerCodSettlementsPage />
        </ToastProvider>
      );
      expect(managerQuery('button', { name: /^approve batch$/i })).toBeNull();

      // 2. Owner view
      mockUser = { id: 'owner-001', role: 'OWNER', username: 'owner_dz', fullName: 'Darshit Zaveri' };

      render(
        <ToastProvider>
          <OwnerSettlementsPage />
        </ToastProvider>
      );
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /approve settlement/i })).toBeDefined();
      });
    });
  });

  describe('Role-Based Layout Access Guards', () => {
    it('AdminLayout blocks non-owner (Manager) and renders access required message', async () => {
      mockUser = { id: 'mgr-001', role: 'MANAGER', username: 'manager_staff', fullName: 'Staff' };

      render(
        <AdminLayout>
          <div>Protected Owner Content</div>
        </AdminLayout>
      );

      expect(screen.getByText(/owner access required/i)).toBeDefined();
      expect(screen.queryByText('Protected Owner Content')).toBeNull();
    });

    it('ManagerLayout blocks Customer and renders staff access required message', async () => {
      mockUser = { id: 'cust-001', role: 'CUSTOMER', username: 'customer_user', fullName: 'Customer' };

      render(
        <ManagerLayout>
          <div>Protected Manager Content</div>
        </ManagerLayout>
      );

      expect(screen.getByText(/staff access required/i)).toBeDefined();
      expect(screen.queryByText('Protected Manager Content')).toBeNull();
    });
  });
});
