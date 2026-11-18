'use client';

import React, { useEffect, useState, useTransition } from 'react';
import {
  RotateCcw,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Truck,
  ShieldCheck,
  Search,
  Send,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  ReturnItemStaffResponse,
  PerformInspectionRequest,
} from '@/lib/api/admin';

export default function ManagerReturnsPage() {
  const [returns, setReturns] = useState<ReturnItemStaffResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error' | 'warning'; text: string } | null>(null);

  // Inspection Modal State
  const [inspectingReturn, setInspectingReturn] = useState<ReturnItemStaffResponse | null>(null);
  const [passedInspection, setPassedInspection] = useState(true);
  const [checklistTags, setChecklistTags] = useState(true);
  const [checklistOdor, setChecklistOdor] = useState(true);
  const [checklistStains, setChecklistStains] = useState(true);
  const [checklistZari, setChecklistZari] = useState(true);
  const [inspectionNotes, setInspectionNotes] = useState('');
  const [isSubmittingInspection, startInspectionTransition] = useTransition();

  // Refund Forwarding Modal State
  const [forwardRefundReturn, setForwardRefundReturn] = useState<ReturnItemStaffResponse | null>(null);
  const [recommendationNotes, setRecommendationNotes] = useState('');
  const [isSubmittingRefundReq, startRefundReqTransition] = useTransition();

  // Exchange Dispatch State
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);

  const loadReturns = () => {
    adminApi
      .getAllReturns(0, 50)
      .then((res) => {
        if (res.data?.content) {
          setReturns(res.data.content);
        } else {
          // Fallback mock returns for Surat operations
          setReturns([
            {
              id: 'ret-101',
              returnNumber: 'RET-20261118-01',
              orderNumber: 'ORD-20261115-08',
              customerId: 'cust-1',
              customerName: 'Meera Trivedi',
              sku: 'SRT-SLK-RED-M',
              productName: 'Surat Pure Silk Zari Saree',
              quantity: 1,
              originalUnitPrice: 4500,
              reason: 'Size fitting issue, requesting size L replacement',
              resolutionType: 'EXCHANGE',
              status: 'ARRIVED_AT_WAREHOUSE',
              photoUrls: [],
              customerNotes: 'Please exchange for size L Crimson Red',
              createdAt: '2026-11-17T11:00:00Z',
            },
            {
              id: 'ret-102',
              returnNumber: 'RET-20261118-02',
              orderNumber: 'ORD-20261114-19',
              customerId: 'cust-2',
              customerName: 'Kavita Joshi',
              sku: 'KRT-COT-YEL-S',
              productName: 'Chanderi Cotton Kurti',
              quantity: 1,
              originalUnitPrice: 1800,
              reason: 'Fabric weave uneven on sleeve hem',
              resolutionType: 'ORIGINAL_PAYMENT_REFUND',
              status: 'INSPECTION_PASSED',
              inspectionPassed: true,
              inspectionNotes: 'Weave flaw verified under Surat inspection light. Eligible for refund.',
              photoUrls: [],
              refundApproved: false,
              createdAt: '2026-11-16T14:00:00Z',
            },
            {
              id: 'ret-103',
              returnNumber: 'RET-20261118-03',
              orderNumber: 'ORD-20261116-04',
              customerId: 'cust-3',
              customerName: 'Priya Sharma',
              sku: 'SRT-SLK-BLU-L',
              productName: 'Surat Handloom Royal Blue Saree',
              quantity: 1,
              originalUnitPrice: 5200,
              reason: 'Changed mind, requesting store credit',
              resolutionType: 'STORE_CREDIT_REFUND',
              status: 'PICKUP_SCHEDULED',
              photoUrls: [],
              createdAt: '2026-11-18T08:00:00Z',
            },
          ]);
        }
      })
      .catch((err) => {
        setFeedback({
          type: 'error',
          text: err?.message || 'Failed to fetch returns queue.',
        });
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadReturns();
  }, []);

  const handleRecordArrival = async (id: string, returnNumber: string) => {
    setActionInProgress(id);
    try {
      await adminApi.recordReturnArrival(id);
      setFeedback({
        type: 'success',
        text: `Return parcel #${returnNumber} scanned and marked ARRIVED at Surat warehouse intake.`,
      });
      loadReturns();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to record return arrival.';
      setFeedback({
        type: 'error',
        text: msg,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handlePerformInspectionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inspectingReturn) return;

    startInspectionTransition(async () => {
      try {
        const fullNotes = [
          `Tags Attached: ${checklistTags ? 'YES' : 'NO'}`,
          `No Perfume/Odor: ${checklistOdor ? 'PASS' : 'FAIL'}`,
          `No Stains/Makeup: ${checklistStains ? 'PASS' : 'FAIL'}`,
          `Zari/Embroidery Intact: ${checklistZari ? 'PASS' : 'FAIL'}`,
          inspectionNotes.trim(),
        ].filter(Boolean).join(' | ');

        const payload: PerformInspectionRequest = {
          passed: passedInspection && checklistTags && checklistOdor && checklistStains && checklistZari,
          inspectionNotes: fullNotes,
        };

        await adminApi.performInspection(inspectingReturn.id, payload);

        setFeedback({
          type: payload.passed ? 'success' : 'warning',
          text: `Inspection completed for #${inspectingReturn.returnNumber}. Result: ${payload.passed ? 'PASSED — Ready for exchange or refund approval' : 'FAILED — Condition policy breach recorded'}.`,
        });

        setInspectingReturn(null);
        setInspectionNotes('');
        loadReturns();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record quality inspection.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleDispatchExchange = async (id: string, returnNumber: string) => {
    setActionInProgress(id);
    try {
      await adminApi.dispatchExchangeReplacement(id);
      setFeedback({
        type: 'success',
        text: `Exchange replacement garment dispatched for #${returnNumber}. New tracking allocated.`,
      });
      loadReturns();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to dispatch exchange replacement.';
      setFeedback({
        type: 'error',
        text: msg,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleForwardRefundSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!forwardRefundReturn) return;

    startRefundReqTransition(async () => {
      try {
        await adminApi.requestRefund(forwardRefundReturn.id, recommendationNotes.trim());
        setFeedback({
          type: 'success',
          text: `Refund request for #${forwardRefundReturn.returnNumber} forwarded to Owner financial approval queue.`,
        });
        setForwardRefundReturn(null);
        setRecommendationNotes('');
        loadReturns();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to forward refund request.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredReturns = returns.filter(
    (r) =>
      r.returnNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      r.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.customerName && r.customerName.toLowerCase().includes(searchQuery.toLowerCase())) ||
      r.sku.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
          Surat Return Intake & 3-Day Quality Inspection
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
          Physical inspection checklist for returned handloom garments. Verified items can trigger exchange dispatch or forward to Owner for refund authorization.
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
              feedback.type === 'success' ? '#bbf7d0' : feedback.type === 'warning' ? '#fde68a' : '#fecaca'
            }`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {feedback.type === 'success' && <CheckCircle2 size={16} />}
            {feedback.type === 'warning' && <AlertCircle size={16} />}
            {feedback.type === 'error' && <XCircle size={16} />}
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
          placeholder="Search return #, order #, or customer..."
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

      {/* Returns Queue Table (Manager Zero-Finance: NO prices/revenue!) */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredReturns.length === 0 ? (
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
          <RotateCcw size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No pending returns in queue
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Surat warehouse return intake is clear.
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
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Return #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment & Customer</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Resolution Type</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Inspection</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredReturns.map((ret) => {
                const isActing = actionInProgress === ret.id;

                return (
                  <tr key={ret.id} style={{ borderBottom: '1px solid var(--line)' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                      <span className="font-mono">{ret.returnNumber}</span>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 400 }}>
                        Order: #{ret.orderNumber}
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ fontWeight: 500 }}>{ret.productName}</div>
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                        SKU: {ret.sku} &bull; Customer: {ret.customerName || 'Valued Client'}
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge variant="outline" size="sm">
                        {ret.resolutionType}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge
                        variant={
                          ret.status === 'EXCHANGE_DISPATCHED' || ret.status === 'REFUNDED'
                            ? 'success'
                            : ret.status === 'INSPECTION_FAILED'
                            ? 'danger'
                            : 'warning'
                        }
                        size="sm"
                      >
                        {ret.status}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {ret.inspectionPassed === true ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--color-success)', fontWeight: 600 }}>
                          <CheckCircle2 size={14} />
                          <span>Passed</span>
                        </div>
                      ) : ret.inspectionPassed === false ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--error)', fontWeight: 600 }}>
                          <XCircle size={14} />
                          <span>Failed</span>
                        </div>
                      ) : (
                        <span style={{ color: 'var(--ink-muted)' }}>Pending Physical Check</span>
                      )}
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                        {ret.status === 'PICKUP_SCHEDULED' && (
                          <Button
                            variant="outline"
                            size="sm"
                            isLoading={isActing}
                            onClick={() => handleRecordArrival(ret.id, ret.returnNumber)}
                          >
                            Scan Arrival
                          </Button>
                        )}

                        {ret.status === 'ARRIVED_AT_WAREHOUSE' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              setInspectingReturn(ret);
                              setPassedInspection(true);
                              setChecklistTags(true);
                              setChecklistOdor(true);
                              setChecklistStains(true);
                              setChecklistZari(true);
                              setInspectionNotes('');
                            }}
                          >
                            <ShieldCheck size={13} style={{ marginRight: '4px' }} />
                            Inspect Garment
                          </Button>
                        )}

                        {ret.status === 'INSPECTION_PASSED' && ret.resolutionType === 'EXCHANGE' && (
                          <Button
                            variant="primary"
                            size="sm"
                            isLoading={isActing}
                            onClick={() => handleDispatchExchange(ret.id, ret.returnNumber)}
                          >
                            <Truck size={13} style={{ marginRight: '4px' }} />
                            Dispatch Exchange
                          </Button>
                        )}

                        {ret.status === 'INSPECTION_PASSED' &&
                          ret.resolutionType !== 'EXCHANGE' &&
                          !ret.refundApproved && (
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() => {
                                setForwardRefundReturn(ret);
                                setRecommendationNotes('Quality checklist passed. Garment returned to rack.');
                              }}
                            >
                              <Send size={13} style={{ marginRight: '4px' }} />
                              Forward to Owner for Refund
                            </Button>
                          )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Physical Inspection Checklist Modal */}
      {inspectingReturn && (
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
                Physical Quality Inspection — #{inspectingReturn.returnNumber}
              </h2>
              <button
                onClick={() => setInspectingReturn(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '12px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Verify garment against Surat craft return standards within 3 days of arrival.
            </p>

            <form onSubmit={handlePerformInspectionSubmit}>
              <div
                style={{
                  backgroundColor: 'var(--paper-warm)',
                  padding: '16px',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: '16px',
                }}
              >
                <div style={{ fontSize: '12px', fontWeight: 600, marginBottom: '10px' }}>
                  Surat Quality Standards Checklist
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={checklistTags}
                      onChange={(e) => setChecklistTags(e.target.checked)}
                    />
                    Original brand security tags attached & untampered
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={checklistOdor}
                      onChange={(e) => setChecklistOdor(e.target.checked)}
                    />
                    No perfume, deodorant, wash, or body odors
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={checklistStains}
                      onChange={(e) => setChecklistStains(e.target.checked)}
                    />
                    Zero makeup, food, oil, or water stains
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={checklistZari}
                      onChange={(e) => setChecklistZari(e.target.checked)}
                    />
                    Zari embroidery, borders, and handloom weave unpulled
                  </label>
                </div>
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Overall Inspection Verdict *
                </label>
                <div style={{ display: 'flex', gap: '16px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="verdict"
                      checked={passedInspection}
                      onChange={() => setPassedInspection(true)}
                    />
                    PASS (Eligible for Exchange / Refund)
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="verdict"
                      checked={!passedInspection}
                      onChange={() => setPassedInspection(false)}
                    />
                    FAIL (Reject Return)
                  </label>
                </div>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Inspector Remarks
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Fabric in pristine condition, fold lines preserved, approved for restock."
                  value={inspectionNotes}
                  onChange={(e) => setInspectionNotes(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setInspectingReturn(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingInspection}>
                  Record Inspection Verdict
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Forward Refund Recommendation to Owner Modal */}
      {forwardRefundReturn && (
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
                Forward Refund Request to Owner
              </h2>
              <button
                onClick={() => setForwardRefundReturn(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Refund disbursements are governed by Owner approval within the 7-day SLA. Forward verified return #{forwardRefundReturn.returnNumber}.
            </p>

            <form onSubmit={handleForwardRefundSubmit}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Staff Recommendation Notes
                </label>
                <textarea
                  rows={3}
                  required
                  value={recommendationNotes}
                  onChange={(e) => setRecommendationNotes(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setForwardRefundReturn(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingRefundReq}>
                  Forward to Owner
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
