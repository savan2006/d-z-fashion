'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';
import { AuthState, AuthUser, UserRole } from './types';

interface AuthContextType extends AuthState {
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  hasRole: (allowedRoles: UserRole | UserRole[]) => boolean;
  switchRoleForDemo: (role: UserRole) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const DEMO_USERS: Record<UserRole, AuthUser> = {
  OWNER: {
    id: '00000000-0000-0000-0000-000000000001',
    username: 'dzowner',
    email: 'owner@dzfashion.com',
    role: 'OWNER',
    fullName: 'D & Z Store Owner',
  },
  MANAGER: {
    id: '00000000-0000-0000-0000-000000000002',
    username: 'suratmanager',
    email: 'manager@dzfashion.com',
    role: 'MANAGER',
    fullName: 'Surat Warehouse Manager',
  },
  CUSTOMER: {
    id: '00000000-0000-0000-0000-000000000003',
    username: 'priyapatel',
    email: 'priya@example.com',
    role: 'CUSTOMER',
    fullName: 'Priya Patel',
  },
};

function getInitialAuthState(): AuthState {
  if (typeof window === 'undefined') {
    return {
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    };
  }

  try {
    const storedToken = localStorage.getItem('dz_auth_token');
    const storedUser = localStorage.getItem('dz_auth_user');

    if (storedToken && storedUser) {
      const parsedUser = JSON.parse(storedUser);
      return {
        token: storedToken,
        user: parsedUser,
        isAuthenticated: true,
        isLoading: false,
      };
    }
  } catch {
    // Ignore storage parse issues
  }

  return {
    token: null,
    user: null,
    isAuthenticated: false,
    isLoading: false,
  };
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>(getInitialAuthState);

  const login = useCallback((token: string, user: AuthUser) => {
    localStorage.setItem('dz_auth_token', token);
    localStorage.setItem('dz_auth_user', JSON.stringify(user));
    setState({
      token,
      user,
      isAuthenticated: true,
      isLoading: false,
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('dz_auth_token');
    localStorage.removeItem('dz_auth_user');
    setState({
      token: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  }, []);

  const hasRole = useCallback(
    (allowedRoles: UserRole | UserRole[]) => {
      if (!state.user) return false;
      const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
      return roles.includes(state.user.role);
    },
    [state.user]
  );

  const switchRoleForDemo = useCallback((role: UserRole) => {
    const demoUser = DEMO_USERS[role];
    const demoToken = `mock-token-${role.toLowerCase()}-${Date.now()}`;
    localStorage.setItem('dz_auth_token', demoToken);
    localStorage.setItem('dz_auth_user', JSON.stringify(demoUser));
    setState({
      token: demoToken,
      user: demoUser,
      isAuthenticated: true,
      isLoading: false,
    });
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        hasRole,
        switchRoleForDemo,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
