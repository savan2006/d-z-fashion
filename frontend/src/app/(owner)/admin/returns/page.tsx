'use client';

import React, { useEffect, useState } from 'react';
import { adminApi, ReturnItemStaffResponse, PageResponse } from '@/lib/api/admin';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { Clock } from 'lucide-react';

function getReturnStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'COMPLETED':
    case 'INSPECTED_PASSED':
    case 'APPROVED':
      return 'success';
    case 'SUBMITTED':
    case 'ITEM_RECEIVED':
      return 'warning';
    case 'REJECTED':
    case 'INSPECTED_REJECTED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function AdminReturnsPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [returnsPage, setReturnsPage] = useState<PageResponse<ReturnItemStaffResponse> | null>(null);

  // Reject Modal State
  const [rejectTarget, setRejectTarget] = useState<ReturnItemStaffResponse | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejecting, setRejecting] = useState(false);
  const [approvingId, setApprovingId] = useState<string | null>(null);

  const loadReturns = () => {
    let isMounted = true;
    adminApi.getAllReturns(0, 20)
      .then((res) => {
        if (!isMounted || !res.data) return;
        setReturnsPage(res.data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  };

  useEffect(() => {
    loadReturns();
  }, []);

  const handleApproveRefund = async (id: string, returnNumber: string) => {
    setApprovingId(id);
    try {
      await adminApi.approveRefund(id);
      toast({
        type: 'success',
        title: 'Refund Authorized',
        message: `Refund approved for Return #${returnNumber}. Accounting deadline: 7 calendar days.`,
      });
      loadReturns();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Approval failed';
      toast({ type: 'error', title: 'Error', message: msg });
    } finally {
      setApprovingId(null);
    }
  };

  const handleRejectRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectTarget) return;
    if (!rejectReason.trim()) {
      toast({ type: 'warning', title: 'Reason Required', message: 'Please enter a rejection reason.' });
      return;
    }

    setRejecting(true);
    try {
      await adminApi.rejectRefund(rejectTarget.id, rejectReason.trim());
      toast({ type: 'info', title: 'Refund Rejected', message: `Return #${rejectTarget.returnNumber} refund rejected.` });
      setRejectTarget(null);
      setRejectReason('');
      loadReturns();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Rejection failed';
      toast({ type: 'error', title: 'Error', message: msg });
    } finally {
      setRejecting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Refund &amp; Return Approvals
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Surat quality inspection reports and Owner refund payout authorizations.
        </p>
      </div>

      {/* SLA Policy Notice */}
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
        <Clock size={20} color="var(--wine)" />
        <div>
          <strong>7-Day Refund SLA Mandate:</strong> Approved customer refunds must be processed through payment reconciliation within 7 calendar days of inspection approval.
        </div>
      </div>

      {/* Returns Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Inspection &amp; Refund Queue</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !returnsPage || returnsPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No returns in queue.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Return #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Order #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Item &amp; Reason</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Resolution</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Inspection Result</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Owner Decision</th>
              </tr>
            </thead>
            <tbody>
              {returnsPage.content.map((r) => {
                const isPendingOwner = r.status === 'INSPECTED_PASSED' && r.resolutionType === 'ORIGINAL_PAYMENT_REFUND';
                return (
                  <tr key={r.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      #{r.returnNumber}
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                      #{r.orderNumber}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 500 }}>{r.productName}</div>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                        SKU: {r.sku} &bull; Reason: {r.reason.replace(/_/g, ' ')}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant="neutral">
                        {r.resolutionType.replace(/_/g, ' ')}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {r.inspectionNotes ? (
                        <div style={{ fontSize: '12px' }}>
                          <span style={{ fontWeight: 600, color: r.inspectionPassed ? 'var(--color-success)' : 'var(--error)' }}>
                            {r.inspectionPassed ? 'Passed' : 'Failed'}
                          </span>
                          : &ldquo;{r.inspectionNotes}&rdquo;
                        </div>
                      ) : (
                        <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>Pending Inspection</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant={getReturnStatusVariant(r.status)}>
                        {r.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      {isPendingOwner ? (
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                          <Button
                            variant="primary"
                            size="sm"
                            isLoading={approvingId === r.id}
                            onClick={() => handleApproveRefund(r.id, r.returnNumber)}
                          >
                            Approve Refund
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            style={{ color: 'var(--error)' }}
                            onClick={() => setRejectTarget(r)}
                          >
                            Reject
                          </Button>
                        </div>
                      ) : (
                        <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Reject Refund Modal */}
      {rejectTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setRejectTarget(null)}
          title={`Reject Refund for Return #${rejectTarget.returnNumber}`}
          description="Provide justification for refund rejection. An audit notice will be dispatched to the customer."
        >
          <form onSubmit={handleRejectRefund} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Rejection Reason <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. Tags tampered with or fabric tear occurred post-delivery..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setRejectTarget(null)} disabled={rejecting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={rejecting} style={{ backgroundColor: 'var(--error)' }}>
                Confirm Rejection
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
