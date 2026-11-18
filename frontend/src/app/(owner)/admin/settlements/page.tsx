'use client';

import React, { useEffect, useState } from 'react';
import { adminApi, CodSettlementBatchResponse, PageResponse } from '@/lib/api/admin';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { AlertTriangle, ShieldCheck } from 'lucide-react';

function getBatchStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'APPROVED':
      return 'success';
    case 'SUBMITTED_FOR_APPROVAL':
      return 'warning';
    case 'REJECTED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function AdminSettlementsPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [batchesPage, setBatchesPage] = useState<PageResponse<CodSettlementBatchResponse> | null>(null);
  const [approvingId, setApprovingId] = useState<string | null>(null);

  const loadBatches = () => {
    let isMounted = true;
    adminApi.getAllCodSettlementBatches(0, 20)
      .then((res) => {
        if (!isMounted || !res.data) return;
        setBatchesPage(res.data);
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
    loadBatches();
  }, []);

  const handleApproveBatch = async (batchId: string, batchNumber: string) => {
    setApprovingId(batchId);
    try {
      await adminApi.approveCodSettlementBatch(batchId);
      toast({
        type: 'success',
        title: 'Batch Approved',
        message: `COD Settlement Batch #${batchNumber} confirmed and reconciled.`,
      });
      loadBatches();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Approval failed';
      toast({ type: 'error', title: 'Approval Error', message: msg });
    } finally {
      setApprovingId(null);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          COD Settlement Approvals
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Courier transport agency COD cash remittances. Strict Owner approval required before financial reconciliation.
        </p>
      </div>

      {/* Security Invariant */}
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
          <strong>Owner Approval Invariant:</strong> Operations managers prepare and verify courier agency remittances against delivered waybills, but only the Store Owner possesses cryptographic authorization to finalize financial reconciliation.
        </div>
      </div>

      {/* Batches Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Agency Remittance Batches</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !batchesPage || batchesPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No COD settlement batches in records.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Batch #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Agency &amp; Invoice</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Orders Count</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Collected vs Remitted</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Discrepancy</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Owner Decision</th>
              </tr>
            </thead>
            <tbody>
              {batchesPage.content.map((b) => {
                const canApprove = b.status === 'SUBMITTED_FOR_APPROVAL' || b.status === 'PREPARED';
                return (
                  <tr key={b.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      #{b.batchNumber}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 500 }}>{b.agencyName}</div>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                        Ref: {b.agencyInvoiceReference}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink)' }}>
                      {b.totalOrders} parcels
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 600, color: 'var(--wine)' }}>
                        ₹{b.totalRemittedAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                        Expected: ₹{b.totalExpectedAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {b.hasDiscrepancy ? (
                        <span style={{ color: 'var(--error)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <AlertTriangle size={12} />
                          -₹{b.discrepancyAmount}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--color-success)', fontWeight: 500 }}>Exact Match</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant={getBatchStatusVariant(b.status)}>
                        {b.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      {canApprove ? (
                        <Button
                          variant="primary"
                          size="sm"
                          isLoading={approvingId === b.id}
                          onClick={() => handleApproveBatch(b.id, b.batchNumber)}
                        >
                          Approve Settlement
                        </Button>
                      ) : (
                        <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                          Reconciled {b.approvedAt ? new Date(b.approvedAt).toLocaleDateString('en-IN') : ''}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
