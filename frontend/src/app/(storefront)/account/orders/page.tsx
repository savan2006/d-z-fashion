'use client';

import React, { useEffect, useState } from 'react';
import { customerApi, OrderSummary, PageResponse } from '@/lib/api/customer';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button, LinkButton } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Package, ArrowRight, ArrowLeft } from 'lucide-react';

function getOrderStatusVariant(status: string): BadgeVariant {
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

export default function MyOrdersPage() {
  const [loading, setLoading] = useState(true);
  const [ordersPage, setOrdersPage] = useState<PageResponse<OrderSummary> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    let isMounted = true;
    customerApi.getMyOrders(currentPage, 10)
      .then((res) => {
        if (isMounted && res.data) {
          setOrdersPage(res.data);
        }
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            My Orders
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Track your luxury ethnic wear orders and inspect parcel progress.
          </p>
        </div>
      </div>

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="80px" />
          <Skeleton height="80px" />
          <Skeleton height="80px" />
        </div>
      ) : !ordersPage || ordersPage.content.length === 0 ? (
        <div
          style={{
            padding: '48px 24px',
            textAlign: 'center',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: 'var(--surface-subtle)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '16px',
              color: 'var(--ink-muted)',
            }}
          >
            <Package size={24} />
          </div>
          <h3 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 600 }}>No orders yet</h3>
          <p style={{ margin: '0 0 20px', fontSize: '13px', color: 'var(--ink-muted)' }}>
            When you place an order, its real-time Surat fulfillment status will appear here.
          </p>
          <LinkButton href="/collections" variant="primary">
            Explore Ethnic Collections
          </LinkButton>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {ordersPage.content.map((order) => {
            const isPreDispatch = ['PENDING_APPROVAL', 'CONFIRMED', 'PROCESSING'].includes(order.orderStatus);
            return (
              <div
                key={order.id}
                style={{
                  backgroundColor: '#ffffff',
                  border: '1px solid var(--line)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '20px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '16px',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: '12px',
                    borderBottom: '1px solid var(--line-light)',
                    paddingBottom: '12px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ fontSize: '14px', fontWeight: 600, color: 'var(--ink)' }}>
                      Order #{order.orderNumber}
                    </span>
                    <Badge variant={getOrderStatusVariant(order.orderStatus)}>
                      {order.orderStatus.replace('_', ' ')}
                    </Badge>
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                    Placed on {new Date(order.createdAt).toLocaleDateString('en-IN', {
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric',
                    })}
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    gap: '16px',
                  }}
                >
                  <div>
                    <span style={{ fontSize: '12px', color: 'var(--ink-muted)', display: 'block' }}>
                      Items &amp; Payment
                    </span>
                    <span style={{ fontSize: '13px', color: 'var(--ink)' }}>
                      {order.totalItems} {order.totalItems === 1 ? 'item' : 'items'} &bull;{' '}
                      {order.paymentMethod === 'CASH_ON_DELIVERY' ? 'Cash on Delivery (COD)' : 'Prepaid Online'}
                    </span>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <span style={{ fontSize: '12px', color: 'var(--ink-muted)', display: 'block' }}>
                      Total Amount
                    </span>
                    <span style={{ fontSize: '16px', fontWeight: 600, color: 'var(--wine)' }}>
                      ₹{order.totalPayable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    paddingTop: '8px',
                    flexWrap: 'wrap',
                    gap: '8px',
                  }}
                >
                  <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                    {isPreDispatch ? (
                      <span style={{ color: 'var(--ink-muted)' }}>Eligible for instant pre-dispatch cancellation</span>
                    ) : order.orderStatus === 'DELIVERED' ? (
                      <span style={{ color: 'var(--wine)' }}>Quality assured: 3-day exchange/return available</span>
                    ) : null}
                  </div>

                  <div style={{ display: 'flex', gap: '8px' }}>
                    <LinkButton href={`/account/orders/${order.orderNumber}`} variant="primary" size="sm">
                      View Order Breakdown
                      <ArrowRight size={14} style={{ marginLeft: '4px' }} />
                    </LinkButton>
                  </div>
                </div>
              </div>
            );
          })}

          {/* Pagination Controls */}
          {ordersPage.totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '16px' }}>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              >
                <ArrowLeft size={14} style={{ marginRight: '4px' }} />
                Previous
              </Button>
              <span style={{ display: 'flex', alignItems: 'center', fontSize: '13px', color: 'var(--ink-muted)', padding: '0 8px' }}>
                Page {currentPage + 1} of {ordersPage.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage >= ordersPage.totalPages - 1}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Next
                <ArrowRight size={14} style={{ marginLeft: '4px' }} />
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
