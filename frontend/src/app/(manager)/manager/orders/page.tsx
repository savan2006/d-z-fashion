'use client';

import React, { useEffect, useState, useTransition } from 'react';
import {
  CheckSquare,
  Package,
  AlertCircle,
  CheckCircle2,
  ShieldAlert,
  Search,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  adminApi,
  AdminOrderSummary,
  ReportShortageRequest,
} from '@/lib/api/admin';

export default function ManagerOrderQueuePage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<AdminOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'UNCLAIMED' | 'MINE' | 'DISPATCHED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: 'success' | 'error' | 'warning'; text: string } | null>(null);

  // Shortage Reporting Modal State
  const [shortageModalOrder, setShortageModalOrder] = useState<AdminOrderSummary | null>(null);
  const [shortageSku, setShortageSku] = useState('');
  const [shortageVariantId, setShortageVariantId] = useState('');
  const [shortageQty, setShortageQty] = useState(1);
  const [shortageReason, setShortageReason] = useState('');
  const [isSubmittingShortage, startShortageTransition] = useTransition();

  // Action Loading tracking
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);

  const fetchOrders = () => {
    adminApi
      .getOrderQueue(0, 50)
      .then((res) => {
        if (res.data?.content) {
          setOrders(res.data.content);
        }
      })
      .catch((err) => {
        setFeedbackMessage({
          type: 'error',
          text: err?.message || 'Failed to fetch warehouse fulfillment queue.',
        });
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleClaim = async (orderNumber: string) => {
    setActionInProgress(orderNumber);
    setFeedbackMessage(null);
    try {
      const res = await adminApi.claimOrder(orderNumber);
      if (res.data) {
        setFeedbackMessage({
          type: 'success',
          text: `Order #${orderNumber} claimed successfully. You are assigned to pick and pack this parcel.`,
        });
        fetchOrders();
      } else {
        setFeedbackMessage({
          type: 'warning',
          text: `Conflict: Order #${orderNumber} could not be claimed. It may have already been claimed by another manager.`,
        });
        fetchOrders();
      }
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : String(err || '');
      const errStatus = typeof err === 'object' && err !== null && 'status' in err ? (err as { status: number }).status : 0;
      if (errorMsg.includes('already claimed') || errorMsg.includes('conflict') || errStatus === 409) {
        setFeedbackMessage({
          type: 'warning',
          text: `Concurrent Claim Conflict: Order #${orderNumber} was already claimed by another warehouse manager. Refreshing queue...`,
        });
      } else {
        setFeedbackMessage({
          type: 'error',
          text: errorMsg || `Failed to claim order #${orderNumber}.`,
        });
      }
      fetchOrders();
    } finally {
      setActionInProgress(null);
    }
  };

  const handleDispatch = async (orderNumber: string) => {
    setActionInProgress(orderNumber);
    setFeedbackMessage(null);
    try {
      await adminApi.dispatchOrder(orderNumber);
      setFeedbackMessage({
        type: 'success',
        text: `Order #${orderNumber} marked as DISPATCHED and ready for agency handover.`,
      });
      fetchOrders();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : `Failed to dispatch order #${orderNumber}.`;
      setFeedbackMessage({
        type: 'error',
        text: msg,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleDeliver = async (orderNumber: string) => {
    setActionInProgress(orderNumber);
    setFeedbackMessage(null);
    try {
      await adminApi.deliverOrder(orderNumber);
      setFeedbackMessage({
        type: 'success',
        text: `Order #${orderNumber} successfully marked as DELIVERED to recipient.`,
      });
      fetchOrders();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : `Failed to mark order #${orderNumber} as delivered.`;
      setFeedbackMessage({
        type: 'error',
        text: msg,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReportShortageSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!shortageModalOrder || !shortageSku.trim() || shortageQty <= 0 || !shortageReason.trim()) return;

    startShortageTransition(async () => {
      try {
        const payload: ReportShortageRequest = {
          variantId: shortageVariantId || 'var-shortage-auto',
          sku: shortageSku.trim().toUpperCase(),
          missingQuantity: Number(shortageQty),
          reason: shortageReason.trim(),
        };
        await adminApi.reportShortage(shortageModalOrder.orderNumber, payload);
        setFeedbackMessage({
          type: 'warning',
          text: `Shortage incident reported for Order #${shortageModalOrder.orderNumber} (SKU: ${payload.sku}). Escalated for inventory audit.`,
        });
        setShortageModalOrder(null);
        setShortageSku('');
        setShortageVariantId('');
        setShortageQty(1);
        setShortageReason('');
        fetchOrders();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to submit shortage incident report.';
        setFeedbackMessage({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (order.claimedByManagerName && order.claimedByManagerName.toLowerCase().includes(searchQuery.toLowerCase()));

    if (!matchesSearch) return false;

    if (statusFilter === 'UNCLAIMED') {
      return !order.claimedByManagerId;
    }
    if (statusFilter === 'MINE') {
      return order.claimedByManagerId === user?.id;
    }
    if (statusFilter === 'DISPATCHED') {
      return order.orderStatus === 'DISPATCHED' || order.orderStatus === 'DELIVERED';
    }
    return true;
  });

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
          Surat Fulfillment Queue & Claims
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
          Claim warehouse picking tasks atomically. Once claimed, pack garments using Surat standard craft packaging.
        </p>
      </div>

      {/* Feedback Banner */}
      {feedbackMessage && (
        <div
          role="status"
          style={{
            padding: '14px 18px',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '20px',
            fontSize: '13px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor:
              feedbackMessage.type === 'success'
                ? 'var(--color-success-bg)'
                : feedbackMessage.type === 'warning'
                ? '#fffbeb'
                : 'var(--error-soft)',
            color:
              feedbackMessage.type === 'success'
                ? 'var(--color-success)'
                : feedbackMessage.type === 'warning'
                ? '#b45309'
                : 'var(--error)',
            border: `1px solid ${
              feedbackMessage.type === 'success'
                ? '#bbf7d0'
                : feedbackMessage.type === 'warning'
                ? '#fde68a'
                : '#fecaca'
            }`,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {feedbackMessage.type === 'success' && <CheckCircle2 size={16} />}
            {feedbackMessage.type === 'warning' && <AlertCircle size={16} />}
            {feedbackMessage.type === 'error' && <ShieldAlert size={16} />}
            <span>{feedbackMessage.text}</span>
          </div>
          <button
            onClick={() => setFeedbackMessage(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', fontWeight: 600 }}
          >
            &times;
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '12px',
          marginBottom: '20px',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          {(['ALL', 'UNCLAIMED', 'MINE', 'DISPATCHED'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setStatusFilter(filter)}
              style={{
                padding: '6px 14px',
                fontSize: '13px',
                fontWeight: statusFilter === filter ? 600 : 400,
                backgroundColor: statusFilter === filter ? 'var(--ink)' : '#ffffff',
                color: statusFilter === filter ? '#ffffff' : 'var(--ink-muted)',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
                cursor: 'pointer',
              }}
            >
              {filter === 'ALL' && 'All Orders'}
              {filter === 'UNCLAIMED' && 'Unclaimed Orders'}
              {filter === 'MINE' && 'Claimed by Me'}
              {filter === 'DISPATCHED' && 'Dispatched / In Transit'}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '240px' }}>
          <Search
            size={14}
            style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
          />
          <input
            type="text"
            placeholder="Search order # or manager..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '6px 10px 6px 32px',
              fontSize: '13px',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
            }}
          />
        </div>
      </div>

      {/* Main Fulfillment Table */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredOrders.length === 0 ? (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '48px 24px',
            textAlign: 'center',
            color: 'var(--ink-muted)',
          }}
        >
          <CheckSquare size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No orders match the selected filter
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Check back shortly or reset your filters to see recent customer orders.
          </p>
        </div>
      ) : (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            overflowX: 'auto',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--paper-warm)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Order #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Items Qty</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Payment Method</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Claim Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.map((order) => {
                const isClaimedByMe = order.claimedByManagerId === user?.id;
                const isClaimed = !!order.claimedByManagerId;
                const isActing = actionInProgress === order.orderNumber;

                return (
                  <tr
                    key={order.id || order.orderNumber}
                    style={{ borderBottom: '1px solid var(--line)', verticalAlign: 'middle' }}
                  >
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                      <span className="font-mono">{order.orderNumber}</span>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 400 }}>
                        {new Date(order.createdAt).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Package size={14} style={{ color: 'var(--ink-muted)' }} />
                        <span style={{ fontWeight: 500 }}>{order.totalItems} units</span>
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge variant="outline" size="sm">
                        {order.paymentMethod === 'CASH_ON_DELIVERY' ? 'COD' : 'PREPAID'}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge
                        variant={
                          order.orderStatus === 'DELIVERED'
                            ? 'success'
                            : order.orderStatus === 'DISPATCHED'
                            ? 'secondary'
                            : order.orderStatus === 'CANCELLED'
                            ? 'danger'
                            : 'warning'
                        }
                        size="sm"
                      >
                        {order.orderStatus}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {isClaimed ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Badge variant={isClaimedByMe ? 'primary' : 'secondary'} size="sm">
                            {isClaimedByMe ? 'Claimed by You' : `Claimed: ${order.claimedByManagerName || 'Staff'}`}
                          </Badge>
                        </div>
                      ) : (
                        <Badge variant="outline" size="sm" style={{ color: '#d97706', borderColor: '#fde68a' }}>
                          Unclaimed
                        </Badge>
                      )}
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                        {!isClaimed && (
                          <Button
                            variant="primary"
                            size="sm"
                            isLoading={isActing}
                            onClick={() => handleClaim(order.orderNumber)}
                          >
                            Claim Order
                          </Button>
                        )}

                        {isClaimed && order.orderStatus !== 'DISPATCHED' && order.orderStatus !== 'DELIVERED' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            isLoading={isActing}
                            onClick={() => handleDispatch(order.orderNumber)}
                          >
                            Mark Dispatched
                          </Button>
                        )}

                        {order.orderStatus === 'DISPATCHED' && (
                          <Button
                            variant="outline"
                            size="sm"
                            isLoading={isActing}
                            onClick={() => handleDeliver(order.orderNumber)}
                          >
                            Mark Delivered
                          </Button>
                        )}

                        <Button
                          variant="ghost"
                          size="sm"
                          style={{ color: 'var(--wine)' }}
                          onClick={() => {
                            setShortageModalOrder(order);
                            setShortageSku('');
                            setShortageQty(1);
                            setShortageReason('');
                          }}
                        >
                          Report Shortage
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Shortage Reporting Modal */}
      {shortageModalOrder && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              border: '1px solid var(--line)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Report Shortage — #{shortageModalOrder.orderNumber}
              </h2>
              <button
                onClick={() => setShortageModalOrder(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Record missing inventory during picking in Surat warehouse. This creates an incident ticket for Owner resolution.
            </p>

            <form onSubmit={handleReportShortageSubmit}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  SKU Code *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. SARI-SILK-MAR-M"
                  value={shortageSku}
                  onChange={(e) => setShortageSku(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Missing Quantity (Units) *
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  value={shortageQty}
                  onChange={(e) => setShortageQty(Number(e.target.value))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Warehouse Incident Reason *
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="e.g. Physical rack empty, misplacement during loom intake, or fabric tear noticed."
                  value={shortageReason}
                  onChange={(e) => setShortageReason(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setShortageModalOrder(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingShortage}>
                  Submit Shortage Incident
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
