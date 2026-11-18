'use client';

import React, { useEffect, useState } from 'react';
import {
  adminApi,
  AdminOrderSummary,
  PageResponse,
  ManagerStaffResponse,
  ShortageIncidentResponse,
} from '@/lib/api/admin';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import {
  UserCheck,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
} from 'lucide-react';

function getOrderStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'DELIVERED':
      return 'success';
    case 'DISPATCHED':
      return 'neutral';
    case 'PROCESSING':
    case 'CONFIRMED':
      return 'warning';
    case 'CANCELLED':
    case 'RETURNED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function AdminOrdersPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [ordersPage, setOrdersPage] = useState<PageResponse<AdminOrderSummary> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);

  // Managers list for reassignment
  const [managers, setManagers] = useState<ManagerStaffResponse[]>([]);

  // Reassign Modal State
  const [reassignOrderTarget, setReassignOrderTarget] = useState<AdminOrderSummary | null>(null);
  const [selectedManagerId, setSelectedManagerId] = useState('');
  const [reassignReason, setReassignReason] = useState('');
  const [reassigning, setReassigning] = useState(false);

  // Shortages State
  const [shortages, setShortages] = useState<ShortageIncidentResponse[]>([]);
  const [resolveTarget, setResolveTarget] = useState<ShortageIncidentResponse | null>(null);
  const [resolutionType, setResolutionType] = useState<'RESTOCKED_RESERVED' | 'CANCELLED_REFUNDED' | 'SUBSTITUTED'>('RESTOCKED_RESERVED');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [resolving, setResolving] = useState(false);

  const loadData = React.useCallback(() => {
    let isMounted = true;
    Promise.all([
      adminApi.getOrderQueue(currentPage, 15).catch(() => null),
      adminApi.listManagers().catch(() => null),
      adminApi.listShortages('OPEN').catch(() => null),
    ]).then(([ordRes, manRes, shortRes]) => {
      if (!isMounted) return;
      if (ordRes?.data) setOrdersPage(ordRes.data);
      if (manRes?.data) setManagers(manRes.data);
      if (shortRes?.data) setShortages(shortRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleReassign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reassignOrderTarget) return;
    if (!reassignReason.trim()) {
      toast({ type: 'warning', title: 'Reason Required', message: 'Please provide a reason for order reassignment.' });
      return;
    }

    setReassigning(true);
    try {
      await adminApi.reassignOrder(reassignOrderTarget.orderNumber, {
        newManagerId: selectedManagerId || undefined,
        reason: reassignReason.trim(),
      });
      toast({
        type: 'success',
        title: 'Order Reassigned',
        message: `Order #${reassignOrderTarget.orderNumber} successfully reassigned.`,
      });
      setReassignOrderTarget(null);
      setReassignReason('');
      setSelectedManagerId('');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to reassign order';
      toast({ type: 'error', title: 'Reassignment Failed', message: msg });
    } finally {
      setReassigning(false);
    }
  };

  const handleResolveShortage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolveTarget) return;
    if (!resolutionNotes.trim()) {
      toast({ type: 'warning', title: 'Notes Required', message: 'Please enter resolution notes.' });
      return;
    }

    setResolving(true);
    try {
      await adminApi.resolveShortage(resolveTarget.incidentNumber, {
        resolutionType,
        resolutionNotes: resolutionNotes.trim(),
      });
      toast({ type: 'success', title: 'Shortage Resolved', message: `Incident #${resolveTarget.incidentNumber} marked as resolved.` });
      setResolveTarget(null);
      setResolutionNotes('');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to resolve shortage';
      toast({ type: 'error', title: 'Resolution Failed', message: msg });
    } finally {
      setResolving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Orders &amp; Reassignments
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Authoritative view of all orders, financial figures, manager assignment claims, and inventory shortages.
        </p>
      </div>

      {/* Shortages Alert Banner if any open incidents */}
      {shortages.length > 0 && (
        <div
          style={{
            backgroundColor: '#fffbeb',
            border: '1px solid #fde68a',
            borderRadius: 'var(--radius-sm)',
            padding: '16px 20px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <AlertTriangle size={18} color="#b45309" />
            <span style={{ fontWeight: 600, fontSize: '14px', color: '#92400e' }}>
              {shortages.length} Unresolved Warehouse Shortage Incident{shortages.length > 1 ? 's' : ''}
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {shortages.map((s) => (
              <div
                key={s.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  backgroundColor: '#ffffff',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid #fef3c7',
                }}
              >
                <div>
                  <span style={{ fontWeight: 600, fontSize: '13px' }}>Incident #{s.incidentNumber}</span>
                  <span style={{ fontSize: '12px', color: 'var(--ink-muted)', marginLeft: '8px' }}>
                    Order #{s.orderNumber} &bull; Missing SKU: {s.sku} (Qty: {s.missingQuantity})
                  </span>
                </div>
                <Button variant="outline" size="sm" onClick={() => setResolveTarget(s)}>
                  Resolve Shortage
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Orders Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Commercial Order Queue</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !ordersPage || ordersPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No orders found in queue.
          </div>
        ) : (
          <div>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Order #</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Date</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Total Payable</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Payment</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600 }}>Claimed By</th>
                  <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Owner Actions</th>
                </tr>
              </thead>
              <tbody>
                {ordersPage.content.map((ord) => (
                  <tr key={ord.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      #{ord.orderNumber}
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                      {new Date(ord.createdAt).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                      })}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant={getOrderStatusVariant(ord.orderStatus)}>
                        {ord.orderStatus}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--wine)' }}>
                      ₹{ord.totalPayable.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ fontSize: '11px', color: 'var(--ink-muted)', display: 'block' }}>
                        {ord.paymentMethod === 'CASH_ON_DELIVERY' ? 'COD' : 'Prepaid'}
                      </span>
                      <span style={{ fontSize: '12px', fontWeight: 500 }}>
                        {ord.paymentStatus}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {ord.claimedByManagerName ? (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            backgroundColor: 'var(--color-success-bg)',
                            color: 'var(--color-success)',
                            padding: '3px 8px',
                            borderRadius: '12px',
                            fontSize: '11px',
                            fontWeight: 600,
                          }}
                        >
                          <UserCheck size={12} />
                          {ord.claimedByManagerName}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--ink-muted)', fontSize: '12px' }}>Unclaimed</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setReassignOrderTarget(ord);
                          setSelectedManagerId(ord.claimedByManagerId || '');
                        }}
                      >
                        Reassign
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            {ordersPage.totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', padding: '16px' }}>
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

      {/* Owner Reassign Order Modal */}
      {reassignOrderTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setReassignOrderTarget(null)}
          title="Reassign Fulfillment Order"
          description={`Order #${reassignOrderTarget.orderNumber} is currently ${reassignOrderTarget.claimedByManagerName ? `assigned to ${reassignOrderTarget.claimedByManagerName}` : 'unclaimed'}.`}
        >
          <form onSubmit={handleReassign} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <Select
              label="Select Assignee"
              value={selectedManagerId}
              onChange={(e) => setSelectedManagerId(e.target.value)}
              options={[
                { value: '', label: '— Unassign / Return to Queue —' },
                ...managers.map((m) => ({ value: m.id, label: `${m.fullName} (@${m.username})` })),
              ]}
            />

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Reassignment Reason <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. Workload balancing, shift handover, or priority picking in Surat warehouse..."
                value={reassignReason}
                onChange={(e) => setReassignReason(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setReassignOrderTarget(null)} disabled={reassigning}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={reassigning}>
                Confirm Reassignment
              </Button>
            </div>
          </form>
        </Dialog>
      )}

      {/* Resolve Shortage Modal */}
      {resolveTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setResolveTarget(null)}
          title="Resolve Inventory Shortage"
          description={`Incident #${resolveTarget.incidentNumber} on Order #${resolveTarget.orderNumber}. Missing SKU: ${resolveTarget.sku}.`}
        >
          <form onSubmit={handleResolveShortage} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <Select
              label="Resolution Strategy"
              value={resolutionType}
              onChange={(e) => setResolutionType(e.target.value as 'RESTOCKED_RESERVED' | 'CANCELLED_REFUNDED' | 'SUBSTITUTED')}
              options={[
                { value: 'RESTOCKED_RESERVED', label: 'Restocked & Reserved (Warehouse fresh intake)' },
                { value: 'CANCELLED_REFUNDED', label: 'Cancel Shortage Item & Refund Customer' },
                { value: 'SUBSTITUTED', label: 'Customer Approved Alternate Variant' },
              ]}
            />

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Resolution Notes <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="Document resolution action taken..."
                value={resolutionNotes}
                onChange={(e) => setResolutionNotes(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setResolveTarget(null)} disabled={resolving}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={resolving}>
                Resolve Incident
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
