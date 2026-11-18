'use client';

import React, { useState } from 'react';
import { customerApi } from '@/lib/api/customer';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { MapPin, CheckCircle2, XCircle, Search } from 'lucide-react';

const AGENCIES = [
  { name: 'Blue Dart Express', serviceType: 'Air Express', hub: 'Surat Central', contact: '+91 261 2470000', status: 'ACTIVE' },
  { name: 'Delhivery Surface', serviceType: 'Surface Commercial', hub: 'Udhna Junction', contact: '+91 261 2890000', status: 'ACTIVE' },
  { name: 'Surat Local Express', serviceType: 'Same-Day Local', hub: 'Ring Road Market', contact: '+91 261 2550000', status: 'ACTIVE' },
];

export default function AdminLogisticsPage() {
  const { toast } = useToast();
  const [pincodeQuery, setPincodeQuery] = useState('');
  const [checking, setChecking] = useState(false);
  const [pincodeResult, setPincodeResult] = useState<{ pincode: string; serviceable: boolean } | null>(null);

  const handleCheckPincode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pincodeQuery.trim()) return;

    setChecking(true);
    try {
      const res = await customerApi.checkCodPincode(pincodeQuery.trim());
      if (res.data) {
        setPincodeResult(res.data);
      }
    } catch {
      toast({ type: 'error', title: 'Lookup Error', message: 'Unable to check PIN serviceability.' });
    } finally {
      setChecking(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          COD PINs &amp; Logistics Partners
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Courier transport agencies, delivery hub coverage, and Cash on Delivery postal code verification.
        </p>
      </div>

      {/* Pincode Lookup Tool */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          padding: '24px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
          <MapPin size={18} color="var(--wine)" />
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Verify COD Pincode Serviceability</h2>
        </div>

        <form onSubmit={handleCheckPincode} style={{ display: 'flex', gap: '12px', maxWidth: '440px' }}>
          <input
            type="text"
            className="form-input"
            placeholder="Enter 6-digit Indian PIN (e.g. 395002)"
            value={pincodeQuery}
            onChange={(e) => setPincodeQuery(e.target.value)}
            maxLength={6}
            style={{ flex: 1 }}
            required
          />
          <Button type="submit" variant="primary" isLoading={checking}>
            <Search size={14} style={{ marginRight: '6px' }} />
            Check PIN
          </Button>
        </form>

        {pincodeResult && (
          <div
            style={{
              marginTop: '16px',
              padding: '12px 16px',
              backgroundColor: pincodeResult.serviceable ? '#ecfdf5' : '#fef2f2',
              border: `1px solid ${pincodeResult.serviceable ? '#a7f3d0' : '#fecaca'}`,
              borderRadius: 'var(--radius-sm)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '10px',
              fontSize: '13px',
              color: pincodeResult.serviceable ? '#065f46' : '#991b1b',
            }}
          >
            {pincodeResult.serviceable ? (
              <>
                <CheckCircle2 size={16} color="#059669" />
                <span>PIN <strong>{pincodeResult.pincode}</strong>: Fully serviceable for Cash on Delivery &amp; Express Handover.</span>
              </>
            ) : (
              <>
                <XCircle size={16} color="#dc2626" />
                <span>PIN <strong>{pincodeResult.pincode}</strong>: COD is currently unserviceable. Online prepaid only.</span>
              </>
            )}
          </div>
        )}
      </div>

      {/* Transport Agency Partners */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Transport Courier Partners</h2>
        </div>

        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Agency Name</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Service Tier</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Surat Dispatch Hub</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Helpline</th>
              <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {AGENCIES.map((ag) => (
              <tr key={ag.name} style={{ borderBottom: '1px solid var(--line-light)' }}>
                <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                  {ag.name}
                </td>
                <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                  {ag.serviceType}
                </td>
                <td style={{ padding: '14px 16px' }}>{ag.hub}</td>
                <td style={{ padding: '14px 16px', color: 'var(--ink)' }}>{ag.contact}</td>
                <td style={{ padding: '14px 16px' }}>
                  <Badge variant="success">{ag.status}</Badge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
