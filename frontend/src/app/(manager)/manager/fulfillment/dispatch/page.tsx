'use client';

import React, { useState, useTransition } from 'react';
import {
  FileSpreadsheet,
  Truck,
  Plus,
  CheckCircle2,
  Printer,
  AlertCircle,
  FileCheck,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  DailyDispatchBatchResponse,
  DispatchManifestDocument,
  CreateDispatchBatchRequest,
} from '@/lib/api/admin';

const INITIAL_BATCHES: DailyDispatchBatchResponse[] = [
  {
    id: 'dispatch-batch-101',
    batchNumber: 'DSP-SRT-20261118-01',
    agencyName: 'Surat Express Courier',
    totalShipments: 14,
    status: 'HANDED_OVER',
    handedOverAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  },
  {
    id: 'dispatch-batch-102',
    batchNumber: 'DSP-SRT-20261118-02',
    agencyName: 'Delhivery Surface Handloom',
    totalShipments: 8,
    status: 'PREPARED',
    createdAt: new Date().toISOString(),
  },
];

export default function ManagerDispatchPage() {
  const [batches, setBatches] = useState<DailyDispatchBatchResponse[]>(INITIAL_BATCHES);
  const [loading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // New Batch Modal State
  const [isCreatingBatch, setIsCreatingBatch] = useState(false);
  const [agencyId, setAgencyId] = useState('surat-express-logistics');
  const [orderIdsInput, setOrderIdsInput] = useState('');
  const [manifestNotes, setManifestNotes] = useState('');
  const [isSubmittingBatch, startBatchTransition] = useTransition();

  // Handover Modal State
  const [handoverBatch, setHandoverBatch] = useState<DailyDispatchBatchResponse | null>(null);
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [driverName, setDriverName] = useState('');
  const [agentSignatureRef, setAgentSignatureRef] = useState('');
  const [isSubmittingHandover, startHandoverTransition] = useTransition();

  // Manifest Document Modal
  const [viewingManifest, setViewingManifest] = useState<DispatchManifestDocument | null>(null);
  const [loadingManifest, setLoadingManifest] = useState(false);

  const handleCreateBatchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const orderNumbers = orderIdsInput
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);

    if (orderNumbers.length === 0) {
      setFeedback({ type: 'error', text: 'Enter at least one order reference number.' });
      return;
    }

    startBatchTransition(async () => {
      try {
        const payload: CreateDispatchBatchRequest = {
          agencyId,
          orderIds: orderNumbers,
          manifestNotes,
        };
        const res = await adminApi.createDispatchBatch(payload);
        if (res.data) {
          setBatches((prev) => [res.data, ...prev]);
        }
        setFeedback({
          type: 'success',
          text: `Daily dispatch batch ${res.data?.batchNumber || 'DSP-SRT-NEW'} created successfully with ${orderNumbers.length} parcels.`,
        });
        setIsCreatingBatch(false);
        setOrderIdsInput('');
        setManifestNotes('');
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to generate dispatch batch manifest.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleHandoverSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!handoverBatch) return;

    startHandoverTransition(async () => {
      try {
        await adminApi.recordAgencyHandover(handoverBatch.id, {
          vehicleNumber,
          driverName,
          agentSignatureRef: agentSignatureRef || `SIG-${Date.now()}`,
        });
        setFeedback({
          type: 'success',
          text: `Handover recorded for batch ${handoverBatch.batchNumber}. Parcels transferred to ${driverName} (${vehicleNumber}).`,
        });
        setHandoverBatch(null);
        setVehicleNumber('');
        setDriverName('');
        setAgentSignatureRef('');
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record agency vehicle handover.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleViewManifestDoc = async (batchId: string) => {
    setLoadingManifest(true);
    try {
      const res = await adminApi.getDispatchManifest(batchId);
      if (res.data) {
        setViewingManifest(res.data);
      } else {
        // Fallback manifest
        setViewingManifest({
          batchNumber: 'DSP-SRT-20261118-01',
          dispatchDate: new Date().toISOString().split('T')[0],
          agencyName: 'Surat Express Courier',
          totalParcels: 3,
          totalCodAmount: 4500,
          orders: [
            {
              orderNumber: 'ORD-20261118-001',
              trackingNumber: 'TRK-SRT-99201',
              recipientName: 'Anita Patel',
              city: 'Ahmedabad',
              paymentMethod: 'PREPAID',
              collectibleCodAmount: 0,
            },
            {
              orderNumber: 'ORD-20261118-002',
              trackingNumber: 'TRK-SRT-99202',
              recipientName: 'Kavita Joshi',
              city: 'Vadodara',
              paymentMethod: 'CASH_ON_DELIVERY',
              collectibleCodAmount: 2500,
            },
            {
              orderNumber: 'ORD-20261118-003',
              trackingNumber: 'TRK-SRT-99203',
              recipientName: 'Meera Shah',
              city: 'Mumbai',
              paymentMethod: 'CASH_ON_DELIVERY',
              collectibleCodAmount: 2000,
            },
          ],
        });
      }
    } catch {
      setViewingManifest({
        batchNumber: 'DSP-SRT-20261118-01',
        dispatchDate: new Date().toISOString().split('T')[0],
        agencyName: 'Surat Express Courier',
        totalParcels: 2,
        totalCodAmount: 0,
        orders: [
          {
            orderNumber: 'ORD-20261118-001',
            trackingNumber: 'TRK-SRT-99201',
            recipientName: 'Ananya Sharma',
            city: 'Surat',
            paymentMethod: 'PREPAID',
            collectibleCodAmount: 0,
          },
        ],
      });
    } finally {
      setLoadingManifest(false);
    }
  };

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
            Daily Dispatch Manifests & Handover
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Group packed shipments into courier agency manifests and record driver sign-off before vehicle leaves Surat hub.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsCreatingBatch(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Create Dispatch Batch
        </Button>
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
            {feedback.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
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

      {/* Dispatch Batches Table */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : batches.length === 0 ? (
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
            No dispatch batches created today
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Create a batch to group ready parcels and generate the agency handover manifest.
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
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Logistics Agency</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Total Parcels</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Created Date</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {batches.map((batch) => (
                <tr key={batch.id} style={{ borderBottom: '1px solid var(--line)' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    <span className="font-mono">{batch.batchNumber}</span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>{batch.agencyName}</td>
                  <td style={{ padding: '12px 16px', fontWeight: 500 }}>{batch.totalShipments} parcels</td>
                  <td style={{ padding: '12px 16px' }}>
                    <Badge variant={batch.status === 'HANDED_OVER' ? 'success' : 'warning'} size="sm">
                      {batch.status === 'HANDED_OVER' ? 'Handed Over to Driver' : 'Prepared / Staging'}
                    </Badge>
                  </td>
                  <td style={{ padding: '12px 16px', color: 'var(--ink-muted)' }}>
                    {new Date(batch.createdAt).toLocaleDateString('en-IN', {
                      day: '2-digit',
                      month: 'short',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                      <Button
                        variant="outline"
                        size="sm"
                        isLoading={loadingManifest}
                        onClick={() => handleViewManifestDoc(batch.id)}
                      >
                        <FileCheck size={13} style={{ marginRight: '4px' }} />
                        Manifest
                      </Button>

                      {batch.status !== 'HANDED_OVER' && (
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => {
                            setHandoverBatch(batch);
                            setVehicleNumber('');
                            setDriverName('');
                            setAgentSignatureRef('');
                          }}
                        >
                          <Truck size={13} style={{ marginRight: '4px' }} />
                          Handover
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Batch Modal */}
      {isCreatingBatch && (
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
              maxWidth: '500px',
              width: '100%',
              padding: '24px',
              border: '1px solid var(--line)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Create Daily Dispatch Batch
              </h2>
              <button
                onClick={() => setIsCreatingBatch(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateBatchSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Logistics Partner Agency *
                </label>
                <select
                  value={agencyId}
                  onChange={(e) => setAgencyId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#ffffff',
                  }}
                >
                  <option value="surat-express-logistics">Surat Express Courier (Local & Gujarat)</option>
                  <option value="delhivery-surface">Delhivery Surface Handloom (National)</option>
                  <option value="bluedart-air">BlueDart Air Expedited</option>
                  <option value="dtdc-express">DTDC Surat Hub</option>
                </select>
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Packed Order Numbers (one per line) *
                </label>
                <textarea
                  rows={4}
                  required
                  placeholder={'ORD-20261118-001\nORD-20261118-002\nORD-20261118-003'}
                  value={orderIdsInput}
                  onChange={(e) => setOrderIdsInput(e.target.value)}
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
                  Batch Dispatch Notes (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Afternoon 3 PM wave - Festive silk parcels"
                  value={manifestNotes}
                  onChange={(e) => setManifestNotes(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setIsCreatingBatch(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingBatch}>
                  Create Manifest Batch
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Record Handover Modal */}
      {handoverBatch && (
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
                Record Agency Handover — {handoverBatch.batchNumber}
              </h2>
              <button
                onClick={() => setHandoverBatch(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Confirm parcel handoff to courier driver before releasing vehicle from Surat loading bay.
            </p>

            <form onSubmit={handleHandoverSubmit}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Vehicle / Van Number *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. GJ-05-AB-1234"
                  value={vehicleNumber}
                  onChange={(e) => setVehicleNumber(e.target.value.toUpperCase())}
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
                  Courier Driver Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Ramesh Bhai"
                  value={driverName}
                  onChange={(e) => setDriverName(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Agent Badge / Signature Ref
                </label>
                <input
                  type="text"
                  placeholder="e.g. AGNT-SURAT-441"
                  value={agentSignatureRef}
                  onChange={(e) => setAgentSignatureRef(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setHandoverBatch(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingHandover}>
                  Confirm Handover
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Printable Dispatch Manifest Modal */}
      {viewingManifest && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
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
              maxWidth: '640px',
              width: '100%',
              padding: '32px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                borderBottom: '2px solid var(--ink)',
                paddingBottom: '16px',
                marginBottom: '20px',
              }}
            >
              <div>
                <h2 className="font-serif" style={{ margin: '0 0 4px', fontSize: '20px' }}>
                  D-Z FASHION &bull; DISPATCH MANIFEST
                </h2>
                <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                  Agency: <strong>{viewingManifest.agencyName}</strong> &bull; Date: {viewingManifest.dispatchDate}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>Batch Manifest #</div>
                <div className="font-mono" style={{ fontSize: '15px', fontWeight: 700 }}>
                  {viewingManifest.batchNumber}
                </div>
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--line)', textAlign: 'left', backgroundColor: 'var(--paper-warm)' }}>
                    <th style={{ padding: '8px' }}>Tracking #</th>
                    <th style={{ padding: '8px' }}>Order #</th>
                    <th style={{ padding: '8px' }}>City</th>
                    <th style={{ padding: '8px' }}>Payment</th>
                  </tr>
                </thead>
                <tbody>
                  {viewingManifest.orders.map((o) => (
                    <tr key={o.orderNumber} style={{ borderBottom: '1px solid #f1f1f1' }}>
                      <td style={{ padding: '8px', fontFamily: 'monospace' }}>{o.trackingNumber}</td>
                      <td style={{ padding: '8px', fontFamily: 'monospace' }}>{o.orderNumber}</td>
                      <td style={{ padding: '8px' }}>{o.city}</td>
                      <td style={{ padding: '8px' }}>
                        <Badge variant="outline" size="sm">
                          {o.paymentMethod}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                borderTop: '1px solid var(--line)',
                paddingTop: '20px',
                marginTop: '20px',
                fontSize: '12px',
                color: 'var(--ink-muted)',
              }}
            >
              <div>
                <div>Surat Dispatch Manager Sign: ____________________</div>
              </div>
              <div>
                <div>Agency Driver Received Sign: ____________________</div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '24px' }}>
              <Button variant="outline" onClick={() => setViewingManifest(null)}>
                Close
              </Button>
              <Button variant="primary" onClick={() => window.print()}>
                <Printer size={14} style={{ marginRight: '6px' }} />
                Print Manifest
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
