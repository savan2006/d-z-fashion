'use client';

import React, { useEffect, useState, useTransition } from 'react';
import {
  Boxes,
  Plus,
  Search,
  CheckCircle2,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  StockItemResponse,
} from '@/lib/api/admin';

export default function ManagerInventoryPage() {
  const [stockItems, setStockItems] = useState<StockItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterLowStockOnly, setFilterLowStockOnly] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Loom Batch Receiving Modal
  const [isReceivingLoomBatch, setIsReceivingLoomBatch] = useState(false);
  const [selectedVariantId, setSelectedVariantId] = useState('');
  const [loomBatchNumber, setLoomBatchNumber] = useState('');
  const [receivedQty, setReceivedQty] = useState(50);
  const [warehouseId] = useState('surat-central-hub');
  const [isSubmittingStock, startStockTransition] = useTransition();

  const loadInventory = () => {
    adminApi
      .getLowStockQueue(0, 50)
      .then((stockRes) => {
        if (stockRes?.data?.content) {
          setStockItems(stockRes.data.content);
        } else {
          // Fallback mock items for Surat operations
          setStockItems([
            {
              id: 'stock-101',
              variantId: 'var-sari-001',
              sku: 'SRT-SLK-RED-M',
              productName: 'Surat Pure Silk Zari Saree',
              size: 'M',
              colour: 'Crimson Red',
              physicalQty: 45,
              reservedQty: 12,
              availableQty: 33,
              quarantinedQty: 0,
              lowStockThreshold: 15,
              isLowStock: false,
              warehouseName: 'Surat Central Hub',
              updatedAt: new Date().toISOString(),
            },
            {
              id: 'stock-102',
              variantId: 'var-sari-002',
              sku: 'SRT-SLK-BLU-L',
              productName: 'Surat Handloom Royal Blue Saree',
              size: 'L',
              colour: 'Royal Blue',
              physicalQty: 6,
              reservedQty: 4,
              availableQty: 2,
              quarantinedQty: 1,
              lowStockThreshold: 10,
              isLowStock: true,
              warehouseName: 'Surat Central Hub',
              updatedAt: new Date().toISOString(),
            },
            {
              id: 'stock-103',
              variantId: 'var-kurti-003',
              sku: 'KRT-COT-YEL-S',
              productName: 'Chanderi Cotton Kurti',
              size: 'S',
              colour: 'Marigold Yellow',
              physicalQty: 18,
              reservedQty: 3,
              availableQty: 15,
              quarantinedQty: 0,
              lowStockThreshold: 12,
              isLowStock: false,
              warehouseName: 'Surat Central Hub',
              updatedAt: new Date().toISOString(),
            },
          ]);
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadInventory();
  }, []);

  const handleLoomBatchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedVariantId || !loomBatchNumber.trim() || receivedQty <= 0) return;

    startStockTransition(async () => {
      try {
        await adminApi.addManufacturedStock({
          variantId: selectedVariantId,
          warehouseId,
          quantity: Number(receivedQty),
          batchNumber: loomBatchNumber.trim(),
        });

        setFeedback({
          type: 'success',
          text: `Successfully received ${receivedQty} units from Surat Loom Batch #${loomBatchNumber}. Inventory updated.`,
        });

        setIsReceivingLoomBatch(false);
        setLoomBatchNumber('');
        setReceivedQty(50);
        loadInventory();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record loom batch stock receipt.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredItems = stockItems.filter((item) => {
    const matchesSearch =
      item.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.colour.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    if (filterLowStockOnly) return item.isLowStock;
    return true;
  });

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
            Surat Stock Receiving & Inventory
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Physical warehouse balances, loom workshop intake receipts, and quarantine reservations.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsReceivingLoomBatch(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Receive Loom Batch
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

      {/* Filter and Search Bar */}
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
        <div style={{ position: 'relative', minWidth: '260px', flex: 1, maxWidth: '360px' }}>
          <Search
            size={14}
            style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
          />
          <input
            type="text"
            placeholder="Search SKU, product, or colour..."
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

        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={filterLowStockOnly}
            onChange={(e) => setFilterLowStockOnly(e.target.checked)}
          />
          <span style={{ fontWeight: 500, color: filterLowStockOnly ? 'var(--error)' : 'var(--ink)' }}>
            Show Low-Stock Critical Only
          </span>
        </label>
      </div>

      {/* Inventory Table */}
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
          <Boxes size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No inventory items found
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Adjust your search query or receive manufactured stock to view rack quantities.
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
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>SKU</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment Name</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Size / Colour</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Physical Qty</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Reserved</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Available</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.id} style={{ borderBottom: '1px solid var(--line)' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    <span className="font-mono">{item.sku}</span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>{item.productName}</td>
                  <td style={{ padding: '12px 16px', color: 'var(--ink-muted)' }}>
                    {item.size} &bull; {item.colour}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>{item.physicalQty} units</td>
                  <td style={{ padding: '12px 16px', color: 'var(--ink-muted)' }}>{item.reservedQty} units</td>
                  <td style={{ padding: '12px 16px', fontWeight: 700, color: item.availableQty <= item.lowStockThreshold ? 'var(--error)' : 'var(--color-success)' }}>
                    {item.availableQty} units
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {item.isLowStock || item.availableQty <= item.lowStockThreshold ? (
                      <Badge variant="danger" size="sm">
                        Low Stock (&le; {item.lowStockThreshold})
                      </Badge>
                    ) : (
                      <Badge variant="success" size="sm">
                        Healthy
                      </Badge>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Loom Batch Receiving Modal */}
      {isReceivingLoomBatch && (
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
                Receive Surat Loom Workshop Batch
              </h2>
              <button
                onClick={() => setIsReceivingLoomBatch(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Record finished handloom garments arriving from master weavers in Surat workshop into central inventory.
            </p>

            <form onSubmit={handleLoomBatchSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Target Variant / Garment *
                </label>
                <select
                  required
                  value={selectedVariantId}
                  onChange={(e) => setSelectedVariantId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: '#ffffff',
                  }}
                >
                  <option value="">Select garment variant...</option>
                  {stockItems.map((s) => (
                    <option key={s.variantId} value={s.variantId}>
                      {s.sku} — {s.productName} ({s.size}/{s.colour})
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Loom Batch Reference Number *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. LOOM-SURAT-2026-B8"
                  value={loomBatchNumber}
                  onChange={(e) => setLoomBatchNumber(e.target.value.toUpperCase())}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Units Received (Count) *
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  value={receivedQty}
                  onChange={(e) => setReceivedQty(Number(e.target.value))}
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
                  Destination Warehouse
                </label>
                <input
                  type="text"
                  disabled
                  value="Surat Central Hub (Ring Road Textile Market)"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: 'var(--paper-warm)',
                    color: 'var(--ink-muted)',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setIsReceivingLoomBatch(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingStock}>
                  Confirm Stock Receipt
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
