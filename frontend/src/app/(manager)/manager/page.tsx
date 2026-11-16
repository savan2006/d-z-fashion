import React from 'react';
import Link from 'next/link';
import {
  PackageCheck,
  CheckSquare,
  AlertTriangle,
  RotateCcw,
  Boxes,
  Truck,
  FileSpreadsheet,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/Table';

export default function ManagerOperationsPage() {
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
              Operational tracking: picking, claims, stock receiving &amp; returns. (Strictly no financial/revenue visibility).
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
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>7 Orders</div>
          <div style={{ fontSize: '11px', color: 'var(--wine)', marginTop: '4px' }}>
            Morning batch in Surat warehouse
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              My Claimed Orders
            </span>
            <PackageCheck size={16} color="var(--color-success)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--color-success)' }}>4 Orders</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Currently claimed by you
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Low-Stock Warnings
            </span>
            <AlertTriangle size={16} color="var(--color-warning)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--color-warning)' }}>3 Variants</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Below Surat warehouse threshold
          </div>
        </div>

        <div style={{ backgroundColor: '#ffffff', padding: '20px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase' }}>
              Pending Return Inspections
            </span>
            <RotateCcw size={16} color="var(--stone)" />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 600, color: 'var(--ink)' }}>2 Parcels</div>
          <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', marginTop: '4px' }}>
            Awaiting Surat physical check
          </div>
        </div>
      </div>

      {/* Operational Actions Queue */}
      <div style={{ backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', padding: '20px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h3 className="font-serif" style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>
              Active Picking &amp; Packing Queue
            </h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-subtle)' }}>
              Claim and prepare physical parcels with packing slips and dispatch labels.
            </p>
          </div>
          <Button size="sm" variant="secondary" startIcon={<FileSpreadsheet size={14} />}>
            Download Picking Sheet
          </Button>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Order ID</TableHead>
              <TableHead>Customer / City</TableHead>
              <TableHead>Units</TableHead>
              <TableHead>Payment Mode</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableCell style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>ORD-20261116-0042</TableCell>
              <TableCell>Priya P. (Ahmedabad, 380001)</TableCell>
              <TableCell>2 items</TableCell>
              <TableCell>
                <Badge variant="wine">COD</Badge>
              </TableCell>
              <TableCell>
                <Badge variant="warning">CONFIRMED</Badge>
              </TableCell>
              <TableCell>
                <Button size="sm" variant="primary">Claim &amp; Pick</Button>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>ORD-20261116-0039</TableCell>
              <TableCell>Ritu S. (Mumbai, 400001)</TableCell>
              <TableCell>1 item</TableCell>
              <TableCell>
                <Badge variant="neutral">ONLINE (SIMULATED)</Badge>
              </TableCell>
              <TableCell>
                <Badge variant="success">IN_PACKING</Badge>
              </TableCell>
              <TableCell>
                <Button size="sm" variant="outline">Print Packing Slip</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>

      {/* Quick Links */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
        <Link
          href="/manager/inventory"
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'block',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Boxes size={18} color="var(--color-success)" />
            <h4 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Stock Receiving</h4>
          </div>
          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.5 }}>
            Log manufactured garment quantities arriving at Surat warehouse ready stock.
          </p>
        </Link>

        <Link
          href="/manager/fulfillment/dispatch"
          style={{
            padding: '20px',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            display: 'block',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
            <Truck size={18} color="var(--color-success)" />
            <h4 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Daily Transport Handover</h4>
          </div>
          <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)', lineHeight: 1.5 }}>
            Prepare carrier manifests and assign parcels to external agencies (Delhivery, Bluedart).
          </p>
        </Link>
      </div>
    </div>
  );
}
