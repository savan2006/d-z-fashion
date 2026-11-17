'use client';

import React, { Suspense, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import {
  customerApi,
  ReturnRequestResponse,
  ReturnReason,
  ReturnResolutionType,
  PageResponse,
  OrderResponse,
} from '@/lib/api/customer';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import {
  RefreshCw,
  Camera,
  ShieldCheck,
  ArrowRight,
  ArrowLeft,
} from 'lucide-react';

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

const REASON_OPTIONS = [
  { value: 'DEFECTIVE_DAMAGED', label: 'Defective or Damaged Weave / Fabric' },
  { value: 'WRONG_ITEM_SENT', label: 'Incorrect Item / Colour / Size Received' },
  { value: 'QUALITY_UNSATISFACTORY', label: 'Quality Below Luxury Standard' },
  { value: 'SIZE_FIT_ISSUE', label: 'Size / Fit Discrepancy' },
];

const RESOLUTION_OPTIONS = [
  { value: 'EXCHANGE', label: 'Exchange for Replacement Piece' },
  { value: 'STORE_CREDIT_REFUND', label: 'Instant Store Credit (Never Expires)' },
  { value: 'ORIGINAL_PAYMENT_REFUND', label: 'Refund to Original Payment Method' },
];

function ReturnsContent() {
  const searchParams = useSearchParams();
  const prefillOrderNumber = searchParams.get('orderNumber') || '';
  const prefillSku = searchParams.get('sku') || '';

  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState<'LIST' | 'CREATE'>('LIST');

  // Returns list state
  const [returnsPage, setReturnsPage] = useState<PageResponse<ReturnRequestResponse> | null>(null);
  const [pageLoading, setPageLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);

  // New return form state
  const [orderNumberInput, setOrderNumberInput] = useState(prefillOrderNumber);
  const [fetchedOrder, setFetchedOrder] = useState<OrderResponse | null>(null);
  const [fetchingOrder, setFetchingOrder] = useState(false);
  const [selectedItemId, setSelectedItemId] = useState('');
  const [reason, setReason] = useState<ReturnReason>('DEFECTIVE_DAMAGED');
  const [resolutionType, setResolutionType] = useState<ReturnResolutionType>('EXCHANGE');
  const [customerNotes, setCustomerNotes] = useState('');
  const [photoInput, setPhotoInput] = useState('');
  const [photoUrls, setPhotoUrls] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  // Load existing returns
  const loadReturns = (page = 0) => {
    customerApi.getMyReturns(page, 10)
      .then((res) => {
        if (res.data) setReturnsPage(res.data);
      })
      .catch(() => {})
      .finally(() => setPageLoading(false));
  };

  useEffect(() => {
    let isMounted = true;
    customerApi.getMyReturns(currentPage, 10)
      .then((res) => {
        if (isMounted && res.data) setReturnsPage(res.data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setPageLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  // Handle prefill
  useEffect(() => {
    if (prefillOrderNumber) {
      let isMounted = true;
      customerApi.getOrderDetails(prefillOrderNumber.trim())
        .then((res) => {
          if (!isMounted || !res.data) return;
          setFetchedOrder(res.data);
          setActiveTab('CREATE');
          if (res.data.items.length > 0) {
            if (prefillSku) {
              const matched = res.data.items.find((it) => it.sku === prefillSku);
              setSelectedItemId(matched ? matched.id : res.data.items[0].id);
            } else {
              setSelectedItemId(res.data.items[0].id);
            }
          }
        })
        .catch(() => {});

      return () => {
        isMounted = false;
      };
    }
  }, [prefillOrderNumber, prefillSku]);

  // Order lookup helper
  const lookupOrder = async (num: string, skuToSelect?: string) => {
    if (!num.trim()) return;
    setFetchingOrder(true);
    try {
      const res = await customerApi.getOrderDetails(num.trim());
      if (res.data) {
        setFetchedOrder(res.data);
        if (res.data.items.length > 0) {
          if (skuToSelect) {
            const matched = res.data.items.find((it) => it.sku === skuToSelect);
            setSelectedItemId(matched ? matched.id : res.data.items[0].id);
          } else {
            setSelectedItemId(res.data.items[0].id);
          }
        }
      }
    } catch {
      toast({ type: 'error', title: 'Order Lookup Failed', message: `Could not verify order #${num}.` });
    } finally {
      setFetchingOrder(false);
    }
  };

  const handleAddPhoto = () => {
    if (!photoInput.trim()) return;
    if (photoUrls.includes(photoInput.trim())) return;
    setPhotoUrls([...photoUrls, photoInput.trim()]);
    setPhotoInput('');
  };

  const handleRemovePhoto = (index: number) => {
    setPhotoUrls(photoUrls.filter((_, i) => i !== index));
  };

  const handleSubmitReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fetchedOrder) {
      toast({ type: 'warning', title: 'Order Required', message: 'Please look up and verify your delivered order.' });
      return;
    }

    const selectedItem = fetchedOrder.items.find((it) => it.id === selectedItemId);
    if (!selectedItem) {
      toast({ type: 'warning', title: 'Item Required', message: 'Please select which item you wish to return.' });
      return;
    }

    // MANDATORY PHOTO ENFORCEMENT
    if (photoUrls.length === 0) {
      toast({
        type: 'warning',
        title: 'Photo Evidence Required',
        message: 'Quality policy requires at least 1 inspection photo before return request submission.',
      });
      return;
    }

    setSubmitting(true);
    try {
      await customerApi.createReturnRequest({
        orderId: fetchedOrder.id,
        orderNumber: fetchedOrder.orderNumber,
        orderItemId: selectedItem.id,
        sku: selectedItem.sku,
        productName: selectedItem.productName,
        quantity: selectedItem.quantity,
        originalUnitPrice: selectedItem.unitPrice,
        reason,
        resolutionType,
        photoUrls,
        customerNotes: customerNotes.trim() || undefined,
      });

      toast({
        type: 'success',
        title: 'Return Request Logged',
        message: 'Our Surat quality inspection team has received your request. Pickup details will be sent via SMS/Email.',
      });

      // Reset form and return to list
      setActiveTab('LIST');
      setPhotoUrls([]);
      setCustomerNotes('');
      setFetchedOrder(null);
      setOrderNumberInput('');
      loadReturns(0);
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to submit return request.';
      toast({ type: 'error', title: 'Submission Denied', message: msg });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header & Mode Switcher */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            3-Day Returns &amp; Exchanges
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Surat craft quality guarantee: return or exchange within 3 calendar days of parcel delivery.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <Button
            variant={activeTab === 'LIST' ? 'primary' : 'outline'}
            size="sm"
            onClick={() => setActiveTab('LIST')}
          >
            My Return Requests
          </Button>
          <Button
            variant={activeTab === 'CREATE' ? 'primary' : 'outline'}
            size="sm"
            onClick={() => setActiveTab('CREATE')}
          >
            + Create New Request
          </Button>
        </div>
      </div>

      {/* Quality Policy Info Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          backgroundColor: 'var(--wine-soft)',
          border: '1px solid var(--wine-line)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '13px',
          color: 'var(--wine-dark)',
        }}
      >
        <ShieldCheck size={20} color="var(--wine)" />
        <div>
          <strong>Strict 3-Day Inspection Policy:</strong> All return requests must be logged within 3 days of delivery with at least 1 clear photo. Returned garments undergo warehouse inspection before exchange or store credit issuance.
        </div>
      </div>

      {activeTab === 'CREATE' ? (
        /* Create Return Request Form */
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '28px',
          }}
        >
          <h2 style={{ margin: '0 0 20px', fontSize: '16px', fontWeight: 600 }}>
            Submit a Quality Return or Exchange
          </h2>

          <form onSubmit={handleSubmitReturn} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Order Lookup Step */}
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Order Number <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. ORD-20261117-0001"
                  value={orderNumberInput}
                  onChange={(e) => setOrderNumberInput(e.target.value)}
                  style={{ flex: 1 }}
                  required
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => lookupOrder(orderNumberInput)}
                  isLoading={fetchingOrder}
                >
                  Verify Order
                </Button>
              </div>
            </div>

            {/* Display Order Items if Verified */}
            {fetchedOrder && (
              <div>
                <label className="form-label" style={{ display: 'block', marginBottom: '8px' }}>
                  Select Item to Return / Exchange <span style={{ color: 'var(--wine)' }}>*</span>
                </label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {fetchedOrder.items.map((it) => (
                    <label
                      key={it.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '12px 16px',
                        border: `1px solid ${selectedItemId === it.id ? 'var(--wine)' : 'var(--line)'}`,
                        borderRadius: 'var(--radius-sm)',
                        backgroundColor: selectedItemId === it.id ? 'var(--wine-soft)' : '#ffffff',
                        cursor: 'pointer',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <input
                          type="radio"
                          name="returnItem"
                          value={it.id}
                          checked={selectedItemId === it.id}
                          onChange={() => setSelectedItemId(it.id)}
                        />
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '13px' }}>{it.productName}</div>
                          <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                            SKU: {it.sku} &bull; Size: {it.size} &bull; Colour: {it.colour} &bull; Qty: {it.quantity}
                          </div>
                        </div>
                      </div>
                      <span style={{ fontWeight: 600, fontSize: '13px', color: 'var(--wine)' }}>
                        ₹{it.itemTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {/* Return Reason Select */}
            <Select
              label="Return Reason"
              options={REASON_OPTIONS}
              value={reason}
              onChange={(e) => setReason(e.target.value as ReturnReason)}
              required
            />

            {/* Resolution Type Select */}
            <Select
              label="Preferred Resolution"
              options={RESOLUTION_OPTIONS}
              value={resolutionType}
              onChange={(e) => setResolutionType(e.target.value as ReturnResolutionType)}
              required
            />

            {/* MANDATORY Photos Input */}
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '4px' }}>
                Garment Inspection Photos <span style={{ color: 'var(--wine)' }}>* (At least 1 required)</span>
              </label>
              <p style={{ margin: '0 0 8px', fontSize: '12px', color: 'var(--ink-muted)' }}>
                Please provide clear URL links showing tags intact and any fabric defects or incorrect measurements.
              </p>
              <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                <input
                  type="url"
                  className="form-input"
                  placeholder="https://example.com/defect-photo.jpg"
                  value={photoInput}
                  onChange={(e) => setPhotoInput(e.target.value)}
                  style={{ flex: 1 }}
                />
                <Button type="button" variant="outline" size="sm" onClick={handleAddPhoto}>
                  <Camera size={14} style={{ marginRight: '6px' }} />
                  Add Photo
                </Button>
              </div>

              {photoUrls.length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px' }}>
                  {photoUrls.map((url, i) => (
                    <div
                      key={i}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        padding: '6px 12px',
                        backgroundColor: 'var(--wine-soft)',
                        border: '1px solid var(--wine-line)',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '12px',
                        color: 'var(--wine)',
                      }}
                    >
                      <Camera size={12} />
                      <span style={{ maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {url}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemovePhoto(i)}
                        style={{ background: 'none', border: 'none', color: 'var(--wine)', cursor: 'pointer', padding: 0 }}
                      >
                        &times;
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Customer Notes */}
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Additional Notes / Defect Description
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="Describe details for Surat warehouse inspectors (e.g., loose zari border on left pallu)..."
                value={customerNotes}
                onChange={(e) => setCustomerNotes(e.target.value)}
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setActiveTab('LIST')} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={submitting}>
                Submit Return Request
              </Button>
            </div>
          </form>
        </div>
      ) : (
        /* List of Returns */
        <div>
          {pageLoading ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <Skeleton height="80px" />
              <Skeleton height="80px" />
            </div>
          ) : !returnsPage || returnsPage.content.length === 0 ? (
            <div
              style={{
                padding: '48px 24px',
                textAlign: 'center',
                backgroundColor: '#ffffff',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <RefreshCw size={32} color="var(--ink-muted)" style={{ marginBottom: '12px' }} />
              <h3 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 600 }}>No return requests</h3>
              <p style={{ margin: '0 0 20px', fontSize: '13px', color: 'var(--ink-muted)' }}>
                You have not submitted any return or exchange requests.
              </p>
              <Button variant="outline" size="sm" onClick={() => setActiveTab('CREATE')}>
                Submit Return Request
              </Button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {returnsPage.content.map((ret) => (
                <div
                  key={ret.id}
                  style={{
                    backgroundColor: '#ffffff',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '20px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '12px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span style={{ fontSize: '14px', fontWeight: 600 }}>
                        Return #{ret.returnNumber}
                      </span>
                      <Badge variant={getReturnStatusVariant(ret.status)}>
                        {ret.status.replace('_', ' ')}
                      </Badge>
                    </div>
                    <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                      Requested on {new Date(ret.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' })}
                    </span>
                  </div>

                  <div style={{ fontSize: '13px', color: 'var(--ink)' }}>
                    <strong>Item:</strong> {ret.productName} (SKU: {ret.sku}) &bull; Qty: {ret.quantity}
                  </div>

                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', fontSize: '12px', color: 'var(--ink-muted)' }}>
                    <span><strong>Reason:</strong> {ret.reason.replace(/_/g, ' ')}</span>
                    <span><strong>Resolution:</strong> {ret.resolutionType.replace(/_/g, ' ')}</span>
                    <span><strong>Order:</strong> #{ret.orderNumber}</span>
                  </div>

                  {ret.photoUrls && ret.photoUrls.length > 0 && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                      <Camera size={14} color="var(--ink-muted)" />
                      <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
                        {ret.photoUrls.length} {ret.photoUrls.length === 1 ? 'photo' : 'photos'} attached
                      </span>
                    </div>
                  )}

                  {ret.customerNotes && (
                    <div style={{ fontSize: '12px', color: 'var(--ink-secondary)', fontStyle: 'italic', backgroundColor: 'var(--surface-subtle)', padding: '8px 12px', borderRadius: 'var(--radius-sm)' }}>
                      &ldquo;{ret.customerNotes}&rdquo;
                    </div>
                  )}
                </div>
              ))}

              {/* Pagination */}
              {returnsPage.totalPages > 1 && (
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
                    Page {currentPage + 1} of {returnsPage.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= returnsPage.totalPages - 1}
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
      )}
    </div>
  );
}

export default function ReturnsPage() {
  return (
    <Suspense fallback={<Skeleton height="300px" />}>
      <ReturnsContent />
    </Suspense>
  );
}
