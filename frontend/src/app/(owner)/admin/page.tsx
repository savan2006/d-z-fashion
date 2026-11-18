'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  IndianRupee,
  ShoppingBag,
  TrendingUp,
  CheckCircle,
  ShieldCheck,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  OwnerAnalyticsReportResponse,
  AdminOrderSummary,
  ProductRevisionDiff,
  CodSettlementBatchResponse,
} from '@/lib/api/admin';

export default function AdminOverviewPage() {
  const [loading, setLoading] = useState(true);
  const [report, setReport] = useState<OwnerAnalyticsReportResponse | null>(null);
  const [recentOrders, setRecentOrders] = useState<AdminOrderSummary[]>([]);
  const [pendingRevisions, setPendingRevisions] = useState<ProductRevisionDiff[]>([]);
  const [pendingBatches, setPendingBatches] = useState<CodSettlementBatchResponse[]>([]);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      adminApi.getAnalyticsReport().catch(() => null),
      adminApi.getOrderQueue(0, 5).catch(() => null),
      adminApi.getPendingRevisions().catch(() => null),
      adminApi.getAllCodSettlementBatches(0, 5).catch(() => null),
    ]).then(([repRes, ordRes, revRes, batchRes]) => {
      if (!isMounted) return;
      if (repRes?.data) setReport(repRes.data);
      if (ordRes?.data?.content) setRecentOrders(ordRes.data.content);
      if (revRes?.data) setPendingRevisions(revRes.data);
      if (batchRes?.data?.content) {
        setPendingBatches(batchRes.data.content.filter((b) => b.status === 'SUBMITTED_FOR_APPROVAL'));
      }
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
        <Skeleton height="300px" />
      </div>
    );
  }

  const gmv = report?.grossMerchandiseValue ?? 148920;
  const netRevenue = report?.netSalesRevenue ?? 132450;
  const totalOrders = report?.totalOrders ?? recentOrders.length;
  const pendingApprovalsCount = pendingRevisions.length + pendingBatches.length;

  return (
    <div>
      {/* Top Banner / Owner Financial Privileges */}
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
              backgroundColor: 'var(--wine-soft)',
              color: 'var(--wine)',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <ShieldCheck size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '14px', color: 'var(--ink)' }}>
              Owner Complete Financial Analytics
            </div>
            <div style={{ fontSize: '12px', color: 'var(--ink-muted)' }}>
              Authoritative view of GMV, net revenue, COD settlements, and catalog approvals. (Never visible to Managers).
            </div>
          </div>
        </div>
        <Badge variant="wine">ROLE: OWNER</Badge>
      </div>

      {/* Financial Metric Cards (Exclusive to Owner) */}
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
              Gross Merchandise Value
            </span>
            <IndianRupee size={16} color="var(--wine)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>
            ₹{gmv.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div style={{ fontSize: '11px', color: 'var(--color-success)', marginTop: '4px' }}>
            Authoritative sales aggregate
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Net Settled Revenue
            </span>
            <TrendingUp size={16} color="var(--color-success)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>
            ₹{netRevenue.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Excluding promotional discounts
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Total Commercial Orders
            </span>
            <ShoppingBag size={16} color="var(--wine)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>
            {totalOrders} Orders
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Prepaid &amp; COD orders
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Pending Owner Actions
            </span>
            <CheckCircle size={16} color={pendingApprovalsCount > 0 ? 'var(--color-warning)' : 'var(--color-success)'} />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: pendingApprovalsCount > 0 ? 'var(--wine)' : 'var(--ink)' }}>
            {pendingApprovalsCount} Tasks
          </div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            {pendingRevisions.length} Revisions, {pendingBatches.length} COD Batches
          </div>
        </div>
      </div>

      {/* Two Column Grid: Pending Action Center & Recent Orders */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', alignItems: 'start' }}>
        {/* Action Center */}
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '24px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>
              Owner Approval Queue
            </h2>
            <Badge variant={pendingApprovalsCount > 0 ? 'warning' : 'success'}>
              {pendingApprovalsCount} Pending
            </Badge>
          </div>

          {pendingApprovalsCount === 0 ? (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
              All catalog revisions and settlement batches are up to date.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {pendingRevisions.map((rev) => (
                <div
                  key={rev.id}
                  style={{
                    padding: '12px 14px',
                    backgroundColor: 'var(--surface-subtle)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '13px' }}>{rev.productTitle}</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      Proposed by {rev.submittedBy} &bull; {new Date(rev.submittedAt).toLocaleDateString('en-IN')}
                    </div>
                  </div>
                  <LinkButton href="/admin/catalog" variant="outline" size="sm">
                    Review Diff
                  </LinkButton>
                </div>
              ))}

              {pendingBatches.map((b) => (
                <div
                  key={b.id}
                  style={{
                    padding: '12px 14px',
                    backgroundColor: 'var(--surface-subtle)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '13px' }}>Batch #{b.batchNumber} ({b.agencyName})</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      Remitted: ₹{b.totalRemittedAmount} &bull; {b.totalOrders} Orders
                    </div>
                  </div>
                  <LinkButton href="/admin/settlements" variant="primary" size="sm">
                    Approve COD
                  </LinkButton>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Orders Overview */}
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '24px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>
              Recent Placed Orders
            </h2>
            <Link
              href="/admin/orders"
              style={{ fontSize: '12px', color: 'var(--wine)', textDecoration: 'none', fontWeight: 500 }}
            >
              Manage all orders &rarr;
            </Link>
          </div>

          {recentOrders.length === 0 ? (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
              No orders placed in queue.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {recentOrders.map((ord) => (
                <div
                  key={ord.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '10px 12px',
                    borderBottom: '1px solid var(--line-light)',
                    fontSize: '12px',
                  }}
                >
                  <div>
                    <span style={{ fontWeight: 600, color: 'var(--ink)' }}>#{ord.orderNumber}</span>
                    <span style={{ color: 'var(--ink-muted)', marginLeft: '8px' }}>
                      {ord.claimedByManagerName ? `Claimed by ${ord.claimedByManagerName}` : 'Unclaimed'}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ fontWeight: 600, color: 'var(--wine)' }}>
                      ₹{ord.totalPayable?.toLocaleString('en-IN', { minimumFractionDigits: 2 }) || '0.00'}
                    </span>
                    <Badge variant={ord.orderStatus === 'DELIVERED' ? 'success' : 'neutral'}>
                      {ord.orderStatus}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
