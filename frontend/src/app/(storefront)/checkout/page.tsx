'use client';

import React, { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  customerApi,
  CheckoutPreviewResponse,
  PaymentMethod,
  StoreCreditBalanceResponse,
} from '@/lib/api/customer';
import { TextField } from '@/components/ui/TextField';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { ArrowLeft, CheckCircle2, ShieldCheck, Tag, Wallet, CreditCard, Banknote } from 'lucide-react';

function CheckoutContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const confirmedPriceParam = searchParams.get('confirmedPrice') === 'true';

  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { toast } = useToast();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [confirmedOrderNumber, setConfirmedOrderNumber] = useState<string | null>(null);

  // Single saved address state
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');

  // Payment, Coupon, Store Credit
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('SIMULATED_ONLINE');
  const [couponCode, setCouponCode] = useState('');
  const [couponInput, setCouponInput] = useState('');
  const [storeCreditBalance, setStoreCreditBalance] = useState<StoreCreditBalanceResponse | null>(null);
  const [storeCreditToUse, setStoreCreditToUse] = useState<number>(0);
  const [preview, setPreview] = useState<CheckoutPreviewResponse | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  // Load profile and store credit
  useEffect(() => {
    if (!isAuthenticated) {
      if (!authLoading) {
        router.push('/login?redirect=/checkout');
      }
      return;
    }

    let isMounted = true;
    Promise.all([
      customerApi.getProfile().catch(() => null),
      customerApi.getStoreCreditBalance().catch(() => null),
    ]).then(([profileRes, creditRes]) => {
      if (!isMounted) return;
      if (profileRes && profileRes.data) {
        const p = profileRes.data;
        setRecipientName(p.fullName || '');
        setRecipientPhone(p.phoneNumber || '');
        setAddressLine1(p.addressLine1 || '');
        setAddressLine2(p.addressLine2 || '');
        setCity(p.city || '');
        setState(p.state || '');
        setPostalCode(p.postalCode || '');
      }
      if (creditRes && creditRes.data) {
        setStoreCreditBalance(creditRes.data);
      }
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, authLoading, router]);

  // Re-preview on payment method, coupon, store credit or postalCode change
  useEffect(() => {
    if (loading || !isAuthenticated) return;

    let isMounted = true;
    customerApi.previewCheckout({
      paymentMethod,
      couponCode: couponCode || undefined,
      storeCreditToUse: storeCreditToUse > 0 ? storeCreditToUse : undefined,
      postalCode: postalCode.trim() || undefined,
    })
      .then((res) => {
        if (isMounted && res.data) {
          setPreview(res.data);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = (err as { message?: string })?.message || 'Unable to calculate checkout totals';
          toast({ type: 'error', title: 'Calculation Error', message: msg });
        }
      })
      .finally(() => {
        if (isMounted) setPreviewLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [loading, isAuthenticated, paymentMethod, couponCode, storeCreditToUse, postalCode, toast]);

  // Apply Coupon
  const handleApplyCoupon = (e: React.FormEvent) => {
    e.preventDefault();
    if (!couponInput.trim()) return;
    setCouponCode(couponInput.trim().toUpperCase());
    toast({ type: 'info', title: 'Coupon Applied', message: `Evaluating promo code ${couponInput.trim().toUpperCase()}...` });
  };

  // Confirm and Place Order
  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!recipientName.trim() || !recipientPhone.trim() || !addressLine1.trim() || !city.trim() || !state.trim() || !postalCode.trim()) {
      toast({ type: 'warning', title: 'Incomplete Address', message: 'Please provide full delivery address and recipient details.' });
      return;
    }

    if (paymentMethod === 'CASH_ON_DELIVERY' && preview && !preview.codServiceable) {
      toast({ type: 'error', title: 'COD Unavailable', message: 'Cash on Delivery is not serviceable for this PIN code.' });
      return;
    }

    setSubmitting(true);
    try {
      const idempotencyKey = 'dz-ord-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
      const res = await customerApi.confirmOrder(
        {
          paymentMethod,
          couponCode: couponCode || undefined,
          storeCreditToUse: storeCreditToUse > 0 ? storeCreditToUse : undefined,
          confirmPriceChanges: confirmedPriceParam || (preview?.priceConfirmationRequired ?? false),
          recipientName: recipientName.trim(),
          recipientPhone: recipientPhone.trim(),
          shippingAddressLine1: addressLine1.trim(),
          shippingAddressLine2: addressLine2.trim() || undefined,
          shippingCity: city.trim(),
          shippingState: state.trim(),
          shippingPostalCode: postalCode.trim(),
          shippingCountry: 'India',
        },
        idempotencyKey
      );

      if (res.data) {
        setConfirmedOrderNumber(res.data.orderNumber);
        toast({
          type: 'success',
          title: 'Order Confirmed',
          message: `Order #${res.data.orderNumber} successfully placed.`,
        });
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Order placement failed';
      toast({ type: 'error', title: 'Checkout Failed', message: msg });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || authLoading) {
    return (
      <div style={{ maxWidth: 'var(--container-md)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 48px)' }}>
        <Skeleton height="32px" width="240px" style={{ marginBottom: '24px' }} />
        <Skeleton height="300px" style={{ marginBottom: '24px' }} />
        <Skeleton height="200px" />
      </div>
    );
  }

  // Order Confirmed State
  if (confirmedOrderNumber) {
    return (
      <div style={{ maxWidth: '640px', margin: '40px auto', padding: '0 20px', textAlign: 'center' }}>
        <div style={{ color: 'var(--color-success)', marginBottom: '16px' }}>
          <CheckCircle2 size={56} />
        </div>
        <span className="eyebrow">Order Placed Successfully</span>
        <h1 className="font-serif" style={{ margin: '8px 0 16px', fontSize: 'var(--text-display-sm)', fontWeight: 400 }}>
          Thank You For Your Order
        </h1>
        <p style={{ margin: '0 0 24px', fontSize: '15px', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
          Your order number is <strong style={{ color: 'var(--ink)' }}>{confirmedOrderNumber}</strong>. We have received your order and our Surat warehouse team will prepare it for carrier handover.
        </p>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <Button
            variant="primary"
            size="md"
            onClick={() => router.push(`/account/orders/${confirmedOrderNumber}`)}
          >
            Track Order Details &rarr;
          </Button>
          <Button
            variant="secondary"
            size="md"
            onClick={() => router.push('/')}
          >
            Return to Store
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 'var(--container-lg)', margin: '0 auto', padding: '36px clamp(20px, 4vw, 48px) 80px' }}>
      <Link
        href="/cart"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '12px',
          color: 'var(--ink-muted)',
          marginBottom: '24px',
          textDecoration: 'none',
        }}
      >
        <ArrowLeft size={14} />
        <span>Return to Shopping Bag</span>
      </Link>

      <h1 className="font-serif" style={{ margin: '0 0 32px', fontSize: 'var(--text-display-sm)', fontWeight: 400 }}>
        Checkout &bull; Surat Dispatch
      </h1>

      <form onSubmit={handlePlaceOrder}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(340px, 1.2fr) minmax(320px, 0.8fr)',
            gap: '40px',
            alignItems: 'start',
          }}
        >
          {/* Left Column: Delivery Address & Payment Method */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
            {/* Delivery Address Section (Requirement 8) */}
            <div
              style={{
                padding: '24px',
                backgroundColor: '#ffffff',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h2 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-sm)' }}>
                  Delivery Address &amp; Recipient
                </h2>
                <span style={{ fontSize: '12px', color: 'var(--ink-subtle)' }}>
                  Single Saved Address
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
                <TextField
                  label="Recipient Full Name"
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  required
                />
                <TextField
                  label="Contact Phone"
                  value={recipientPhone}
                  onChange={(e) => setRecipientPhone(e.target.value)}
                  placeholder="10-digit mobile number"
                  required
                />
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <TextField
                  label="Address Line 1"
                  value={addressLine1}
                  onChange={(e) => setAddressLine1(e.target.value)}
                  placeholder="House/Flat No., Building Name, Street"
                  required
                />
                <TextField
                  label="Address Line 2 (Optional)"
                  value={addressLine2}
                  onChange={(e) => setAddressLine2(e.target.value)}
                  placeholder="Landmark, Area, Sector"
                />

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
                  <TextField
                    label="City"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    required
                  />
                  <TextField
                    label="State"
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    required
                  />
                  <TextField
                    label="PIN Code"
                    value={postalCode}
                    onChange={(e) => setPostalCode(e.target.value)}
                    placeholder="6 digits"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Payment Method Selection (Requirement 10) */}
            <div
              style={{
                padding: '24px',
                backgroundColor: '#ffffff',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <h2 className="font-serif" style={{ margin: '0 0 16px', fontSize: 'var(--text-heading-sm)' }}>
                Payment Method
              </h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {/* Simulated Online Payment */}
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '12px',
                    padding: '16px',
                    border: `1px solid ${paymentMethod === 'SIMULATED_ONLINE' ? 'var(--wine)' : 'var(--line)'}`,
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: paymentMethod === 'SIMULATED_ONLINE' ? 'var(--paper-warm)' : '#ffffff',
                    cursor: 'pointer',
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="SIMULATED_ONLINE"
                    checked={paymentMethod === 'SIMULATED_ONLINE'}
                    onChange={() => setPaymentMethod('SIMULATED_ONLINE')}
                    style={{ marginTop: '3px' }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600, fontSize: '14px' }}>
                      <CreditCard size={16} color="var(--wine)" />
                      <span>Simulated Online Payment (Test / Demo)</span>
                    </div>
                    <p style={{ margin: '4px 0 0', fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.4 }}>
                      Simulated payment for instant order confirmation. Zero card, CVV, or bank numbers required.
                    </p>
                  </div>
                </label>

                {/* Cash on Delivery (COD) */}
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '12px',
                    padding: '16px',
                    border: `1px solid ${paymentMethod === 'CASH_ON_DELIVERY' ? 'var(--wine)' : 'var(--line)'}`,
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: paymentMethod === 'CASH_ON_DELIVERY' ? 'var(--paper-warm)' : '#ffffff',
                    cursor: preview?.codServiceable ? 'pointer' : 'not-allowed',
                    opacity: preview?.codServiceable ? 1 : 0.6,
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="CASH_ON_DELIVERY"
                    disabled={!preview?.codServiceable}
                    checked={paymentMethod === 'CASH_ON_DELIVERY'}
                    onChange={() => setPaymentMethod('CASH_ON_DELIVERY')}
                    style={{ marginTop: '3px' }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600, fontSize: '14px' }}>
                      <Banknote size={16} color="var(--color-success)" />
                      <span>Cash on Delivery (COD)</span>
                    </div>
                    <p style={{ margin: '4px 0 0', fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.4 }}>
                      {preview?.codServiceable
                        ? 'Pay cash upon parcel delivery at your doorstep.'
                        : 'COD is currently not available for this PIN code.'}
                    </p>
                  </div>
                </label>
              </div>
            </div>
          </div>

          {/* Right Column: Server-Authoritative Totals Breakdown (Requirement 9) */}
          <aside
            style={{
              padding: '24px',
              backgroundColor: '#ffffff',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
              display: 'flex',
              flexDirection: 'column',
              gap: '20px',
            }}
          >
            <h2 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-sm)' }}>
              Authoritative Price Breakdown
            </h2>

            {/* Coupon Code Section */}
            <div>
              <label className="eyebrow" style={{ display: 'block', marginBottom: '6px' }}>
                Promotional Coupon
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  type="text"
                  placeholder="e.g. FESTIVE10"
                  value={couponInput}
                  onChange={(e) => setCouponInput(e.target.value)}
                  className="form-input"
                  style={{ textTransform: 'uppercase', height: '38px', fontSize: '13px' }}
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleApplyCoupon}
                >
                  Apply
                </Button>
              </div>
              {couponCode && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: 'var(--color-success)', marginTop: '4px' }}>
                  <Tag size={12} />
                  <span>Code <strong>{couponCode}</strong> evaluated</span>
                </div>
              )}
            </div>

            {/* Store Credit Redemption Section (Requirement 14) */}
            {storeCreditBalance && storeCreditBalance.currentBalance > 0 && (
              <div
                style={{
                  padding: '12px',
                  backgroundColor: 'var(--paper-warm)',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--line)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
                  <Wallet size={16} color="var(--wine)" />
                  <strong style={{ fontSize: '13px' }}>
                    Store Credit: ₹{storeCreditBalance.currentBalance.toLocaleString('en-IN')}
                  </strong>
                </div>
                <p style={{ margin: '0 0 8px', fontSize: '11px', color: 'var(--ink-subtle)' }}>
                  Non-expiring wallet credit. Enter amount to redeem towards this order:
                </p>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    type="number"
                    min={0}
                    max={storeCreditBalance.currentBalance}
                    value={storeCreditToUse || ''}
                    onChange={(e) => setStoreCreditToUse(Math.min(Number(e.target.value) || 0, storeCreditBalance.currentBalance))}
                    placeholder="₹ to use"
                    className="form-input"
                    style={{ height: '36px', fontSize: '13px' }}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setStoreCreditToUse(storeCreditBalance.currentBalance)}
                    style={{ fontSize: '11px', padding: '0 8px' }}
                  >
                    Max
                  </Button>
                </div>
              </div>
            )}

            {/* Line Items Breakdown */}
            <div style={{ borderTop: '1px solid var(--line)', paddingTop: '16px', display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '14px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Merchandise Subtotal</span>
                <span style={{ fontFamily: 'var(--font-mono)' }}>
                  ₹{preview ? preview.subtotalBeforeDiscountAndTax.toLocaleString('en-IN') : '...'}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>
                  Delivery Charge ({preview?.freeDeliveryEligible ? 'Free over ₹2,000' : `${preview?.totalPhysicalUnits || 1} units @ ₹110`})
                </span>
                <span style={{ fontFamily: 'var(--font-mono)' }}>
                  {preview?.freeDeliveryEligible ? 'FREE' : `₹${preview?.deliveryCharge || 0}`}
                </span>
              </div>

              {preview && preview.promotionalDiscountAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--color-success)' }}>
                  <span>Promo Discount ({preview.appliedCouponCode})</span>
                  <span style={{ fontFamily: 'var(--font-mono)' }}>
                    -₹{preview.promotionalDiscountAmount.toLocaleString('en-IN')}
                  </span>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>
                  GST Tax ({preview?.gstRatePercent || 12}%)
                </span>
                <span style={{ fontFamily: 'var(--font-mono)' }}>
                  ₹{preview ? preview.gstAmount.toLocaleString('en-IN') : '...'}
                </span>
              </div>

              {preview && preview.storeCreditUsed > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--wine)' }}>
                  <span>Store Credit Applied</span>
                  <span style={{ fontFamily: 'var(--font-mono)' }}>
                    -₹{preview.storeCreditUsed.toLocaleString('en-IN')}
                  </span>
                </div>
              )}

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingTop: '12px',
                  borderTop: '1px solid var(--line)',
                  fontSize: '18px',
                  fontWeight: 600,
                  color: 'var(--ink)',
                }}
              >
                <span>Total Payable</span>
                <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--wine)' }}>
                  ₹{preview ? preview.finalPayableAmount.toLocaleString('en-IN') : '...'}
                </span>
              </div>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={submitting || previewLoading}
              style={{ width: '100%', marginTop: '8px' }}
            >
              Place Order &bull; ₹{preview ? preview.finalPayableAmount.toLocaleString('en-IN') : '...'}
            </Button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '11px', color: 'var(--ink-subtle)', justifyContent: 'center' }}>
              <ShieldCheck size={14} color="var(--color-success)" />
              <span>Idempotent Transaction &bull; Surat Atelier Ready Stock</span>
            </div>
          </aside>
        </div>
      </form>
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <Suspense fallback={<div>Loading checkout...</div>}>
      <CheckoutContent />
    </Suspense>
  );
}
