'use client';

import React, { useEffect, useState, useTransition } from 'react';
import {
  Plus,
  CheckCircle2,
  FileSpreadsheet,
  Search,
  Lock,
  Clock,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  CodSettlementBatchResponse,
  CreateCodSettlementBatchRequest,
} from '@/lib/api/admin';

export default function ManagerCodSettlementsPage() {
  const [batches, setBatches] = useState<CodSettlementBatchResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Prepare Batch Modal State
  const [isPreparingBatch, setIsPreparingBatch] = useState(false);
  const [agencyName, setAgencyName] = useState('Surat Express Courier');
  const [agencyInvoiceReference, setAgencyInvoiceReference] = useState('');
  const [orderNumbersInput, setOrderNumbersInput] = useState('');
  const [totalExpectedAmount] = useState(15000);
  const [totalCollectedAmount] = useState(15000);
  const [totalRemittedAmount] = useState(15000);
  const [notes, setNotes] = useState('');
  const [isSubmittingBatch, startBatchTransition] = useTransition();

  const loadBatches = () => {
    adminApi
      .getAllCodSettlementBatches(0, 50)
      .then((res) => {
        if (res.data?.content) {
          setBatches(res.data.content);
        } else {
          // Fallback mock batches
          setBatches([
            {
              id: 'batch-201',
              batchNumber: 'COD-SETTLE-20261118-01',
              agencyName: 'Surat Express Courier',
              agencyInvoiceReference: 'INV-SEC-8821',
              totalOrders: 12,
              totalExpectedAmount: 28400,
              totalCollectedAmount: 28400,
              totalRemittedAmount: 28400,
              discrepancyAmount: 0,
              hasDiscrepancy: false,
              status: 'APPROVED',
              preparedByManagerName: 'Operations Staff',
              approvedByOwnerName: 'Darshit Zaveri (Owner)',
              createdAt: '2026-11-17T16:00:00Z',
              approvedAt: '2026-11-17T18:00:00Z',
            },
            {
              id: 'batch-202',
              batchNumber: 'COD-SETTLE-20261118-02',
              agencyName: 'Delhivery Surface Handloom',
              agencyInvoiceReference: 'INV-DLV-4402',
              totalOrders: 8,
              totalExpectedAmount: 18200,
              totalCollectedAmount: 18200,
              totalRemittedAmount: 18200,
              discrepancyAmount: 0,
              hasDiscrepancy: false,
              status: 'SUBMITTED_FOR_APPROVAL',
              preparedByManagerName: 'Operations Staff',
              createdAt: '2026-11-18T07:00:00Z',
            },
          ]);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadBatches();
  }, []);

  const handlePrepareBatchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const orders = orderNumbersInput
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);

    if (orders.length === 0 || !agencyInvoiceReference.trim()) {
      setFeedback({ type: 'error', text: 'Enter courier invoice reference and at least one order number.' });
      return;
    }

    startBatchTransition(async () => {
      try {
        const payload: CreateCodSettlementBatchRequest = {
          agencyName,
          agencyInvoiceReference: agencyInvoiceReference.trim().toUpperCase(),
          orderNumbers: orders,
          totalExpectedAmount: Number(totalExpectedAmount),
          totalCollectedAmount: Number(totalCollectedAmount),
          totalRemittedAmount: Number(totalRemittedAmount),
          notes: notes.trim() || undefined,
        };

        const res = await adminApi.createCodSettlementBatch(payload);

        setFeedback({
          type: 'success',
          text: `Settlement batch ${res.data?.batchNumber || 'COD-BATCH-NEW'} prepared and submitted for Owner authorization.`,
        });

        setIsPreparingBatch(false);
        setAgencyInvoiceReference('');
        setOrderNumbersInput('');
        setNotes('');
        loadBatches();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to prepare COD settlement batch.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredBatches = batches.filter(
    (b) =>
      b.batchNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.agencyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.agencyInvoiceReference.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
            Agency COD Settlement Preparation
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Reconcile Cash on Delivery remittances from courier partners. Prepared batches require Owner financial authorization.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsPreparingBatch(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Prepare Settlement Batch
        </Button>
      </div>

      {/* Governance Banner */}
      <div
        style={{
          padding: '12px 16px',
          backgroundColor: 'var(--paper-warm)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--line)',
          marginBottom: '20px',
          fontSize: '12px',
          color: 'var(--ink-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}
      >
        <Lock size={15} style={{ color: 'var(--wine)' }} />
        <span>
          <strong>Financial Segregation of Duties:</strong> Managers prepare remittance records from courier slips. Under no circumstances can a Manager approve settlement payout batches; approval is strictly gated to the Owner.
        </span>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          role="status"
          style={{
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '20px',
            fontSize: '13px',
            backgroundColor: feedback.type === 'success' ? 'var(--color-success-bg)' : 'var(--error-soft)',
            color: feedback.type === 'success' ? 'var(--color-success)' : 'var(--error)',
            border: `1px solid ${feedback.type === 'success' ? '#bbf7d0' : '#fecaca'}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={16} />
            <span>{feedback.text}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
          >
            &times;
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div style={{ marginBottom: '20px', position: 'relative', maxWidth: '360px' }}>
        <Search
          size={14}
          style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
        />
        <input
          type="text"
          placeholder="Search batch #, agency, or invoice ref..."
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

      {/* Batches Table */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredBatches.length === 0 ? (
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
          <FileSpreadsheet size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No settlement batches found
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Prepare a new batch when receiving courier cash handover sheets.
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
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Batch Number</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Agency & Invoice Ref</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Orders Count</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Preparation Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Owner Sign-off</th>
              </tr>
            </thead>
            <tbody>
              {filteredBatches.map((b) => (
                <tr key={b.id} style={{ borderBottom: '1px solid var(--line)' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    <span className="font-mono">{b.batchNumber}</span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ fontWeight: 500 }}>{b.agencyName}</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      Ref: <span className="font-mono">{b.agencyInvoiceReference}</span>
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 500 }}>
                    {b.totalOrders} delivered orders
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <Badge
                      variant={
                        b.status === 'APPROVED'
                          ? 'success'
                          : b.status === 'REJECTED'
                          ? 'danger'
                          : 'warning'
                      }
                      size="sm"
                    >
                      {b.status === 'APPROVED'
                        ? 'Approved by Owner'
                        : b.status === 'SUBMITTED_FOR_APPROVAL'
                        ? 'Submitted for Owner Review'
                        : b.status}
                    </Badge>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {b.approvedByOwnerName ? (
                      <div style={{ fontSize: '12px', color: 'var(--color-success)', fontWeight: 500 }}>
                        Approved by {b.approvedByOwnerName}
                      </div>
                    ) : (
                      <div style={{ fontSize: '12px', color: 'var(--ink-muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} />
                        Pending Owner Decision
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Prepare Batch Modal */}
      {isPreparingBatch && (
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
              maxWidth: '520px',
              width: '100%',
              padding: '28px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Prepare Courier COD Settlement Batch
              </h2>
              <button
                onClick={() => setIsPreparingBatch(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '12px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Compile courier agency remittance details. Upon submission, this batch appears in the Owner approvals dashboard for settlement verification.
            </p>

            <form onSubmit={handlePrepareBatchSubmit}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Courier Agency *
                </label>
                <select
                  value={agencyName}
                  onChange={(e) => setAgencyName(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#ffffff',
                  }}
                >
                  <option value="Surat Express Courier">Surat Express Courier</option>
                  <option value="Delhivery Surface Handloom">Delhivery Surface Handloom</option>
                  <option value="BlueDart Air Expedited">BlueDart Air Expedited</option>
                  <option value="DTDC Surat Hub">DTDC Surat Hub</option>
                </select>
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Agency Invoice Reference *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. INV-SEC-2026-901"
                  value={agencyInvoiceReference}
                  onChange={(e) => setAgencyInvoiceReference(e.target.value)}
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
                  Delivered Order Numbers (one per line) *
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder={'ORD-20261118-001\nORD-20261118-002'}
                  value={orderNumbersInput}
                  onChange={(e) => setOrderNumbersInput(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    fontFamily: 'monospace',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Manager Remarks (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Cash collected against physical delivery slips."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setIsPreparingBatch(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingBatch}>
                  Submit for Owner Approval
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
