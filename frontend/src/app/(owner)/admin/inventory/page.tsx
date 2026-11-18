'use client';

import React, { useEffect, useState } from 'react';
import {
  adminApi,
  StockItemResponse,
  LowStockAlert,
  PageResponse,
} from '@/lib/api/admin';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { AlertTriangle } from 'lucide-react';

export default function AdminInventoryPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [lowStockPage, setLowStockPage] = useState<PageResponse<StockItemResponse> | null>(null);
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);

  // Stock Adjustment Modal
  const [adjustTarget, setAdjustTarget] = useState<StockItemResponse | null>(null);
  const [quantityDelta, setQuantityDelta] = useState('0');
  const [movementType, setMovementType] = useState<'MANUAL_COUNT' | 'DAMAGE_WRITEOFF' | 'WORKSHOP_RECEIPT'>('MANUAL_COUNT');
  const [reason, setReason] = useState('');
  const [adjusting, setAdjusting] = useState(false);

  // Threshold update modal
  const [thresholdTarget, setThresholdTarget] = useState<StockItemResponse | null>(null);
  const [newThreshold, setNewThreshold] = useState('5');
  const [updatingThreshold, setUpdatingThreshold] = useState(false);

  const loadData = () => {
    let isMounted = true;
    Promise.all([
      adminApi.getLowStockQueue(0, 20).catch(() => null),
      adminApi.getActiveAlerts().catch(() => null),
    ]).then(([stockRes, alertRes]) => {
      if (!isMounted) return;
      if (stockRes?.data) setLowStockPage(stockRes.data);
      if (alertRes?.data) setAlerts(alertRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAdjustStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!adjustTarget) return;
    const delta = parseInt(quantityDelta, 10);
    if (isNaN(delta) || delta === 0) {
      toast({ type: 'warning', title: 'Invalid Delta', message: 'Enter a positive or negative quantity change.' });
      return;
    }
    if (!reason.trim()) {
      toast({ type: 'warning', title: 'Reason Required', message: 'Please provide a clear reason for inventory adjustment.' });
      return;
    }

    setAdjusting(true);
    try {
      await adminApi.adjustStock({
        variantId: adjustTarget.variantId,
        warehouseId: '00000000-0000-0000-0000-000000000001',
        quantityDelta: delta,
        movementType,
        reason: reason.trim(),
      });
      toast({
        type: 'success',
        title: 'Stock Adjusted',
        message: `Inventory for SKU ${adjustTarget.sku} adjusted by ${delta > 0 ? `+${delta}` : delta}.`,
      });
      setAdjustTarget(null);
      setQuantityDelta('0');
      setReason('');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Adjustment failed';
      toast({ type: 'error', title: 'Error', message: msg });
    } finally {
      setAdjusting(false);
    }
  };

  const handleUpdateThreshold = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!thresholdTarget) return;
    const t = parseInt(newThreshold, 10);
    if (isNaN(t) || t < 0) return;

    setUpdatingThreshold(true);
    try {
      await adminApi.updateThreshold(thresholdTarget.variantId, t);
      toast({ type: 'success', title: 'Threshold Updated', message: `Low stock alert threshold set to ${t} units.` });
      setThresholdTarget(null);
      loadData();
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Failed to update threshold.' });
    } finally {
      setUpdatingThreshold(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Inventory &amp; Warehouse Management
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Surat main warehouse live physical, reserved, available, and quarantined return stocks.
        </p>
      </div>

      {/* Low Stock Alerts Banner */}
      {alerts.length > 0 && (
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
              {alerts.length} Low-Stock Alert{alerts.length > 1 ? 's' : ''} in Surat Ready Stock
            </span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {alerts.map((a) => (
              <span
                key={a.id}
                style={{
                  fontSize: '12px',
                  backgroundColor: '#ffffff',
                  border: '1px solid #fde68a',
                  padding: '4px 10px',
                  borderRadius: '12px',
                  color: '#78350f',
                }}
              >
                <strong>{a.sku}:</strong> {a.availableQty} units left (Threshold: {a.threshold})
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Inventory Stock Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Surat Warehouse Variant Stock</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !lowStockPage || lowStockPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No stock entries found.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>SKU</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Product &amp; Variant</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Physical Qty</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Reserved</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Available</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Quarantined</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Threshold</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {lowStockPage.content.map((s) => (
                <tr key={s.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                    {s.sku}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <div style={{ fontWeight: 500 }}>{s.productName}</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      Size: {s.size} &bull; Colour: {s.colour}
                    </div>
                  </td>
                  <td style={{ padding: '14px 16px', fontWeight: 600 }}>{s.physicalQty}</td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>{s.reservedQty}</td>
                  <td style={{ padding: '14px 16px' }}>
                    <span
                      style={{
                        fontWeight: 600,
                        color: s.availableQty <= s.lowStockThreshold ? 'var(--color-warning)' : 'var(--color-success)',
                      }}
                    >
                      {s.availableQty}
                    </span>
                  </td>
                  <td style={{ padding: '14px 16px', color: s.quarantinedQty > 0 ? 'var(--wine)' : 'var(--ink-muted)' }}>
                    {s.quarantinedQty}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <button
                      type="button"
                      onClick={() => {
                        setThresholdTarget(s);
                        setNewThreshold(String(s.lowStockThreshold));
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--wine)',
                        cursor: 'pointer',
                        padding: 0,
                        fontSize: '12px',
                        textDecoration: 'underline',
                      }}
                    >
                      {s.lowStockThreshold} units
                    </button>
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <Button variant="outline" size="sm" onClick={() => setAdjustTarget(s)}>
                      Adjust
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Adjust Stock Modal */}
      {adjustTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setAdjustTarget(null)}
          title={`Adjust Inventory: ${adjustTarget.sku}`}
          description={`Current Available: ${adjustTarget.availableQty} units. Mandatory audit reason required.`}
        >
          <form onSubmit={handleAdjustStock} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <Select
              label="Adjustment Movement Type"
              value={movementType}
              onChange={(e) => setMovementType(e.target.value as 'MANUAL_COUNT' | 'DAMAGE_WRITEOFF' | 'WORKSHOP_RECEIPT')}
              options={[
                { value: 'MANUAL_COUNT', label: 'Physical Count Reconciliation' },
                { value: 'DAMAGE_WRITEOFF', label: 'Damaged / Defective Write-Off' },
                { value: 'WORKSHOP_RECEIPT', label: 'Direct Workshop Batch Receipt' },
              ]}
              required
            />

            <TextField
              label="Quantity Delta (+ to add, - to deduct)"
              type="number"
              value={quantityDelta}
              onChange={(e) => setQuantityDelta(e.target.value)}
              required
            />

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Audit Adjustment Reason <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. Surat loom shipment batch #401 arrived..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setAdjustTarget(null)} disabled={adjusting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={adjusting}>
                Commit Adjustment
              </Button>
            </div>
          </form>
        </Dialog>
      )}

      {/* Update Threshold Modal */}
      {thresholdTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setThresholdTarget(null)}
          title={`Set Low-Stock Alert Threshold: ${thresholdTarget.sku}`}
          description="Alerts will trigger in Manager and Owner dashboards when ready stock drops below this number."
        >
          <form onSubmit={handleUpdateThreshold} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="Low-Stock Threshold (Units)"
              type="number"
              value={newThreshold}
              onChange={(e) => setNewThreshold(e.target.value)}
              required
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setThresholdTarget(null)} disabled={updatingThreshold}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={updatingThreshold}>
                Save Threshold
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
