'use client';

import React, { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  customerApi,
  OrderResponse,
  OrderItem,
  OrderStatus,
} from '@/lib/api/customer';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { ReviewModal } from '@/components/storefront/ReviewModal';
import {
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  Clock,
  MapPin,
  ShieldAlert,
  Star,
  RefreshCw,
} from 'lucide-react';

function getOrderStatusVariant(status: OrderStatus): BadgeVariant {
  switch (status) {
    case 'DELIVERED':
      return 'success';
    case 'DISPATCHED':
      return 'neutral';
    case 'PROCESSING':
    case 'CONFIRMED':
    case 'PENDING_APPROVAL':
      return 'warning';
    case 'CANCELLED':
    case 'RETURNED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function OrderDetailsPage({
  params,
}: {
  params: Promise<{ orderNumber: string }>;
}) {
  const resolvedParams = use(params);
  const orderNumber = resolvedParams.orderNumber;
  const router = useRouter();
  const { toast } = useToast();

  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<OrderResponse | null>(null);

  // Cancellation State
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelling, setCancelling] = useState(false);

  // Review Modal State
  const [reviewItem, setReviewItem] = useState<OrderItem | null>(null);

  useEffect(() => {
    let isMounted = true;
    customerApi.getOrderDetails(orderNumber)
      .then((res) => {
        if (isMounted && res.data) setOrder(res.data);
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = (err as { message?: string })?.message || 'Failed to load order details';
          toast({ type: 'error', title: 'Error', message: msg });
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [orderNumber, toast]);

  // Cancel order handler
  const handleCancelOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelReason.trim()) {
      toast({ type: 'warning', title: 'Reason Required', message: 'Please provide a reason for cancelling this order.' });
      return;
    }

    setCancelling(true);
    try {
      const res = await customerApi.cancelOrder(orderNumber, cancelReason.trim());
      if (res.data) {
        setOrder(res.data);
        setCancelModalOpen(false);
        toast({
          type: 'success',
          title: 'Order Cancelled',
          message: 'Your order was successfully cancelled. Any store credit used has been restored.',
        });
      }
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Unable to cancel order.';
      toast({ type: 'error', title: 'Cancellation Denied', message: msg });
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <Skeleton height="60px" />
        <Skeleton height="200px" />
        <Skeleton height="300px" />
      </div>
    );
  }

  if (!order) {
    return (
      <div
        style={{
          padding: '48px 24px',
          textAlign: 'center',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
        }}
      >
        <AlertTriangle size={32} color="var(--wine)" style={{ marginBottom: '12px' }} />
        <h2 style={{ fontSize: '18px', margin: '0 0 8px' }}>Order Not Found</h2>
        <p style={{ fontSize: '13px', color: 'var(--ink-muted)', marginBottom: '20px' }}>
          We couldn&apos;t find order #{orderNumber}.
        </p>
        <Button variant="outline" onClick={() => router.push('/account/orders')}>
          Back to My Orders
        </Button>
      </div>
    );
  }

  const isPreDispatch = ['PENDING_APPROVAL', 'CONFIRMED', 'PROCESSING'].includes(order.orderStatus);
  const isTerminal = ['CANCELLED', 'RETURNED'].includes(order.orderStatus);

  // Status Step Hierarchy
  const steps: { key: OrderStatus; label: string; desc: string }[] = [
    { key: 'CONFIRMED', label: 'Confirmed', desc: 'Order verified & allocated' },
    { key: 'PROCESSING', label: 'Processing', desc: 'Picking & Surat QC inspection' },
    { key: 'DISPATCHED', label: 'Dispatched', desc: 'Handed to courier express' },
    { key: 'DELIVERED', label: 'Delivered', desc: 'Delivered to your address' },
  ];

  const getStepStatus = (stepKey: OrderStatus) => {
    if (isTerminal) return 'neutral';
    const sequence: OrderStatus[] = ['CONFIRMED', 'PROCESSING', 'DISPATCHED', 'DELIVERED'];
    const currentIndex = sequence.indexOf(order.orderStatus);
    const stepIndex = sequence.indexOf(stepKey);
    if (stepIndex < currentIndex) return 'completed';
    if (stepIndex === currentIndex) return 'current';
    return 'upcoming';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Back button and Header */}
      <div>
        <Link
          href="/account/orders"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            marginBottom: '16px',
            textDecoration: 'none',
          }}
        >
          <ArrowLeft size={14} />
          <span>Back to All Orders</span>
        </Link>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 className="font-serif" style={{ margin: 0, fontSize: '24px', fontWeight: 500 }}>
                Order #{order.orderNumber}
              </h1>
              <Badge variant={getOrderStatusVariant(order.orderStatus)}>
                {order.orderStatus.replace('_', ' ')}
              </Badge>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
              Placed on {new Date(order.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}
            </p>
          </div>

          {/* Action buttons */}
          <div style={{ display: 'flex', gap: '8px' }}>
            {isPreDispatch && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCancelModalOpen(true)}
                style={{ color: 'var(--error)', borderColor: 'var(--line)' }}
              >
                Cancel Order
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Terminal Status Warning Banner if Cancelled */}
      {order.orderStatus === 'CANCELLED' && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '16px 20px',
            backgroundColor: 'var(--error-soft)',
            border: '1px solid var(--error-line)',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--error)',
            fontSize: '13px',
          }}
        >
          <ShieldAlert size={20} />
          <div>
            <strong>This order has been cancelled.</strong> No courier shipment will occur. If you used store credit or prepaid, adjustments have been credited to your account.
          </div>
        </div>
      )}

      {/* Real-Time Lifecycle Tracker */}
      {!isTerminal && (
        <div
          style={{
            padding: '24px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
          }}
        >
          <h2 style={{ margin: '0 0 20px', fontSize: '14px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Delivery Progress
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px' }}>
            {steps.map((st) => {
              const state = getStepStatus(st.key);
              const isDone = state === 'completed';
              const isCurrent = state === 'current';
              return (
                <div
                  key={st.key}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    borderTop: `3px solid ${isDone ? 'var(--success)' : isCurrent ? 'var(--wine)' : 'var(--line)'}`,
                    paddingTop: '12px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                    {isDone ? (
                      <CheckCircle2 size={16} color="var(--success)" />
                    ) : isCurrent ? (
                      <Clock size={16} color="var(--wine)" />
                    ) : (
                      <div
                        style={{
                          width: '10px',
                          height: '10px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--line-strong)',
                        }}
                      />
                    )}
                    <span
                      style={{
                        fontSize: '13px',
                        fontWeight: isCurrent ? 600 : 500,
                        color: isCurrent ? 'var(--wine)' : isDone ? 'var(--ink)' : 'var(--ink-muted)',
                      }}
                    >
                      {st.label}
                    </span>
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--ink-muted)', lineHeight: 1.3 }}>
                    {st.desc}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Order Details: Items & Summary */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1.2fr', gap: '24px', alignItems: 'start' }}>
        {/* Left Column: Line Items */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div
            style={{
              padding: '24px',
              backgroundColor: '#ffffff',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <h2 style={{ margin: '0 0 16px', fontSize: '15px', fontWeight: 600 }}>
              Purchased Items ({order.items.length})
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {order.items.map((item) => (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    paddingBottom: '16px',
                    borderBottom: '1px solid var(--line-light)',
                    gap: '16px',
                    flexWrap: 'wrap',
                  }}
                >
                  <div>
                    <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: 'var(--ink)' }}>
                      {item.productName}
                    </h3>
                    <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginTop: '4px' }}>
                      SKU: {item.sku} &bull; Size: {item.size} &bull; Colour: {item.colour}
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--ink)', marginTop: '2px' }}>
                      Qty: {item.quantity} &times; ₹{item.unitPrice.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                    <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--wine)' }}>
                      ₹{item.itemTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </span>

                    {/* Delivered Order Actions */}
                    {order.orderStatus === 'DELIVERED' && (
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setReviewItem(item)}
                          style={{ fontSize: '11px', padding: '4px 8px' }}
                        >
                          <Star size={12} style={{ marginRight: '4px', color: '#b8860b' }} />
                          Write Review
                        </Button>
                        <Link
                          href={`/account/returns?orderNumber=${encodeURIComponent(order.orderNumber)}&sku=${encodeURIComponent(item.sku)}`}
                          style={{
                            fontSize: '11px',
                            color: 'var(--wine)',
                            textDecoration: 'none',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            fontWeight: 500,
                            padding: '4px 8px',
                          }}
                        >
                          <RefreshCw size={11} />
                          Return/Exchange
                        </Link>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Delivery & Shipping Info */}
          <div
            style={{
              padding: '24px',
              backgroundColor: '#ffffff',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
              <MapPin size={18} color="var(--wine)" />
              <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Delivery Destination</h2>
            </div>
            <div style={{ fontSize: '13px', lineHeight: 1.6, color: 'var(--ink-secondary)' }}>
              <p style={{ margin: 0, fontWeight: 600, color: 'var(--ink)' }}>{order.recipientName}</p>
              <p style={{ margin: 0 }}>Phone: +91 {order.recipientPhone}</p>
              <p style={{ margin: 0 }}>{order.shippingAddressLine1}</p>
              {order.shippingAddressLine2 && <p style={{ margin: 0 }}>{order.shippingAddressLine2}</p>}
              <p style={{ margin: 0 }}>
                {order.shippingCity}, {order.shippingState} - {order.shippingPostalCode}
              </p>
            </div>
          </div>
        </div>

        {/* Right Column: Server-Authoritative Totals Breakdown */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div
            style={{
              padding: '24px',
              backgroundColor: '#ffffff',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <h2 style={{ margin: '0 0 16px', fontSize: '15px', fontWeight: 600 }}>
              Payment &amp; Financial Summary
            </h2>

            {/* Payment Method Details */}
            <div
              style={{
                padding: '12px',
                backgroundColor: 'var(--surface-subtle)',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '16px',
                fontSize: '12px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Payment Method</span>
                <span style={{ fontWeight: 600 }}>
                  {order.paymentMethod === 'CASH_ON_DELIVERY' ? 'Cash on Delivery (COD)' : 'Prepaid Online'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Payment Status</span>
                <Badge variant={order.paymentStatus === 'PAID' ? 'success' : 'neutral'}>
                  {order.paymentStatus}
                </Badge>
              </div>
            </div>

            {/* Server Breakdown */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Merchandise Subtotal</span>
                <span>₹{order.subtotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Delivery Charge</span>
                <span>
                  {order.deliveryCharge === 0 ? (
                    <span style={{ color: 'var(--success)', fontWeight: 600 }}>FREE</span>
                  ) : (
                    `₹${order.deliveryCharge.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
                  )}
                </span>
              </div>

              {order.discountAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--success)' }}>
                  <span>Promotional Discount</span>
                  <span>-₹{order.discountAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--ink-muted)' }}>Surat GST (Included in price)</span>
                <span>₹{order.taxAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </div>

              {order.storeCreditUsed > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--wine)' }}>
                  <span>Store Credit Redeemed</span>
                  <span>-₹{order.storeCreditUsed.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                </div>
              )}

              <div
                style={{
                  borderTop: '1px solid var(--line)',
                  paddingTop: '12px',
                  marginTop: '6px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontSize: '15px', fontWeight: 600 }}>Total Payable</span>
                <span className="font-serif" style={{ fontSize: '20px', fontWeight: 600, color: 'var(--wine)' }}>
                  ₹{order.totalPayable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Pre-Dispatch Cancellation Modal */}
      <Dialog
        isOpen={cancelModalOpen}
        onClose={() => setCancelModalOpen(false)}
        title="Cancel Order"
        description={`Order #${order.orderNumber} is currently ${order.orderStatus.toLowerCase()}. Pre-dispatch cancellation is permitted.`}
      >
        <form onSubmit={handleCancelOrder} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
              Reason for Cancellation <span style={{ color: 'var(--wine)' }}>*</span>
            </label>
            <textarea
              className="form-input"
              rows={3}
              placeholder="e.g. Changed mind, ordered incorrect size, or duplicate order..."
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              required
              style={{ width: '100%', resize: 'vertical' }}
            />
          </div>

          <div
            style={{
              padding: '12px',
              backgroundColor: 'var(--surface-subtle)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '12px',
              color: 'var(--ink-muted)',
            }}
          >
            <strong>Note:</strong> Once cancelled, reserved inventory is released back to stock. Any store credit redeemed will be immediately returned to your non-expiring balance.
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
            <Button type="button" variant="outline" onClick={() => setCancelModalOpen(false)} disabled={cancelling}>
              Keep Order
            </Button>
            <Button type="submit" variant="primary" isLoading={cancelling} style={{ backgroundColor: 'var(--error)' }}>
              Confirm Cancellation
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Product Review Modal */}
      {reviewItem && (
        <ReviewModal
          isOpen={true}
          onClose={() => setReviewItem(null)}
          productId={reviewItem.productId}
          orderId={order.id}
          orderItemId={reviewItem.id}
          productName={reviewItem.productName}
          onReviewSubmitted={() => {
            toast({ type: 'success', title: 'Review Saved', message: 'Your review was submitted successfully.' });
          }}
        />
      )}
    </div>
  );
}
