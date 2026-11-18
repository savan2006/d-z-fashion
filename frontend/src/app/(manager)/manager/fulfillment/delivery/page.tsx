'use client';

import React, { useState, useTransition } from 'react';
import {
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Search,
  ShieldAlert,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import {
  adminApi,
  RecordDeliveryAttemptRequest,
} from '@/lib/api/admin';

interface ShipmentAttemptItem {
  shipmentId: string;
  orderNumber: string;
  trackingNumber: string;
  courierAgency: string;
  recipientCity: string;
  currentAttempts: number;
  lastAttemptReason?: string;
  lastAttemptDate?: string;
  status: 'OUT_FOR_DELIVERY' | 'ATTEMPT_1_FAILED' | 'ATTEMPT_2_FAILED_RTO' | 'DELIVERED';
}

const INITIAL_SHIPMENTS: ShipmentAttemptItem[] = [
  {
    shipmentId: 'ship-101',
    orderNumber: 'ORD-20261118-001',
    trackingNumber: 'TRK-SRT-99201',
    courierAgency: 'Surat Express',
    recipientCity: 'Ahmedabad',
    currentAttempts: 0,
    status: 'OUT_FOR_DELIVERY',
  },
  {
    shipmentId: 'ship-102',
    orderNumber: 'ORD-20261118-002',
    trackingNumber: 'TRK-SRT-99202',
    courierAgency: 'Delhivery Surface',
    recipientCity: 'Vadodara',
    currentAttempts: 1,
    lastAttemptReason: 'Customer door locked / phone unreachable',
    lastAttemptDate: '2026-11-17',
    status: 'ATTEMPT_1_FAILED',
  },
  {
    shipmentId: 'ship-103',
    orderNumber: 'ORD-20261118-003',
    trackingNumber: 'TRK-SRT-99203',
    courierAgency: 'BlueDart Air',
    recipientCity: 'Mumbai',
    currentAttempts: 2,
    lastAttemptReason: 'Customer refused COD payment on 2nd attempt',
    lastAttemptDate: '2026-11-18',
    status: 'ATTEMPT_2_FAILED_RTO',
  },
];

export default function ManagerDeliveryAttemptsPage() {
  const [shipments, setShipments] = useState<ShipmentAttemptItem[]>(INITIAL_SHIPMENTS);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'warning' | 'error'; text: string } | null>(null);

  // Attempt Logger Modal State
  const [activeShipment, setActiveShipment] = useState<ShipmentAttemptItem | null>(null);
  const [attemptNumber, setAttemptNumber] = useState<1 | 2>(1);
  const [failureReason, setFailureReason] = useState('Customer unavailable / door locked');
  const [courierNotes, setCourierNotes] = useState('');
  const [isSubmittingAttempt, startAttemptTransition] = useTransition();

  // Success Confirmation Modal
  const [successShipment, setSuccessShipment] = useState<ShipmentAttemptItem | null>(null);
  const [receiverName, setReceiverName] = useState('');
  const [signatureConfirmed, setSignatureConfirmed] = useState(true);
  const [isSubmittingSuccess, startSuccessTransition] = useTransition();

  // Warehouse Return Intake Modal
  const [rtoShipment, setRtoShipment] = useState<ShipmentAttemptItem | null>(null);
  const [intakeReason, setIntakeReason] = useState('Failed 2 delivery attempts (RTO returned to Surat)');
  const [conditionSummary, setConditionSummary] = useState('Packaging intact, security seal unbroken');
  const [isSubmittingIntake, startIntakeTransition] = useTransition();

  const handleRecordAttemptSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeShipment) return;

    startAttemptTransition(async () => {
      try {
        const payload: RecordDeliveryAttemptRequest = {
          attemptNumber,
          failureReason,
          attemptDate: new Date().toISOString(),
          courierNotes,
        };
        await adminApi.recordDeliveryAttempt(activeShipment.shipmentId, payload);

        setShipments((prev) =>
          prev.map((s) => {
            if (s.shipmentId === activeShipment.shipmentId) {
              const newAttempts = attemptNumber;
              const isRTO = newAttempts >= 2;
              return {
                ...s,
                currentAttempts: newAttempts,
                lastAttemptReason: failureReason,
                lastAttemptDate: new Date().toISOString().split('T')[0],
                status: isRTO ? 'ATTEMPT_2_FAILED_RTO' : 'ATTEMPT_1_FAILED',
              };
            }
            return s;
          })
        );

        if (attemptNumber >= 2) {
          setFeedback({
            type: 'warning',
            text: `Attempt 2 failed for #${activeShipment.orderNumber}. Parcel exceeded 2 delivery attempts and is now routed for Surat warehouse return intake.`,
          });
        } else {
          setFeedback({
            type: 'success',
            text: `Delivery attempt 1 recorded for #${activeShipment.orderNumber}. One attempt remaining before return intake.`,
          });
        }

        setActiveShipment(null);
        setCourierNotes('');
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record delivery attempt.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleRecordSuccessSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!successShipment || !receiverName.trim()) return;

    startSuccessTransition(async () => {
      try {
        await adminApi.recordDeliverySuccess(successShipment.shipmentId, {
          receiverName: receiverName.trim(),
          signatureConfirmed,
        });

        setShipments((prev) =>
          prev.map((s) => {
            if (s.shipmentId === successShipment.shipmentId) {
              return {
                ...s,
                status: 'DELIVERED',
              };
            }
            return s;
          })
        );

        setFeedback({
          type: 'success',
          text: `Shipment #${successShipment.orderNumber} successfully marked as DELIVERED to ${receiverName}.`,
        });

        setSuccessShipment(null);
        setReceiverName('');
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record delivery success.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleReturnIntakeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!rtoShipment) return;

    startIntakeTransition(async () => {
      try {
        await adminApi.receiveReturnAtWarehouse(rtoShipment.shipmentId, {
          intakeReason,
          conditionSummary,
        });

        setShipments((prev) => prev.filter((s) => s.shipmentId !== rtoShipment.shipmentId));

        setFeedback({
          type: 'success',
          text: `Return parcel #${rtoShipment.orderNumber} successfully ingested into Surat warehouse intake rack for quality inspection.`,
        });

        setRtoShipment(null);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to ingest return parcel at warehouse.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredShipments = shipments.filter(
    (s) =>
      s.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.trackingNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.recipientCity.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
          Courier Handover & Delivery Attempt Logger
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
          Enforces Surat standard rule: maximum 2 delivery attempts. On second failure, parcel is automatically routed for return intake.
        </p>
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
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor:
              feedback.type === 'success'
                ? 'var(--color-success-bg)'
                : feedback.type === 'warning'
                ? '#fffbeb'
                : 'var(--error-soft)',
            color:
              feedback.type === 'success'
                ? 'var(--color-success)'
                : feedback.type === 'warning'
                ? '#b45309'
                : 'var(--error)',
            border: `1px solid ${
              feedback.type === 'success'
                ? '#bbf7d0'
                : feedback.type === 'warning'
                ? '#fde68a'
                : '#fecaca'
            }`,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {feedback.type === 'success' && <CheckCircle2 size={16} />}
            {feedback.type === 'warning' && <AlertTriangle size={16} />}
            {feedback.type === 'error' && <ShieldAlert size={16} />}
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
          placeholder="Search order #, tracking #, or city..."
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

      {/* Shipments Table */}
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
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Tracking & Order #</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Agency & City</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Attempt Count</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status / Last Reason</th>
              <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredShipments.map((s) => (
              <tr key={s.shipmentId} style={{ borderBottom: '1px solid var(--line)' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                  <span className="font-mono">{s.trackingNumber}</span>
                  <div style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 400 }}>
                    Order: #{s.orderNumber}
                  </div>
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <div style={{ fontWeight: 500 }}>{s.courierAgency}</div>
                  <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>{s.recipientCity}</div>
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <span
                    style={{
                      fontWeight: 600,
                      color: s.currentAttempts >= 2 ? 'var(--error)' : s.currentAttempts === 1 ? '#b45309' : 'var(--ink)',
                    }}
                  >
                    {s.currentAttempts} / 2 attempts
                  </span>
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <div style={{ marginBottom: '2px' }}>
                    <Badge
                      variant={
                        s.status === 'DELIVERED'
                          ? 'success'
                          : s.status === 'ATTEMPT_2_FAILED_RTO'
                          ? 'danger'
                          : s.status === 'ATTEMPT_1_FAILED'
                          ? 'warning'
                          : 'secondary'
                      }
                      size="sm"
                    >
                      {s.status === 'ATTEMPT_2_FAILED_RTO'
                        ? 'RTO (2 Failed Attempts)'
                        : s.status === 'ATTEMPT_1_FAILED'
                        ? '1 Attempt Failed'
                        : s.status === 'DELIVERED'
                        ? 'Delivered'
                        : 'Out For Delivery'}
                    </Badge>
                  </div>
                  {s.lastAttemptReason && (
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)', maxWidth: '240px' }}>
                      {s.lastAttemptReason}
                    </div>
                  )}
                </td>
                <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                    {s.status !== 'DELIVERED' && s.status !== 'ATTEMPT_2_FAILED_RTO' && (
                      <>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setActiveShipment(s);
                            setAttemptNumber((s.currentAttempts + 1) as 1 | 2);
                            setFailureReason('Customer unavailable / door locked');
                            setCourierNotes('');
                          }}
                        >
                          Record Failed Attempt
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => {
                            setSuccessShipment(s);
                            setReceiverName('');
                            setSignatureConfirmed(true);
                          }}
                        >
                          Confirm Delivery
                        </Button>
                      </>
                    )}

                    {s.status === 'ATTEMPT_2_FAILED_RTO' && (
                      <Button
                        variant="primary"
                        size="sm"
                        style={{ backgroundColor: 'var(--wine)', borderColor: 'var(--wine)' }}
                        onClick={() => {
                          setRtoShipment(s);
                          setIntakeReason('Failed 2 delivery attempts (RTO returned to Surat)');
                          setConditionSummary('Packaging intact, security seal unbroken');
                        }}
                      >
                        <RotateCcw size={13} style={{ marginRight: '4px' }} />
                        Intake at Warehouse
                      </Button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Record Failed Attempt Modal */}
      {activeShipment && (
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
                Record Delivery Attempt — {activeShipment.trackingNumber}
              </h2>
              <button
                onClick={() => setActiveShipment(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleRecordAttemptSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Attempt Sequence *
                </label>
                <div style={{ display: 'flex', gap: '12px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}>
                    <input
                      type="radio"
                      name="attemptNum"
                      checked={attemptNumber === 1}
                      onChange={() => setAttemptNumber(1)}
                    />
                    Attempt 1
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}>
                    <input
                      type="radio"
                      name="attemptNum"
                      checked={attemptNumber === 2}
                      onChange={() => setAttemptNumber(2)}
                    />
                    Attempt 2 (Final &bull; routes to RTO if failed)
                  </label>
                </div>
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Courier Failure Reason *
                </label>
                <select
                  value={failureReason}
                  onChange={(e) => setFailureReason(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#ffffff',
                  }}
                >
                  <option value="Customer unavailable / door locked">Customer unavailable / door locked</option>
                  <option value="Customer phone switched off / unreachable">Customer phone switched off / unreachable</option>
                  <option value="Incorrect address / landmark missing">Incorrect address / landmark missing</option>
                  <option value="Customer requested reschedule">Customer requested reschedule</option>
                  <option value="Customer refused COD cash payment">Customer refused COD cash payment</option>
                  <option value="Customer cancelled order at door">Customer cancelled order at door</option>
                </select>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Courier Driver Remarks
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Guard confirmed recipient was out of town."
                  value={courierNotes}
                  onChange={(e) => setCourierNotes(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setActiveShipment(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingAttempt}>
                  Save Attempt Record
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirm Delivery Modal */}
      {successShipment && (
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
              maxWidth: '460px',
              width: '100%',
              padding: '24px',
              border: '1px solid var(--line)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Confirm Delivery — {successShipment.trackingNumber}
              </h2>
              <button
                onClick={() => setSuccessShipment(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleRecordSuccessSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Receiver Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Anita Patel (Self / Family)"
                  value={receiverName}
                  onChange={(e) => setReceiverName(e.target.value)}
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
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={signatureConfirmed}
                    onChange={(e) => setSignatureConfirmed(e.target.checked)}
                  />
                  OTP / Physical Signature verified by courier
                </label>
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setSuccessShipment(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingSuccess}>
                  Confirm Delivered
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Warehouse Return Intake Modal */}
      {rtoShipment && (
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
                Intake Returned Parcel at Surat Warehouse
              </h2>
              <button
                onClick={() => setRtoShipment(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Log the return arrival of Order #{rtoShipment.orderNumber} ({rtoShipment.trackingNumber}) into the Surat inspection queue.
            </p>

            <form onSubmit={handleReturnIntakeSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Intake Reason
                </label>
                <input
                  type="text"
                  required
                  value={intakeReason}
                  onChange={(e) => setIntakeReason(e.target.value)}
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
                  Condition Summary
                </label>
                <textarea
                  rows={2}
                  required
                  value={conditionSummary}
                  onChange={(e) => setConditionSummary(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setRtoShipment(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingIntake}>
                  Confirm Warehouse Intake
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
