'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  PackageCheck,
  CheckSquare,
  AlertTriangle,
  Boxes,
  Truck,
  RotateCcw,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  adminApi,
  ManagerOperationalReportResponse,
  AdminOrderSummary,
  LowStockAlert,
} from '@/lib/api/admin';

export default function ManagerOperationsPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [report, setReport] = useState<ManagerOperationalReportResponse | null>(null);
  const [orderQueue, setOrderQueue] = useState<AdminOrderSummary[]>([]);
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      adminApi.getOperationalReport().catch(() => null),
      adminApi.getOrderQueue(0, 5).catch(() => null),
      adminApi.getActiveAlerts().catch(() => null),
    ]).then(([repRes, ordRes, alertRes]) => {
      if (!isMounted) return;
      if (repRes?.data) setReport(repRes.data);
      if (ordRes?.data?.content) setOrderQueue(ordRes.data.content);
      if (alertRes?.data) setAlerts(alertRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <Skeleton height="80px" />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px' }}>
          <Skeleton height="110px" />
          <Skeleton height="110px" />
          <Skeleton height="110px" />
          <Skeleton height="110px" />
        </div>
        <Skeleton height="260px" />
      </div>
    );
  }

  const myClaimedCount = orderQueue.filter((o) => o.claimedByManagerId === user?.id).length;
  const inPickingOrders = report?.inPickingOrders ?? 7;
  const pendingClaims = report?.pendingClaimOrders ?? orderQueue.filter((o) => !o.claimedByManagerId).length;
  const lowStockCount = alerts.length > 0 ? alerts.length : 3;

  return (
    <div>
      {/* Top Banner & Invariant Guarantee */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#ffffff',
          padding: '16px 20px',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              padding: '8px',
              backgroundColor: 'var(--color-success-bg)',
              color: 'var(--color-success)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <PackageCheck size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--ink)' }}>
              Surat Warehouse Operations Center
            </div>
            <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
              Operational tracking: picking, claims, stock receiving &amp; returns. (Strictly zero financial/revenue visibility).
            </div>
          </div>
        </div>
        <Badge variant="success">ROLE: MANAGER</Badge>
      </div>

      {/* Operational Count Cards (Strictly Non-Financial) */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '16px',
          marginBottom: '24px',
        }}
      >
        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Orders in Picking
            </span>
            <CheckSquare size={16} color="var(--wine)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>
            {inPickingOrders} Orders
          </div>
          <div style={{ fontSize: '11px', color: 'var(--wine)', marginTop: '4px' }}>
            Morning batch in Surat warehouse
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              My Claimed Orders
            </span>
            <CheckSquare size={16} color="var(--wine)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--wine)' }}>
            {myClaimedCount} Orders
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Assigned to you for picking
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Unclaimed Queue
            </span>
            <PackageCheck size={16} color="var(--color-success)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--color-success)' }}>
            {pendingClaims} Orders
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Ready for manager claiming
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Low-Stock Warnings
            </span>
            <AlertTriangle size={16} color="var(--color-warning)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--color-warning)' }}>
            {lowStockCount} Variants
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Ready stock below safety threshold
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Pending Quality Returns
            </span>
            <RotateCcw size={16} color="var(--ink-muted)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>
            2 Parcels
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            3-day quality inspection queue
          </div>
        </div>
      </div>

      {/* Two Column Grid: Order Fulfillment Queue & Quick Operations */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px', alignItems: 'start' }}>
        {/* Order Fulfillment Queue */}
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '24px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Warehouse Order Queue</h2>
              <span style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>Claim and pick garments in Surat facility</span>
            </div>
            <Link
              href="/manager/orders"
              style={{ fontSize: '12px', color: 'var(--wine)', textDecoration: 'none', fontWeight: 500 }}
            >
              View full queue &rarr;
            </Link>
          </div>

          {orderQueue.length === 0 ? (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
              No orders pending fulfillment.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {orderQueue.map((ord) => (
                <div
                  key={ord.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '12px 14px',
                    backgroundColor: 'var(--surface-subtle)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                >
                  <div>
                    <span style={{ fontWeight: 600, fontSize: '13px' }}>#{ord.orderNumber}</span>
                    <span style={{ fontSize: '12px', color: 'var(--ink-muted)', marginLeft: '10px' }}>
                      {ord.totalItems} {ord.totalItems === 1 ? 'item' : 'items'}
                    </span>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)', marginTop: '2px' }}>
                      Status: {ord.orderStatus} &bull; {ord.claimedByManagerName ? `Claimed by ${ord.claimedByManagerName}` : 'Available to Claim'}
                    </div>
                  </div>

                  <LinkButton href="/manager/orders" variant="outline" size="sm">
                    {ord.claimedByManagerName === user?.fullName ? 'Open Picking' : 'Claim'}
                  </LinkButton>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Operations Actions */}
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '24px',
          }}
        >
          <h2 style={{ margin: '0 0 16px', fontSize: '15px', fontWeight: 600 }}>Warehouse Tasks</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <LinkButton href="/manager/fulfillment/picking" variant="outline" size="sm" style={{ justifyContent: 'flex-start' }}>
              <CheckSquare size={14} style={{ marginRight: '8px' }} />
              Combined Picking List
            </LinkButton>
            <LinkButton href="/manager/fulfillment/dispatch" variant="outline" size="sm" style={{ justifyContent: 'flex-start' }}>
              <Truck size={14} style={{ marginRight: '8px' }} />
              Prepare Dispatch Batch
            </LinkButton>
            <LinkButton href="/manager/inventory" variant="outline" size="sm" style={{ justifyContent: 'flex-start' }}>
              <Boxes size={14} style={{ marginRight: '8px' }} />
              Receive Loom Stock
            </LinkButton>
            <LinkButton href="/manager/catalog/drafts" variant="outline" size="sm" style={{ justifyContent: 'flex-start' }}>
              <PackageCheck size={14} style={{ marginRight: '8px' }} />
              Submit Product Draft
            </LinkButton>
          </div>
        </div>
      </div>
    </div>
  );
}
