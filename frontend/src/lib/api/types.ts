export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  [key: string]: unknown;
}

export interface ApiError {
  status: number;
  message: string;
  error?: string;
  path?: string;
  timestamp?: string;
  fieldErrors?: Record<string, string>;
}

export interface PaginatedResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  data: T;
  status: number;
  ok: boolean;
  correlationId?: string;
}
