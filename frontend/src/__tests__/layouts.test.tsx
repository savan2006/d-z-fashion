import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { StorefrontHeader } from '@/components/layout/StorefrontHeader';
import { StorefrontFooter } from '@/components/layout/StorefrontFooter';
import { AdminSidebar } from '@/components/layout/AdminSidebar';
import { ManagerSidebar } from '@/components/layout/ManagerSidebar';
import { AuthProvider } from '@/lib/auth/AuthContext';
import ManagerOperationsPage from '@/app/(manager)/manager/page';
import AdminOverviewPage from '@/app/(owner)/admin/page';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => '/',
  useSearchParams: () => new URLSearchParams(),
}));

describe('Layout Shells and Role Boundaries', () => {
  describe('Storefront Shell', () => {
    it('renders announcement bar, navigation categories, and bag link', () => {
      render(
        <AuthProvider>
          <StorefrontHeader />
          <StorefrontFooter />
        </AuthProvider>
      );

      expect(screen.getByText(/pan-india shipping/i)).toBeDefined();
      expect(screen.getAllByText('New Arrivals')[0]).toBeDefined();
      expect(screen.getAllByText('Indian Wear')[0]).toBeDefined();
      expect(screen.getByRole('link', { name: /shopping bag/i })).toBeDefined();
      expect(screen.getByText(/surat craftsmanship/i) || screen.getByText(/surat, gujarat/i)).toBeDefined();
    });
  });

  describe('Owner Administration Shell', () => {
    it('renders Owner badge and full financial and audit controls', async () => {
      render(
        <AuthProvider>
          <AdminSidebar />
          <AdminOverviewPage />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByText('Owner Admin')).toBeDefined();
        expect(screen.getByText('Overview & Analytics')).toBeDefined();
        expect(screen.getByText('Audit Trail')).toBeDefined();
        expect(screen.getByText('Gross Merchandise Value')).toBeDefined();
        expect(screen.getByText('Net Settled Revenue')).toBeDefined();
      });
    });
  });

  describe('Manager Operational Shell (Strict Invariant: Zero Financial Data)', () => {
    it('renders operational queues and strictly ZERO revenue, profit, or GMV metrics', async () => {
      const { container } = render(
        <AuthProvider>
          <ManagerSidebar />
          <ManagerOperationsPage />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByText('Operations Manager')).toBeDefined();
        expect(screen.getByText('Orders in Picking')).toBeDefined();
        expect(screen.getByText('My Claimed Orders')).toBeDefined();
        expect(screen.getByText('Low-Stock Warnings')).toBeDefined();
      });

      // STRICT INVARIANT CHECK: Zero financial terms in Manager operational view
      const text = container.textContent || '';
      expect(text).not.toContain('Gross Merchandise Value');
      expect(text).not.toContain('Net Settled Revenue');
      expect(text).not.toContain('GMV');
      expect(text).not.toContain('Profit');
    });
  });
});
