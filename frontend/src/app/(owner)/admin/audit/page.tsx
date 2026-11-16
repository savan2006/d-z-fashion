import React from 'react';
import Link from 'next/link';
import { Search, Filter, ArrowLeft, Lock } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { TextField } from '@/components/ui/TextField';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/Table';

const MOCK_AUDIT_LOGS = [
  {
    id: 'aud-001',
    timestamp: '2026-11-16 16:15:30',
    actorRole: 'OWNER',
    actorEmail: 'owner@dzfashion.com',
    action: 'APPROVE_REFUND',
    entityType: 'Return',
    entityId: 'RET-20261116-0001',
    details: 'Refund of ₹1,499 approved. 7-day processing window initialized.',
    ip: '192.168.1.1',
  },
  {
    id: 'aud-002',
    timestamp: '2026-11-16 15:42:12',
    actorRole: 'MANAGER',
    actorEmail: 'manager@dzfashion.com',
    action: 'INSPECT_RETURN',
    entityType: 'Return',
    entityId: 'RET-20261116-0001',
    details: 'Physical parcel inspected at Surat warehouse. Condition: PASSED, disposition: DAMAGED_QUARANTINE.',
    ip: '10.0.0.12',
  },
  {
    id: 'aud-003',
    timestamp: '2026-11-16 14:10:05',
    actorRole: 'CUSTOMER',
    actorEmail: 'priya@example.com',
    action: 'LOGIN_ATTEMPT',
    entityType: 'Auth',
    entityId: 'usr-9948',
    details: '{"action":"login", password=***MASKED***, otp=***MASKED***, token=***MASKED***, "auth":"Bearer ***MASKED***"}',
    ip: '157.34.22.9',
  },
  {
    id: 'aud-004',
    timestamp: '2026-11-16 12:05:44',
    actorRole: 'MANAGER',
    actorEmail: 'manager@dzfashion.com',
    action: 'CLAIM_ORDER',
    entityType: 'Order',
    entityId: 'ORD-20261116-0042',
    details: 'Claimed order for picking and packing at Surat facility.',
    ip: '10.0.0.12',
  },
];

export default function AdminAuditPage() {
  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <Link
          href="/admin"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            marginBottom: '12px',
          }}
        >
          <ArrowLeft size={14} />
          <span>Back to Control Center</span>
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
          <Badge variant="wine" icon={<Lock size={12} />}>RESTRICTED: ROLE_OWNER ONLY</Badge>
        </div>
        <h1 className="font-serif" style={{ margin: '0 0 6px', fontSize: 'var(--text-heading-xl)', fontWeight: 500 }}>
          Immutable Audit Trail
        </h1>
        <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
          Append-only compliance log. All passwords, OTPs, auth tokens and credentials are regex-scrubbed before persistence.
        </p>
      </div>

      <div
        style={{
          display: 'flex',
          gap: '12px',
          marginBottom: '20px',
          alignItems: 'flex-end',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ flex: 1, minWidth: '240px' }}>
          <TextField
            placeholder="Search by action, entity ID or actor email..."
            startIcon={<Search size={16} />}
          />
        </div>
        <Button variant="secondary" size="md" startIcon={<Filter size={16} />}>
          Filter by Entity
        </Button>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Timestamp</TableHead>
            <TableHead>Actor</TableHead>
            <TableHead>Action</TableHead>
            <TableHead>Entity</TableHead>
            <TableHead>Sanitized Details (Secrets Scrubbed)</TableHead>
            <TableHead>IP Address</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {MOCK_AUDIT_LOGS.map((log) => (
            <TableRow key={log.id}>
              <TableCell style={{ fontSize: '12px', fontFamily: 'var(--font-mono)', whiteSpace: 'nowrap' }}>
                {log.timestamp}
              </TableCell>
              <TableCell>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>{log.actorEmail}</div>
                <Badge
                  variant={log.actorRole === 'OWNER' ? 'wine' : log.actorRole === 'MANAGER' ? 'success' : 'neutral'}
                  style={{ fontSize: '9px', marginTop: '2px' }}
                >
                  {log.actorRole}
                </Badge>
              </TableCell>
              <TableCell style={{ fontWeight: 600 }}>{log.action}</TableCell>
              <TableCell>
                <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>{log.entityType}</span>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>{log.entityId}</div>
              </TableCell>
              <TableCell style={{ maxWidth: '380px', fontSize: '12px', color: 'var(--ink-muted)' }}>
                {log.details}
              </TableCell>
              <TableCell style={{ fontSize: '11px', fontFamily: 'var(--font-mono)' }}>
                {log.ip}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
