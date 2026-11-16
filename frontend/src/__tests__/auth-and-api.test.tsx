import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import React from 'react';
import { generateCorrelationId, apiFetch } from '@/lib/api/client';
import { AuthProvider, useAuth } from '@/lib/auth/AuthContext';

describe('Auth and API Boundary', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  describe('API Client', () => {
    it('generates unique correlation ID with valid prefix', () => {
      const id1 = generateCorrelationId();
      const id2 = generateCorrelationId();
      expect(id1).toBeDefined();
      expect(typeof id1).toBe('string');
      expect(id1).not.toBe(id2);
    });

    it('injects correlation ID and Authorization header into fetch calls', async () => {
      localStorage.setItem('dz_auth_token', 'jwt-test-token-123');

      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        headers: new Headers({ 'X-Correlation-ID': 'test-corr-id' }),
        json: async () => ({ success: true, count: 5 }),
      });
      global.fetch = mockFetch;

      const response = await apiFetch<{ success: boolean }>('/api/v1/test');

      expect(mockFetch).toHaveBeenCalledTimes(1);
      const [url, init] = mockFetch.mock.calls[0];
      expect(url).toContain('/api/v1/test');
      expect(init.headers['Authorization']).toBe('Bearer jwt-test-token-123');
      expect(init.headers['X-Correlation-ID']).toBeDefined();
      expect(response.data.success).toBe(true);
      expect(response.correlationId).toBe('test-corr-id');
    });

    it('throws ApiError with status and message on HTTP failure', async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        statusText: 'Forbidden',
        headers: new Headers(),
        json: async () => ({ message: 'Access denied for Manager role to view financial reports' }),
      });
      global.fetch = mockFetch;

      await expect(apiFetch('/api/v1/admin/reports/analytics')).rejects.toMatchObject({
        status: 403,
        message: 'Access denied for Manager role to view financial reports',
      });
    });
  });

  describe('AuthContext and Role RBAC', () => {
    it('correctly reports role access and supports role switching', () => {
      const wrapper = ({ children }: { children: React.ReactNode }) => (
        <AuthProvider>{children}</AuthProvider>
      );

      const { result } = renderHook(() => useAuth(), { wrapper });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.hasRole('OWNER')).toBe(false);

      // Switch to Owner
      act(() => {
        result.current.switchRoleForDemo('OWNER');
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user?.role).toBe('OWNER');
      expect(result.current.hasRole('OWNER')).toBe(true);
      expect(result.current.hasRole('MANAGER')).toBe(false);
      expect(result.current.hasRole(['OWNER', 'MANAGER'])).toBe(true);

      // Switch to Manager
      act(() => {
        result.current.switchRoleForDemo('MANAGER');
      });

      expect(result.current.user?.role).toBe('MANAGER');
      expect(result.current.hasRole('MANAGER')).toBe(true);
      expect(result.current.hasRole('OWNER')).toBe(false);

      // Logout
      act(() => {
        result.current.logout();
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
    });
  });
});
