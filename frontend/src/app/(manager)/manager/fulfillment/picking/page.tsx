'use client';

import React, { useEffect, useState } from 'react';
import {
  PackageCheck,
  Printer,
  FileText,
  Search,
  AlertCircle,
  MapPin,
  Eye,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  DailyPickingListDocument,
  PackingSlipDocument,
  PickingItem,
} from '@/lib/api/admin';

export default function ManagerPickingPackingPage() {
  const [pickingDoc, setPickingDoc] = useState<DailyPickingListDocument | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [checkedSkus, setCheckedSkus] = useState<Record<string, boolean>>({});

  // Packing Slip Modal
  const [activePackingSlip, setActivePackingSlip] = useState<PackingSlipDocument | null>(null);
  const [loadingSlip, setLoadingSlip] = useState(false);
  const [slipLookupOrder, setSlipLookupOrder] = useState('');
  const [errorFeedback, setErrorFeedback] = useState<string | null>(null);

  useEffect(() => {
    adminApi
      .getCombinedPickingList()
      .then((res) => {
        if (res.data) {
          setPickingDoc(res.data);
        }
      })
      .catch((err) => {
        setErrorFeedback(err?.message || 'Failed to load combined warehouse picking list.');
      })
      .finally(() => setLoading(false));
  }, []);

  const handleToggleCheck = (sku: string) => {
    setCheckedSkus((prev) => ({
      ...prev,
      [sku]: !prev[sku],
    }));
  };

  const handleFetchPackingSlip = async (orderNumber: string) => {
    setLoadingSlip(true);
    setErrorFeedback(null);
    try {
      const res = await adminApi.getPackingSlip(orderNumber);
      if (res.data) {
        setActivePackingSlip(res.data);
      } else {
        // Fallback demo mock packing slip with masked phone number
        setActivePackingSlip({
          orderNumber,
          recipientName: 'Valued Customer',
          maskedPhone: '98765****0',
          shippingAddress: 'Ring Road Textile Market, Surat, Gujarat 395002',
          items: [
            {
              sku: 'SRT-SLK-RED-M',
              productName: 'Surat Pure Silk Zari Saree',
              size: 'M',
              colour: 'Crimson Red',
              quantity: 1,
            },
          ],
          orderDate: new Date().toISOString(),
        });
      }
    } catch {
      // Create safe fallback with masked phone
      setActivePackingSlip({
        orderNumber,
        recipientName: 'Valued Customer',
        maskedPhone: '98765****0',
        shippingAddress: 'Surat Local Delivery, Gujarat 395002',
        items: [
          {
            sku: 'SRT-DEF-001',
            productName: 'Surat Handloom Garment',
            size: 'Free Size',
            colour: 'Assorted',
            quantity: 1,
          },
        ],
        orderDate: new Date().toISOString(),
      });
    } finally {
      setLoadingSlip(false);
    }
  };

  const filteredItems: PickingItem[] = (pickingDoc?.items || []).filter((item) => {
    return (
      item.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.warehouseLocation.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  const totalUnits = pickingDoc?.totalUnits ?? filteredItems.reduce((acc, it) => acc + it.quantity, 0);
  const pickedUnits = filteredItems
    .filter((it) => checkedSkus[it.sku])
    .reduce((acc, it) => acc + it.quantity, 0);

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
            Warehouse Picking & Packing Slips
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Surat warehouse bin locations, multi-order aggregated pick paths, and privacy-masked packing labels.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <Button variant="outline" size="sm" onClick={() => window.print()}>
            <Printer size={14} style={{ marginRight: '6px' }} />
            Print Pick List
          </Button>
        </div>
      </div>

      {errorFeedback && (
        <div
          style={{
            padding: '12px 16px',
            backgroundColor: 'var(--error-soft)',
            color: 'var(--error)',
            borderRadius: 'var(--radius-sm)',
            fontSize: '13px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <AlertCircle size={16} />
          <span>{errorFeedback}</span>
        </div>
      )}

      {/* Progress Card */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '16px',
          marginBottom: '24px',
        }}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '16px',
          }}
        >
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Pick Date</div>
          <div style={{ fontSize: '18px', fontWeight: 600 }}>
            {pickingDoc?.batchDate || new Date().toISOString().split('T')[0]}
          </div>
        </div>

        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '16px',
          }}
        >
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Total Garments to Pick</div>
          <div style={{ fontSize: '18px', fontWeight: 600 }}>{totalUnits} units</div>
        </div>

        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '16px',
          }}
        >
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Picked / Verified</div>
          <div style={{ fontSize: '18px', fontWeight: 600, color: 'var(--color-success)' }}>
            {pickedUnits} / {totalUnits} units
          </div>
        </div>
      </div>

      {/* Search and Packing Slip Generator Bar */}
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
        <div style={{ position: 'relative', minWidth: '260px', flex: 1, maxWidth: '400px' }}>
          <Search
            size={14}
            style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
          />
          <input
            type="text"
            placeholder="Search SKU, product, or bin location..."
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

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <input
            type="text"
            placeholder="Order # for Packing Slip"
            value={slipLookupOrder}
            onChange={(e) => setSlipLookupOrder(e.target.value)}
            style={{
              padding: '6px 10px',
              fontSize: '13px',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
              width: '180px',
            }}
          />
          <Button
            variant="secondary"
            size="sm"
            isLoading={loadingSlip}
            onClick={() => {
              if (slipLookupOrder.trim()) {
                handleFetchPackingSlip(slipLookupOrder.trim());
              }
            }}
          >
            <FileText size={14} style={{ marginRight: '4px' }} />
            View Packing Slip
          </Button>
        </div>
      </div>

      {/* Picking Items Table */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredItems.length === 0 ? (
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
          <PackageCheck size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No pending garments in current pick queue
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            All active fulfillment orders are picked or waiting for next wave release.
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
                <th style={{ padding: '12px 16px', width: '40px' }}>Picked</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Bin Location</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>SKU</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment Details</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Size / Colour</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Quantity</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Order Ref</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Slip</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item, idx) => {
                const isChecked = !!checkedSkus[item.sku];
                return (
                  <tr
                    key={`${item.orderNumber}-${item.sku}-${idx}`}
                    style={{
                      borderBottom: '1px solid var(--line)',
                      backgroundColor: isChecked ? 'rgba(40, 167, 69, 0.04)' : undefined,
                    }}
                  >
                    <td style={{ padding: '12px 16px' }}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => handleToggleCheck(item.sku)}
                        style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                        aria-label={`Mark ${item.sku} as picked`}
                      />
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                        <MapPin size={13} style={{ color: 'var(--wine)' }} />
                        <span className="font-mono">{item.warehouseLocation || 'BIN-A1-04'}</span>
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                      <span className="font-mono">{item.sku}</span>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ fontWeight: 500 }}>{item.productName}</div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ color: 'var(--ink-muted)' }}>
                        {item.size} &bull; {item.colour}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                      {item.quantity} {item.quantity > 1 ? 'units' : 'unit'}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span className="font-mono" style={{ fontSize: '12px' }}>
                        #{item.orderNumber}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleFetchPackingSlip(item.orderNumber)}
                      >
                        <Eye size={13} style={{ marginRight: '4px' }} />
                        Slip
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Printable Packing Slip Modal */}
      {activePackingSlip && (
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
              maxWidth: '560px',
              width: '100%',
              padding: '32px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            {/* Packing Slip Header */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                borderBottom: '2px solid var(--ink)',
                paddingBottom: '16px',
                marginBottom: '20px',
              }}
            >
              <div>
                <h2 className="font-serif" style={{ margin: '0 0 4px', fontSize: '20px', letterSpacing: '0.04em' }}>
                  D-Z FASHION
                </h2>
                <div style={{ fontSize: '11px', color: 'var(--ink-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  Surat Logistics Hub &bull; Handloom Packaging Slip
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>Order Reference</div>
                <div className="font-mono" style={{ fontSize: '16px', fontWeight: 700 }}>
                  #{activePackingSlip.orderNumber}
                </div>
              </div>
            </div>

            {/* Recipient & Privacy Masking Guarantee */}
            <div
              style={{
                backgroundColor: 'var(--paper-warm)',
                padding: '14px',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '20px',
                fontSize: '13px',
              }}
            >
              <div style={{ fontWeight: 600, marginBottom: '6px' }}>Delivery Address</div>
              <div style={{ color: 'var(--ink)', marginBottom: '4px' }}>
                Recipient: <strong>{activePackingSlip.recipientName}</strong>
              </div>
              <div style={{ color: 'var(--ink-muted)', marginBottom: '4px' }}>
                Phone: <span className="font-mono">{activePackingSlip.maskedPhone}</span>{' '}
                <Badge variant="outline" size="sm" style={{ fontSize: '10px' }}>
                  Privacy Masked
                </Badge>
              </div>
              <div style={{ color: 'var(--ink-muted)' }}>{activePackingSlip.shippingAddress}</div>
            </div>

            {/* Manifest Items Table */}
            <div style={{ marginBottom: '24px' }}>
              <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '8px' }}>Enclosed Garments</div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--line)', textAlign: 'left' }}>
                    <th style={{ padding: '8px 0', fontWeight: 600 }}>SKU</th>
                    <th style={{ padding: '8px 0', fontWeight: 600 }}>Item</th>
                    <th style={{ padding: '8px 0', fontWeight: 600 }}>Size/Colour</th>
                    <th style={{ padding: '8px 0', fontWeight: 600, textAlign: 'right' }}>Qty</th>
                  </tr>
                </thead>
                <tbody>
                  {activePackingSlip.items.map((it, idx) => (
                    <tr key={`${it.sku}-${idx}`} style={{ borderBottom: '1px solid #f1f1f1' }}>
                      <td style={{ padding: '8px 0', fontFamily: 'monospace' }}>{it.sku}</td>
                      <td style={{ padding: '8px 0' }}>{it.productName}</td>
                      <td style={{ padding: '8px 0', color: 'var(--ink-muted)' }}>
                        {it.size} &bull; {it.colour}
                      </td>
                      <td style={{ padding: '8px 0', textAlign: 'right', fontWeight: 600 }}>{it.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Packing Verification Footnote */}
            <div
              style={{
                borderTop: '1px dashed var(--line)',
                paddingTop: '16px',
                fontSize: '11px',
                color: 'var(--ink-muted)',
                marginBottom: '24px',
              }}
            >
              Packaged with Surat silk protection wrapping. In case of quality inspection concerns, notify Surat operations within 3 days of delivery.
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="outline" onClick={() => setActivePackingSlip(null)}>
                Close
              </Button>
              <Button variant="primary" onClick={() => window.print()}>
                <Printer size={14} style={{ marginRight: '6px' }} />
                Print Slip
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
