'use client';

import React, { useEffect, useState } from 'react';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  customerApi,
  UpdateProfileRequest,
} from '@/lib/api/customer';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import {
  MapPin,
  User,
  ShieldCheck,
} from 'lucide-react';

export default function ProfileAndAddressPage() {
  const { user } = useAuth();
  const { toast } = useToast();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Form State
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [country, setCountry] = useState('India');

  useEffect(() => {
    let isMounted = true;
    customerApi.getProfile()
      .then((res) => {
        if (isMounted && res.data) {
          const p = res.data;
          setFullName(p.fullName || user?.fullName || '');
          setPhoneNumber(p.phoneNumber || '');
          setAddressLine1(p.addressLine1 || '');
          setAddressLine2(p.addressLine2 || '');
          setCity(p.city || '');
          setState(p.state || '');
          setPostalCode(p.postalCode || '');
          setCountry(p.country || 'India');
        }
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [user]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!fullName.trim()) {
      toast({ type: 'warning', title: 'Name Required', message: 'Please enter your full name.' });
      return;
    }

    if (phoneNumber.trim() && !/^\d{10}$/.test(phoneNumber.trim().replace(/\D/g, ''))) {
      toast({ type: 'warning', title: 'Invalid Phone', message: 'Please enter a valid 10-digit mobile number.' });
      return;
    }

    if (postalCode.trim() && !/^\d{6}$/.test(postalCode.trim())) {
      toast({ type: 'warning', title: 'Invalid PIN Code', message: 'Indian PIN code must be exactly 6 digits.' });
      return;
    }

    setSaving(true);
    try {
      const payload: UpdateProfileRequest = {
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim() || undefined,
        addressLine1: addressLine1.trim() || undefined,
        addressLine2: addressLine2.trim() || undefined,
        city: city.trim() || undefined,
        state: state.trim() || undefined,
        postalCode: postalCode.trim() || undefined,
        country: country.trim() || 'India',
      };

      const res = await customerApi.updateProfile(payload);
      if (res.data) {
        toast({
          type: 'success',
          title: 'Profile Updated',
          message: 'Your personal details and primary delivery address were saved successfully.',
        });
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to update profile';
      toast({ type: 'error', title: 'Update Error', message: msg });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <Skeleton height="100px" />
        <Skeleton height="300px" />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Saved Address &amp; Profile
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Manage your account credentials and primary delivery address.
        </p>
      </div>

      {/* Single Saved Address Info Notice */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '13px',
          color: 'var(--ink-secondary)',
        }}
      >
        <ShieldCheck size={20} color="var(--wine)" />
        <div>
          <strong>Single Saved Delivery Address:</strong> D&amp;Z Fashion maintains one verified primary address per profile to avoid parcel misdirection and enable seamless express checkout.
        </div>
      </div>

      {/* Main Form */}
      <form
        onSubmit={handleSave}
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          padding: '28px',
          display: 'flex',
          flexDirection: 'column',
          gap: '24px',
        }}
      >
        {/* Personal Details */}
        <div>
          <h2 style={{ margin: '0 0 16px', fontSize: '15px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <User size={16} color="var(--wine)" />
            Personal Details
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
            <TextField
              label="Full Name"
              placeholder="e.g. Priya Patel"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              required
            />

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Email Address
              </label>
              <input
                type="email"
                className="form-input"
                value={user?.email || ''}
                disabled
                style={{ backgroundColor: 'var(--surface-subtle)', cursor: 'not-allowed' }}
              />
              <span style={{ fontSize: '11px', color: 'var(--ink-muted)', marginTop: '4px', display: 'block' }}>
                Email is verified and cannot be changed here.
              </span>
            </div>

            <TextField
              label="Mobile Number (+91)"
              placeholder="10-digit mobile number"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              helperText="Used for courier delivery alerts & SMS OTP"
            />
          </div>
        </div>

        {/* Primary Delivery Address */}
        <div style={{ borderTop: '1px solid var(--line-light)', paddingTop: '20px' }}>
          <h2 style={{ margin: '0 0 16px', fontSize: '15px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MapPin size={16} color="var(--wine)" />
            Primary Delivery Destination
          </h2>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="Address Line 1"
              placeholder="Flat, House no., Building, Apartment"
              value={addressLine1}
              onChange={(e) => setAddressLine1(e.target.value)}
              helperText="Street address for delivery courier"
            />

            <TextField
              label="Address Line 2 (Optional)"
              placeholder="Area, Street, Landmark, Sector"
              value={addressLine2}
              onChange={(e) => setAddressLine2(e.target.value)}
            />

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px' }}>
              <TextField
                label="City / Town"
                placeholder="e.g. Surat, Mumbai, Ahmedabad"
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />

              <TextField
                label="State"
                placeholder="e.g. Gujarat, Maharashtra"
                value={state}
                onChange={(e) => setState(e.target.value)}
              />

              <TextField
                label="PIN Code"
                placeholder="6-digit PIN code"
                value={postalCode}
                onChange={(e) => setPostalCode(e.target.value)}
                maxLength={6}
              />

              <TextField
                label="Country"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                disabled
              />
            </div>
          </div>
        </div>

        {/* Submit Button */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid var(--line-light)', paddingTop: '16px' }}>
          <Button type="submit" variant="primary" isLoading={saving}>
            Save Changes
          </Button>
        </div>
      </form>
    </div>
  );
}
