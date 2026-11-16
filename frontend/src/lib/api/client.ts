import { ApiError, ApiResponse } from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'dz-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
}

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const correlationId = generateCorrelationId();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Correlation-ID': correlationId,
    ...(options.headers as Record<string, string>),
  };

  // Add auth token if available in browser storage
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('dz_auth_token');
    if (token && !headers['Authorization']) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    const returnedCorrelationId = response.headers.get('X-Correlation-ID') || correlationId;

    if (!response.ok) {
      let errorPayload: Partial<ApiError> & { detail?: string } = {};
      try {
        errorPayload = await response.json();
      } catch {
        errorPayload = { message: response.statusText };
      }

      const error: ApiError = {
        status: response.status,
        message:
          errorPayload.message ||
          errorPayload.detail ||
          `Request failed with status ${response.status}`,
        error: errorPayload.error,
        fieldErrors: errorPayload.fieldErrors,
        timestamp: new Date().toISOString(),
      };

      throw error;
    }

    if (response.status === 204) {
      return {
        data: {} as T,
        status: response.status,
        ok: true,
        correlationId: returnedCorrelationId,
      };
    }

    const data = await response.json();
    return {
      data,
      status: response.status,
      ok: true,
      correlationId: returnedCorrelationId,
    };
  } catch (err) {
    if ((err as ApiError).status) {
      throw err;
    }

    const networkError: ApiError = {
      status: 0,
      message: (err as Error).message || 'Unable to connect to D & Z Fashion services.',
      timestamp: new Date().toISOString(),
    };
    throw networkError;
  }
}

export const api = {
  get: <T>(endpoint: string, options?: RequestInit) =>
    apiFetch<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    apiFetch<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),

  put: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    apiFetch<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  patch: <T>(endpoint: string, body?: unknown, options?: RequestInit) =>
    apiFetch<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    apiFetch<T>(endpoint, { ...options, method: 'DELETE' }),
};
