export type UserRole = 'CUSTOMER' | 'MANAGER' | 'OWNER';

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  fullName?: string;
}

export interface AuthState {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
