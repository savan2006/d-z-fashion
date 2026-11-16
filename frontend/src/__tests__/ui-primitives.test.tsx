import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { Button, LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { TextField } from '@/components/ui/TextField';
import { Select } from '@/components/ui/Select';
import { Checkbox } from '@/components/ui/Checkbox';
import { Dialog } from '@/components/ui/Dialog';
import { Pagination } from '@/components/ui/Pagination';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Skeleton } from '@/components/ui/Skeleton';

describe('UI Primitives System', () => {
  describe('Button & LinkButton', () => {
    it('renders with primary variant and triggers click', () => {
      const handleClick = vi.fn();
      render(<Button onClick={handleClick}>Click Me</Button>);
      const btn = screen.getByRole('button', { name: /click me/i });
      expect(btn.className).toContain('btn-primary');
      fireEvent.click(btn);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('disables button when isLoading is true and renders spinner', () => {
      const handleClick = vi.fn();
      render(<Button isLoading={true} onClick={handleClick}>Submit</Button>);
      const btn = screen.getByRole('button', { name: /submit/i }) as HTMLButtonElement;
      expect(btn.disabled).toBe(true);
      fireEvent.click(btn);
      expect(handleClick).not.toHaveBeenCalled();
    });

    it('renders LinkButton with proper href and classes', () => {
      render(<LinkButton href="/admin" variant="outline">Admin Panel</LinkButton>);
      const link = screen.getByRole('link', { name: /admin panel/i });
      expect(link.getAttribute('href')).toBe('/admin');
      expect(link.className).toContain('btn-outline');
    });
  });

  describe('Badge', () => {
    it('renders semantic variants', () => {
      render(
        <div>
          <Badge variant="wine">Wine Badge</Badge>
          <Badge variant="success">Delivered</Badge>
          <Badge variant="warning">Low Stock</Badge>
          <Badge variant="error">Damaged</Badge>
        </div>
      );
      expect(screen.getByText('Wine Badge').parentElement?.className).toContain('badge-wine');
      expect(screen.getByText('Delivered').parentElement?.className).toContain('badge-success');
      expect(screen.getByText('Low Stock').parentElement?.className).toContain('badge-warning');
      expect(screen.getByText('Damaged').parentElement?.className).toContain('badge-error');
    });
  });

  describe('TextField & Select & Checkbox', () => {
    it('renders TextField with label, error and aria attributes', () => {
      render(
        <TextField
          id="email-input"
          label="Email Address"
          required={true}
          error="Invalid email format"
        />
      );
      const input = screen.getByLabelText(/email address/i);
      expect(input.getAttribute('aria-invalid')).toBe('true');
      expect(screen.getByRole('alert').textContent).toBe('Invalid email format');
    });

    it('renders Select with options and placeholder', () => {
      render(
        <Select
          label="Category"
          placeholder="Choose category"
          options={[
            { value: 'kurti', label: 'Kurti' },
            { value: 'dress', label: 'Dress' },
          ]}
        />
      );
      const select = screen.getByLabelText(/category/i);
      expect(select).toBeDefined();
      expect(screen.getByText('Kurti')).toBeDefined();
    });

    it('renders Checkbox and handles state toggles', () => {
      const handleChange = vi.fn();
      render(
        <Checkbox
          id="terms-cb"
          label="Accept 3-day policy"
          description="Quality inspection required in Surat"
          onChange={handleChange}
        />
      );
      const checkbox = screen.getByLabelText(/accept 3-day policy/i) as HTMLInputElement;
      expect(checkbox.checked).toBe(false);
      fireEvent.click(checkbox);
      expect(handleChange).toHaveBeenCalled();
    });
  });

  describe('Dialog', () => {
    it('renders modal dialog and responds to escape key', () => {
      const handleClose = vi.fn();
      const { rerender } = render(
        <Dialog isOpen={false} onClose={handleClose} title="Test Modal">
          <p>Dialog content</p>
        </Dialog>
      );
      expect(screen.queryByRole('dialog')).toBeNull();

      rerender(
        <Dialog isOpen={true} onClose={handleClose} title="Test Modal">
          <p>Dialog content</p>
        </Dialog>
      );
      expect(screen.getByRole('dialog')).toBeDefined();
      expect(screen.getByText('Test Modal')).toBeDefined();

      fireEvent.keyDown(document, { key: 'Escape' });
      expect(handleClose).toHaveBeenCalled();
    });
  });

  describe('Pagination, EmptyState & ErrorState', () => {
    it('renders pagination and handles page change', () => {
      const handlePage = vi.fn();
      render(<Pagination currentPage={2} totalPages={5} onPageChange={handlePage} />);
      const nextBtn = screen.getByRole('button', { name: /next page/i });
      fireEvent.click(nextBtn);
      expect(handlePage).toHaveBeenCalledWith(3);
    });

    it('renders EmptyState and ErrorState with action triggers', () => {
      const handleRetry = vi.fn();
      render(
        <div>
          <EmptyState title="No items" description="Your bag is empty" />
          <ErrorState message="Network error" onRetry={handleRetry} />
        </div>
      );
      expect(screen.getByText('No items')).toBeDefined();
      expect(screen.getByText('Network error')).toBeDefined();

      const retryBtn = screen.getByRole('button', { name: /try again/i });
      fireEvent.click(retryBtn);
      expect(handleRetry).toHaveBeenCalledTimes(1);
    });

    it('renders Skeleton box with shimmer animation attribute', () => {
      const { container } = render(<Skeleton variant="circle" width={32} height={32} />);
      const el = container.firstChild as HTMLElement;
      expect(el.className).toContain('skeleton-box');
      expect(el.style.borderRadius).toBe('50%');
    });
  });
});
