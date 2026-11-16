import React from 'react';
import Link from 'next/link';
import {
  IndianRupee,
  ShoppingBag,
  TrendingUp,
  Boxes,
  CheckCircle,
  ShieldCheck,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/Table';

export default function AdminOverviewPage() {
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
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>₹1,48,920</div>
          <div style={{ fontSize: '11px', color: 'var(--color-success)', marginTop: '4px' }}>
            +18.4% vs previous 30 days
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Net Settled Revenue
            </span>
            <TrendingUp size={16} color="var(--color-success)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>₹1,32,450</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Excluding tax &amp; refunds
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Pending Approvals
            </span>
            <CheckCircle size={16} color="var(--color-warning)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--wine)' }}>4 Tasks</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            2 Drafts, 1 COD batch, 1 Refund
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Total Order Volume
            </span>
            <ShoppingBag size={16} color="var(--stone)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>86 Orders</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Pan-India external logistics
          </div>
        </div>
      </div>

      {/* Pending Approvals Queue */}
      <div style={{ backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '20px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h3 className="font-serif" style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>
              Owner Gatekeeper &bull; Actions Requiring Decision
            </h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>
              Mandatory approval checkpoints before customer visibility or fund release.
            </p>
          </div>
          <Badge variant="warning">4 PENDING</Badge>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Type</TableHead>
              <TableHead>Reference / Item</TableHead>
              <TableHead>Submitted By</TableHead>
              <TableHead>Details</TableHead>
              <TableHead>Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell>
                <Badge variant="wine">CATALOG DRAFT</Badge>
              </TableCell>
              <TableCell style={{ fontWeight: 600 }}>Surat Silk Anarkali Kurti</TableCell>
              <TableCell>suratmanager</TableCell>
              <TableCell>Draft with 4 size variants, price set to ₹1,899</TableCell>
              <TableCell>
                <Button size="sm" variant="primary">Review &amp; Approve</Button>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>
                <Badge variant="warning">COD SETTLEMENT</Badge>
              </TableCell>
              <TableCell style={{ fontWeight: 600 }}>BATCH-20261116-DEL</TableCell>
              <TableCell>Delhivery Agency</TableCell>
              <TableCell>₹14,250 collected across 8 parcels. Discrepancy: ₹0</TableCell>
              <TableCell>
                <Button size="sm" variant="outline">Approve Settlement</Button>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>
                <Badge variant="error">REFUND REQUEST</Badge>
              </TableCell>
              <TableCell style={{ fontWeight: 600 }}>RET-20261116-0001</TableCell>
              <TableCell>Priya Patel</TableCell>
              <TableCell>Quality inspection passed in Surat. ₹1,499 Store Credit</TableCell>
              <TableCell>
                <Button size="sm" variant="outline">Authorize 7-Day Refund</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>

      {/* Quick Navigation Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
        <Link
          href="/admin/audit"
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'block',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <ShieldCheck size={18} color="var(--wine)" />
            <h4 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Immutable Audit Log</h4>
          </div>
          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.5 }}>
            Append-only system audit trail with sensitive credential/token scrubbing. Owner-only query access.
          </p>
        </Link>

        <Link
          href="/design-system"
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'block',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Boxes size={18} color="var(--wine)" />
            <h4 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Design System Documentation</h4>
          </div>
          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.5 }}>
            Explore tokens, typography, button variants, dialogs, forms, toasts, and responsive shells.
          </p>
        </Link>
      </div>
    </div>
  );
}
