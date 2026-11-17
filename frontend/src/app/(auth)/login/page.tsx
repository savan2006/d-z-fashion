'use client';

import React, { Suspense, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth/AuthContext';
import { customerApi } from '@/lib/api/customer';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { ArrowRight, UserCheck } from 'lucide-react';

function LoginFormContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect') || '/';
  const { login, switchRoleForDemo } = useAuth();
  const { toast } = useToast();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !password) {
      toast({ type: 'warning', title: 'Missing credentials', message: 'Please enter your username/email and password.' });
      return;
    }

    setLoading(true);
    try {
      const res = await customerApi.login(identifier.trim(), password);
      if (res.data) {
        login(res.data.accessToken, res.data.user);
        toast({
          type: 'success',
          title: 'Welcome Back',
          message: `Signed in as ${res.data.user.fullName || res.data.user.username}.`,
        });
        router.push(redirectUrl);
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Invalid username/email or password';
      toast({ type: 'error', title: 'Sign In Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Demo Switch Handler for development/eval testing
  const handleDemoLogin = (role: 'CUSTOMER' | 'OWNER' | 'MANAGER') => {
    switchRoleForDemo(role);
    toast({
      type: 'info',
      title: 'Demo Session Active',
      message: `Signed in as ${role} for live testing.`,
    });
    if (role === 'OWNER') router.push('/admin');
    else if (role === 'MANAGER') router.push('/manager');
    else router.push(redirectUrl);
  };

  return (
    <div>
      <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <div>
          <span className="eyebrow">Customer Sign In</span>
          <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
            Welcome Back
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Sign in to access your saved address, review orders, or proceed to checkout.
          </p>
        </div>

        <TextField
          label="Username or Email"
          placeholder="Enter username or email"
          value={identifier}
          onChange={(e) => setIdentifier(e.target.value)}
          required
          autoFocus
        />

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
            <label className="form-label" style={{ margin: 0 }}>
              Password
            </label>
            <Link
              href="/forgot-password"
              style={{ fontSize: '12px', color: 'var(--wine)', textDecoration: 'none' }}
            >
              Forgot password?
            </Link>
          </div>
          <TextField
            type="password"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <Button type="submit" variant="primary" size="lg" isLoading={loading} endIcon={<ArrowRight size={16} />}>
          Sign In
        </Button>

        <div style={{ textAlign: 'center', fontSize: '13px', color: 'var(--ink-muted)', marginTop: '4px' }}>
          New to D &amp; Z Fashion?{' '}
          <Link href={`/signup?redirect=${encodeURIComponent(redirectUrl)}`} style={{ color: 'var(--wine)', fontWeight: 600 }}>
            Create an Account
          </Link>
        </div>
      </form>

      {/* Demo Quick Sign-in Section */}
      <div
        style={{
          marginTop: '32px',
          paddingTop: '20px',
          borderTop: '1px solid var(--line)',
          textAlign: 'center',
        }}
      >
        <span style={{ fontSize: '11px', color: 'var(--ink-subtle)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          Fast Evaluation &bull; Demo Roles
        </span>
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', marginTop: '10px' }}>
          <button
            type="button"
            onClick={() => handleDemoLogin('CUSTOMER')}
            className="btn btn-secondary btn-sm"
            style={{ fontSize: '12px' }}
          >
            <UserCheck size={14} style={{ marginRight: '4px' }} />
            Priya (Customer)
          </button>
          <button
            type="button"
            onClick={() => handleDemoLogin('OWNER')}
            className="btn btn-secondary btn-sm"
            style={{ fontSize: '12px' }}
          >
            Owner
          </button>
          <button
            type="button"
            onClick={() => handleDemoLogin('MANAGER')}
            className="btn btn-secondary btn-sm"
            style={{ fontSize: '12px' }}
          >
            Manager
          </button>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div>Loading sign in...</div>}>
      <LoginFormContent />
    </Suspense>
  );
}
