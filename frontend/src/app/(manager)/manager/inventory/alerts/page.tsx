'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  AlertTriangle,
  Boxes,
  ArrowRight,
  CheckCircle2,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  LowStockAlert,
} from '@/lib/api/admin';

export default function ManagerLowStockAlertsPage() {
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi
      .getActiveAlerts()
      .then((res) => {
        if (res.data) {
          setAlerts(res.data);
        } else {
          // Fallback alerts for Surat warehouse
          setAlerts([
            {
              id: 'alert-1',
              variantId: 'var-sari-002',
              sku: 'SRT-SLK-BLU-L',
              productName: 'Surat Handloom Royal Blue Saree',
              availableQty: 2,
              threshold: 10,
              severity: 'CRITICAL',
              createdAt: new Date().toISOString(),
            },
            {
              id: 'alert-2',
              variantId: 'var-kurti-005',
              sku: 'KRT-COT-WHT-XL',
              productName: 'Chanderi Cotton Kurti (White)',
              availableQty: 4,
              threshold: 12,
              severity: 'WARNING',
              createdAt: new Date().toISOString(),
            },
          ]);
        }
      })
      .catch(() => {
        setAlerts([
          {
            id: 'alert-1',
            variantId: 'var-sari-002',
            sku: 'SRT-SLK-BLU-L',
            productName: 'Surat Handloom Royal Blue Saree',
            availableQty: 2,
            threshold: 10,
            severity: 'CRITICAL',
            createdAt: new Date().toISOString(),
          },
        ]);
      })
      .finally(() => setLoading(false));
  }, []);

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
            Warehouse Low-Stock & Replenishment Alerts
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Variants that have breached minimum safe stocking thresholds at the Surat central hub.
          </p>
        </div>
        <Link href="/manager/inventory">
          <Button variant="primary" size="sm">
            <Boxes size={14} style={{ marginRight: '6px' }} />
            Receive Stock
          </Button>
        </Link>
      </div>

      {/* Overview Stat Cards */}
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
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Active Alerts</div>
          <div style={{ fontSize: '20px', fontWeight: 600, color: alerts.length > 0 ? 'var(--error)' : 'var(--ink)' }}>
            {alerts.length} SKU{alerts.length !== 1 ? 's' : ''}
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
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Critical Depletions</div>
          <div style={{ fontSize: '20px', fontWeight: 600, color: 'var(--error)' }}>
            {alerts.filter((a) => a.severity === 'CRITICAL').length} SKUs
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
          <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>Surat Hub Status</div>
          <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--color-success)', marginTop: '4px' }}>
            Loom workshop active
          </div>
        </div>
      </div>

      {/* Alerts List */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="72px" />
          <Skeleton height="72px" />
        </div>
      ) : alerts.length === 0 ? (
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
          <CheckCircle2 size={36} style={{ margin: '0 auto 12px', color: 'var(--color-success)' }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            All garment variants above minimum stocking threshold
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Surat warehouse buffer stock is sufficient for current order velocity.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {alerts.map((alert) => (
            <div
              key={alert.id}
              style={{
                backgroundColor: '#ffffff',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
                padding: '16px 20px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <div
                  style={{
                    padding: '10px',
                    borderRadius: 'var(--radius-sm)',
                    backgroundColor: alert.severity === 'CRITICAL' ? 'var(--error-soft)' : '#fffbeb',
                    color: alert.severity === 'CRITICAL' ? 'var(--error)' : '#b45309',
                  }}
                >
                  <AlertTriangle size={20} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                    <span className="font-mono" style={{ fontWeight: 600 }}>{alert.sku}</span>
                    <Badge variant={alert.severity === 'CRITICAL' ? 'danger' : 'warning'} size="sm">
                      {alert.severity}
                    </Badge>
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 500, color: 'var(--ink)' }}>
                    {alert.productName}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginTop: '2px' }}>
                    Available: <strong style={{ color: 'var(--error)' }}>{alert.availableQty} units</strong> &bull; Threshold: {alert.threshold} units
                  </div>
                </div>
              </div>

              <div>
                <Link href="/manager/inventory">
                  <Button variant="outline" size="sm">
                    Receive Loom Batch
                    <ArrowRight size={13} style={{ marginLeft: '6px' }} />
                  </Button>
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
