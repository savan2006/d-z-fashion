'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { customerApi } from '@/lib/api/customer';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { CheckCircle2, ArrowRight } from 'lucide-react';

type Step = 'EMAIL' | 'OTP' | 'PASSWORD' | 'SUCCESS';

export default function ForgotPasswordPage() {
  const router = useRouter();
  const { toast } = useToast();

  const [step, setStep] = useState<Step>('EMAIL');
  const [loading, setLoading] = useState(false);

  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [otpToken, setOtpToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Step 1: Request OTP
  const handleInitiate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    setLoading(true);
    try {
      await customerApi.initiateForgotPassword(email.trim());
      setStep('OTP');
      toast({
        type: 'success',
        title: 'OTP Dispatched',
        message: `A 4-digit reset code was sent to ${email.trim()}.`,
      });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to request password reset';
      toast({ type: 'error', title: 'Reset Error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify OTP
  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 4) return;

    setLoading(true);
    try {
      const res = await customerApi.verifyForgotPasswordOtp(email.trim(), otp.trim());
      setOtpToken(res.data.otpToken);
      setStep('PASSWORD');
      toast({ type: 'success', title: 'Code Verified', message: 'Enter your new password.' });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Invalid or expired OTP';
      toast({ type: 'error', title: 'Verification Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Complete Reset
  const handleComplete = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) {
      toast({ type: 'warning', title: 'Weak Password', message: 'Password must be at least 8 characters.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      toast({ type: 'error', title: 'Mismatch', message: 'Passwords do not match.' });
      return;
    }

    setLoading(true);
    try {
      await customerApi.completeForgotPassword(otpToken, newPassword, confirmPassword);
      setStep('SUCCESS');
      toast({ type: 'success', title: 'Password Reset', message: 'Your password has been updated.' });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to reset password';
      toast({ type: 'error', title: 'Reset Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {step === 'EMAIL' && (
        <form onSubmit={handleInitiate} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">Password Recovery</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Reset Your Password
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Enter your account email to receive a 4-digit verification code.
            </p>
          </div>

          <TextField
            label="Email Address"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />

          <Button type="submit" variant="primary" size="lg" isLoading={loading} endIcon={<ArrowRight size={16} />}>
            Send Reset Code
          </Button>

          <div style={{ textAlign: 'center', fontSize: '13px', marginTop: '4px' }}>
            <Link href="/login" style={{ color: 'var(--wine)', textDecoration: 'none' }}>
              &larr; Back to Sign In
            </Link>
          </div>
        </form>
      )}

      {step === 'OTP' && (
        <form onSubmit={handleVerifyOtp} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">Verification</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Enter 4-Digit Code
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Enter the code sent to <strong>{email}</strong>.
            </p>
          </div>

          <TextField
            label="4-Digit Code"
            placeholder="1234"
            maxLength={4}
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
            style={{ fontSize: '24px', letterSpacing: '8px', textAlign: 'center', fontFamily: 'var(--font-mono)' }}
            required
            autoFocus
          />

          <Button type="submit" variant="primary" size="lg" isLoading={loading} disabled={otp.length !== 4}>
            Verify Code
          </Button>

          <button
            type="button"
            onClick={() => setStep('EMAIL')}
            className="btn btn-ghost"
            style={{ fontSize: '12px', color: 'var(--ink-muted)' }}
          >
            &larr; Change email address
          </button>
        </form>
      )}

      {step === 'PASSWORD' && (
        <form onSubmit={handleComplete} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">New Password</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Set New Password
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Must be at least 8 characters.
            </p>
          </div>

          <TextField
            label="New Password"
            type="password"
            placeholder="At least 8 characters"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            autoFocus
          />

          <TextField
            label="Confirm New Password"
            type="password"
            placeholder="Re-enter password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />

          <Button type="submit" variant="primary" size="lg" isLoading={loading}>
            Save New Password
          </Button>
        </form>
      )}

      {step === 'SUCCESS' && (
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
          <div style={{ color: 'var(--color-success)', marginTop: '8px' }}>
            <CheckCircle2 size={48} />
          </div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-xl)' }}>
            Password Reset Complete
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: 'var(--ink-muted)' }}>
            Your password has been successfully updated. You can now sign in with your new credentials.
          </p>
          <div style={{ width: '100%', marginTop: '16px' }}>
            <Button
              type="button"
              variant="primary"
              size="lg"
              style={{ width: '100%' }}
              onClick={() => router.push('/login')}
            >
              Go to Sign In &rarr;
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
