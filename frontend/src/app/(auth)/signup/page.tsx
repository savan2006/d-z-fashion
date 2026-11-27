'use client';

import React, { Suspense, useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth/AuthContext';
import { customerApi } from '@/lib/api/customer';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { CheckCircle2, ArrowRight, RotateCcw } from 'lucide-react';

type SignupStep = 'DETAILS' | 'OTP' | 'PASSWORD' | 'SUCCESS';

function SignupFormContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect') || '/';
  const { login } = useAuth();
  const { toast } = useToast();

  const [step, setStep] = useState<SignupStep>('DETAILS');
  const [loading, setLoading] = useState(false);

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [otpToken, setOtpToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Countdown timer for OTP resend
  const [countdown, setCountdown] = useState(60);
  const canResend = countdown === 0;

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (step === 'OTP' && countdown > 0) {
      timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [step, countdown]);

  // Step 1: Initiate Signup
  const handleInitiateSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !fullName.trim() || !username.trim()) {
      toast({ type: 'warning', title: 'Missing fields', message: 'Please fill in all fields.' });
      return;
    }

    setLoading(true);
    try {
      const res = await customerApi.initiateSignup(email.trim(), fullName.trim(), username.trim());
      setCountdown(res.data.expiresInSeconds || 60);
      setStep('OTP');
      toast({
        type: 'success',
        title: 'OTP Dispatched',
        message: `A 4-digit code was sent to ${email.trim()}.`,
      });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to initiate registration';
      toast({ type: 'error', title: 'Registration Error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify OTP
  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length !== 4) {
      toast({ type: 'warning', title: 'Invalid code', message: 'Please enter the 4-digit OTP.' });
      return;
    }

    setLoading(true);
    try {
      const res = await customerApi.verifySignupOtp(email.trim(), otp.trim());
      setOtpToken(res.data.verificationTicket || res.data.otpToken);
      setStep('PASSWORD');
      toast({ type: 'success', title: 'Email Verified', message: 'Please set your account password.' });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Invalid or expired OTP';
      toast({ type: 'error', title: 'Verification Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Resend OTP
  const handleResendOtp = async () => {
    if (!canResend) return;
    setLoading(true);
    try {
      const res = await customerApi.initiateSignup(email.trim(), fullName.trim(), username.trim());
      setCountdown(res.data.expiresInSeconds || 60);
      setOtp('');
      toast({ type: 'info', title: 'Code Resent', message: 'A new 4-digit code has been sent.' });
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Unable to resend OTP';
      toast({ type: 'error', title: 'Resend Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Complete Signup
  const handleCompleteSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 8) {
      toast({ type: 'warning', title: 'Weak Password', message: 'Password must be at least 8 characters.' });
      return;
    }
    if (password !== confirmPassword) {
      toast({ type: 'error', title: 'Passwords Mismatch', message: 'Passwords do not match.' });
      return;
    }

    setLoading(true);
    try {
      const res = await customerApi.completeSignup(
        otpToken,
        password,
        confirmPassword,
        username.trim(),
        email.trim(),
        fullName.trim()
      );
      if (res.data) {
        // Automatically log user in
        login(res.data.accessToken, res.data.user);
        setStep('SUCCESS');
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Could not complete registration';
      toast({ type: 'error', title: 'Registration Failed', message: msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Step 1: Details */}
      {step === 'DETAILS' && (
        <form onSubmit={handleInitiateSignup} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">Customer Registration &bull; Step 1 of 3</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Create Your Account
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Join D &amp; Z Fashion to save your delivery address, track orders, and browse Surat ready stock.
            </p>
          </div>

          <TextField
            label="Full Name"
            placeholder="e.g. Priya Patel"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            autoFocus
          />

          <TextField
            label="Username"
            placeholder="Choose a username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <TextField
            label="Email Address"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            helperText="We will send a 4-digit verification code to this email."
            required
          />

          <Button type="submit" variant="primary" size="lg" isLoading={loading} endIcon={<ArrowRight size={16} />}>
            Send 4-Digit OTP
          </Button>

          <div style={{ textAlign: 'center', fontSize: '13px', color: 'var(--ink-muted)', marginTop: '8px' }}>
            Already have an account?{' '}
            <Link href={`/login?redirect=${encodeURIComponent(redirectUrl)}`} style={{ color: 'var(--wine)', fontWeight: 600 }}>
              Sign In
            </Link>
          </div>
        </form>
      )}

      {/* Step 2: 4-Digit OTP */}
      {step === 'OTP' && (
        <form onSubmit={handleVerifyOtp} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">Verification &bull; Step 2 of 3</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Enter 4-Digit Code
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Sent to <strong>{email}</strong>. Check your inbox or spam folder.
            </p>
          </div>

          <TextField
            label="4-Digit OTP Code"
            placeholder="1234"
            maxLength={4}
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
            style={{
              fontSize: '24px',
              letterSpacing: '8px',
              textAlign: 'center',
              fontFamily: 'var(--font-mono)',
            }}
            required
            autoFocus
          />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '13px' }}>
            <span style={{ color: 'var(--ink-subtle)' }}>
              {countdown > 0 ? `Code expires in ${countdown}s` : 'Code expired'}
            </span>
            <button
              type="button"
              disabled={!canResend || loading}
              onClick={handleResendOtp}
              className="btn btn-ghost"
              style={{ padding: '4px 8px', minHeight: 'auto', fontSize: '12px', color: canResend ? 'var(--wine)' : 'var(--ink-subtle)' }}
            >
              <RotateCcw size={12} style={{ marginRight: '4px' }} />
              Resend Code
            </button>
          </div>

          <Button type="submit" variant="primary" size="lg" isLoading={loading} disabled={otp.length !== 4}>
            Verify Code
          </Button>

          <button
            type="button"
            onClick={() => setStep('DETAILS')}
            className="btn btn-ghost"
            style={{ fontSize: '12px', color: 'var(--ink-muted)' }}
          >
            &larr; Change email or details
          </button>
        </form>
      )}

      {/* Step 3: Password & Confirm Password */}
      {step === 'PASSWORD' && (
        <form onSubmit={handleCompleteSignup} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <span className="eyebrow">Security &bull; Step 3 of 3</span>
            <h1 className="font-serif" style={{ margin: '4px 0 8px', fontSize: 'var(--text-heading-lg)', fontWeight: 400 }}>
              Set Your Password
            </h1>
            <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
              Choose a secure password for your D &amp; Z Fashion account.
            </p>
          </div>

          <TextField
            label="Password"
            type="password"
            placeholder="At least 8 characters"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            helperText="Minimum 8 characters with at least one letter and one number."
            required
            autoFocus
          />

          <TextField
            label="Confirm Password"
            type="password"
            placeholder="Re-enter your password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />

          <Button type="submit" variant="primary" size="lg" isLoading={loading}>
            Complete Registration
          </Button>
        </form>
      )}

      {/* Step 4: Success Result */}
      {step === 'SUCCESS' && (
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
          <div style={{ color: 'var(--color-success)', marginTop: '8px' }}>
            <CheckCircle2 size={48} />
          </div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-xl)' }}>
            Welcome to D &amp; Z Fashion
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
            Your account has been verified and created. You are now logged in and ready to shop Surat ready stock.
          </p>

          <div style={{ width: '100%', marginTop: '16px' }}>
            <Button
              type="button"
              variant="primary"
              size="lg"
              style={{ width: '100%' }}
              onClick={() => router.push(redirectUrl)}
            >
              Continue to {redirectUrl === '/' ? 'Storefront' : 'Checkout'} &rarr;
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function SignupPage() {
  return (
    <Suspense fallback={<div>Loading signup...</div>}>
      <SignupFormContent />
    </Suspense>
  );
}
