'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ShoppingBag, ArrowLeft, Trash2, AlertTriangle, ArrowRight, Truck } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { customerApi, CartResponse } from '@/lib/api/customer';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/components/ui/Toast';

export default function CartPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const { toast } = useToast();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [acceptedPriceChanges, setAcceptedPriceChanges] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return;

    let isMounted = true;
    customerApi.getCart()
      .then((res) => {
        if (isMounted && res.data) {
          setCart(res.data);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = (err as { message?: string })?.message || 'Failed to load shopping bag';
          toast({ type: 'error', title: 'Bag Error', message: msg });
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, toast]);

  const handleUpdateQuantity = async (itemId: string, newQty: number) => {
    if (newQty < 1) return;
    setUpdatingId(itemId);
    try {
      const res = await customerApi.updateCartItemQuantity(itemId, newQty);
      if (res.data) {
        setCart(res.data);
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to update quantity';
      toast({ type: 'error', title: 'Update Failed', message: msg });
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    setUpdatingId(itemId);
    try {
      const res = await customerApi.removeCartItem(itemId);
      if (res.data) {
        setCart(res.data);
        toast({ type: 'info', title: 'Item Removed', message: 'Garment removed from your shopping bag.' });
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to remove item';
      toast({ type: 'error', title: 'Remove Failed', message: msg });
    } finally {
      setUpdatingId(null);
    }
  };

  if (authLoading || (isAuthenticated && loading)) {
    return (
      <div style={{ maxWidth: 'var(--container-md)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 48px)' }}>
        <Skeleton height="32px" width="200px" style={{ marginBottom: '24px' }} />
        <Skeleton height="160px" style={{ marginBottom: '16px' }} />
        <Skeleton height="160px" />
      </div>
    );
  }

  // Unauthenticated user prompt
  if (!isAuthenticated) {
    return (
      <div style={{ maxWidth: 'var(--container-md)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 48px)' }}>
        <EmptyState
          icon={<ShoppingBag size={32} strokeWidth={1.5} />}
          title="Sign in to view your shopping bag"
          description="Your bag is saved to your account. Sign in or register to add garments and proceed to checkout."
          action={
            <div style={{ display: 'flex', gap: '12px' }}>
              <Button
                variant="primary"
                size="md"
                onClick={() => router.push('/login?redirect=/cart')}
              >
                Sign In
              </Button>
              <Button
                variant="outline"
                size="md"
                onClick={() => router.push('/signup?redirect=/cart')}
              >
                Create Account
              </Button>
            </div>
          }
        />
      </div>
    );
  }

  const items = cart?.items || [];
  const subtotal = cart?.subtotal || 0;
  const isFreeDeliveryEligible = subtotal > 2000;
  const amountToFreeDelivery = Math.max(0, 2000.01 - subtotal);
  const hasPriceChanges = cart?.priceConfirmationRequired || items.some((i) => i.priceChanged);

  if (items.length === 0) {
    return (
      <div style={{ maxWidth: 'var(--container-md)', margin: '0 auto', padding: '48px clamp(20px, 4vw, 48px)' }}>
        <Link
          href="/"
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
          <span>Continue Shopping</span>
        </Link>

        <h1 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)', fontWeight: 400 }}>
          Shopping Bag
        </h1>

        <EmptyState
          icon={<ShoppingBag size={32} strokeWidth={1.5} />}
          title="Your shopping bag is empty"
          description="Thoughtful garments crafted in Surat with quality ready stock. Explore our collections and find pieces that feel like your own."
          action={
            <Button variant="primary" size="md" onClick={() => router.push('/collections')}>
              Discover Collections
            </Button>
          }
        />
      </div>
    );
  }

  const formattedSubtotal = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(subtotal);

  return (
    <div style={{ maxWidth: 'var(--container-lg)', margin: '0 auto', padding: '40px clamp(20px, 4vw, 48px) 80px' }}>
      <Link
        href="/collections"
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
        <span>Continue Browsing</span>
      </Link>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '28px' }}>
        <h1 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-display-sm)', fontWeight: 400 }}>
          Shopping Bag ({cart?.totalItems} {cart?.totalItems === 1 ? 'item' : 'items'})
        </h1>
      </div>

      {/* Price Shift Alert Banner (Requirement 7) */}
      {hasPriceChanges && (
        <div
          style={{
            padding: '16px 20px',
            backgroundColor: '#fffbeb',
            border: '1px solid #fde68a',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '28px',
            display: 'flex',
            gap: '12px',
            alignItems: 'flex-start',
          }}
        >
          <AlertTriangle size={20} color="var(--color-warning)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <strong style={{ fontSize: '14px', color: '#92400e', display: 'block', marginBottom: '4px' }}>
              Garment Price Shift Detected
            </strong>
            <p style={{ margin: '0 0 10px', fontSize: '13px', color: '#b45309', lineHeight: 1.5 }}>
              Prices for one or more garments in your bag have updated at our Surat workshop. Please review the updated prices below and accept the changes to proceed to checkout.
            </p>
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px', fontWeight: 600, color: '#78350f' }}>
              <input
                type="checkbox"
                checked={acceptedPriceChanges}
                onChange={(e) => setAcceptedPriceChanges(e.target.checked)}
              />
              <span>I acknowledge and accept the updated garment prices</span>
            </label>
          </div>
        </div>
      )}

      {/* Free Delivery Qualification Threshold Progress Bar */}
      <div
        style={{
          padding: '16px 20px',
          backgroundColor: isFreeDeliveryEligible ? '#ecfdf5' : 'var(--paper-warm)',
          border: `1px solid ${isFreeDeliveryEligible ? '#a7f3d0' : 'var(--line)'}`,
          borderRadius: 'var(--radius-sm)',
          marginBottom: '28px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }}
      >
        <Truck size={20} color={isFreeDeliveryEligible ? 'var(--color-success)' : 'var(--wine)'} />
        <span style={{ fontSize: '13px', color: 'var(--ink)' }}>
          {isFreeDeliveryEligible ? (
            <strong style={{ color: 'var(--color-success)' }}>
              Congratulations! Your order qualifies for Free Pan-India Delivery (pre-GST subtotal &gt; ₹2,000).
            </strong>
          ) : (
            <>
              Add <strong>₹{Math.ceil(amountToFreeDelivery)}</strong> more of pre-GST merchandise to qualify for{' '}
              <strong>Free Delivery</strong> (standard shipping: ₹110 per unit).
            </>
          )}
        </span>
      </div>

      {/* Two Column Layout: Items List & Order Summary */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(320px, 1fr) minmax(300px, 360px)',
          gap: '36px',
          alignItems: 'start',
        }}
      >
        {/* Cart Items List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {items.map((item) => {
            const isItemUpdating = updatingId === item.id;
            const currentItemTotal = new Intl.NumberFormat('en-IN', {
              style: 'currency',
              currency: 'INR',
              maximumFractionDigits: 0,
            }).format(item.itemTotal);

            const snapshotUnitPrice = new Intl.NumberFormat('en-IN', {
              style: 'currency',
              currency: 'INR',
              maximumFractionDigits: 0,
            }).format(item.priceSnapshot);

            const currentUnitPrice = new Intl.NumberFormat('en-IN', {
              style: 'currency',
              currency: 'INR',
              maximumFractionDigits: 0,
            }).format(item.currentPrice);

            return (
              <div
                key={item.id}
                style={{
                  padding: '20px',
                  backgroundColor: '#ffffff',
                  border: '1px solid var(--line)',
                  borderRadius: 'var(--radius-sm)',
                  display: 'flex',
                  gap: '16px',
                  opacity: isItemUpdating ? 0.6 : 1,
                  transition: 'opacity var(--transition-fast)',
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h3 style={{ margin: '0 0 4px', fontSize: '15px', fontWeight: 600 }}>
                        {item.productName}
                      </h3>
                      <p style={{ margin: '0 0 8px', fontSize: '12px', color: 'var(--ink-muted)' }}>
                        SKU: {item.sku} &bull; Size: <strong>{item.size}</strong> &bull; Colour: <strong>{item.colour}</strong>
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleRemoveItem(item.id)}
                      disabled={isItemUpdating}
                      className="btn btn-ghost"
                      style={{ padding: '4px', minHeight: 'auto', color: 'var(--ink-subtle)' }}
                      aria-label={`Remove ${item.productName} from bag`}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>

                  {/* Price Shift Details */}
                  {item.priceChanged && (
                    <div
                      style={{
                        padding: '6px 10px',
                        backgroundColor: '#fffbeb',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '12px',
                        marginBottom: '10px',
                        display: 'inline-block',
                      }}
                    >
                      Price changed: <del style={{ color: 'var(--ink-subtle)' }}>{snapshotUnitPrice}</del>{' '}
                      &rarr; <strong style={{ color: '#b45309' }}>{currentUnitPrice}</strong>
                    </div>
                  )}

                  {/* Quantity Stepper & Price Row */}
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginTop: '12px',
                      paddingTop: '12px',
                      borderTop: '1px solid var(--line)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}
                        disabled={item.quantity <= 1 || isItemUpdating}
                        className="btn btn-secondary btn-sm"
                        style={{ width: '28px', height: '28px', padding: 0, minHeight: 'auto' }}
                      >
                        -
                      </button>
                      <span style={{ fontSize: '14px', fontWeight: 600, minWidth: '24px', textAlign: 'center' }}>
                        {item.quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                        disabled={isItemUpdating}
                        className="btn btn-secondary btn-sm"
                        style={{ width: '28px', height: '28px', padding: 0, minHeight: 'auto' }}
                      >
                        +
                      </button>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <span style={{ fontSize: '15px', fontWeight: 600, color: 'var(--ink)', fontFamily: 'var(--font-mono)' }}>
                        {currentItemTotal}
                      </span>
                      <span style={{ fontSize: '11px', color: 'var(--ink-subtle)', display: 'block' }}>
                        excl. GST
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Order Summary Sidebar */}
        <aside
          style={{
            padding: '24px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
          }}
        >
          <h2 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-md)' }}>
            Bag Summary
          </h2>

          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '14px', borderBottom: '1px solid var(--line)', paddingBottom: '12px' }}>
            <span style={{ color: 'var(--ink-muted)' }}>Merchandise Subtotal (pre-GST)</span>
            <span style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{formattedSubtotal}</span>
          </div>

          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)', lineHeight: 1.5 }}>
            Delivery charge, tax classification (GST), and available store credit will be calculated server-side during checkout.
          </p>

          <Button
            variant="primary"
            size="lg"
            disabled={hasPriceChanges && !acceptedPriceChanges}
            onClick={() => router.push(`/checkout${hasPriceChanges ? '?confirmedPrice=true' : ''}`)}
            endIcon={<ArrowRight size={18} />}
            style={{ width: '100%', marginTop: '8px' }}
          >
            Proceed to Checkout
          </Button>

          {hasPriceChanges && !acceptedPriceChanges && (
            <span style={{ fontSize: '12px', color: 'var(--color-error)', textAlign: 'center' }}>
              Please check the price shift confirmation above to proceed.
            </span>
          )}
        </aside>
      </div>
    </div>
  );
}
