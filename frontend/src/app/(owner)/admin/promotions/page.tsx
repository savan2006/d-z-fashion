'use client';

import React, { useEffect, useState } from 'react';
import {
  adminApi,
  PromotionResponse,
  PromotionType,
  PageResponse,
} from '@/lib/api/admin';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { Plus } from 'lucide-react';

const PROMO_TYPE_OPTIONS = [
  { value: 'FLAT_AMOUNT', label: 'Flat Rupee Discount (e.g. ₹500 off)' },
  { value: 'PERCENTAGE', label: 'Percentage Off (e.g. 15% off with max cap)' },
  { value: 'BUY_X_GET_Y', label: 'Buy X Get Y (e.g. Buy 2 Get 1)' },
  { value: 'FREE_DELIVERY', label: 'Free Delivery Waiver (Overrides ₹110 unit fee)' },
  { value: 'BUNDLE_DISCOUNT', label: 'Bundle Discount (Multi-garment tier)' },
];

export default function AdminPromotionsPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [promotionsPage, setPromotionsPage] = useState<PageResponse<PromotionResponse> | null>(null);

  // Create Modal State
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [promoType, setPromoType] = useState<PromotionType>('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('10');
  const [minimumSpend, setMinimumSpend] = useState('1500');
  const [maximumDiscountCap, setMaximumDiscountCap] = useState('500');
  const [totalUsageLimit, setTotalUsageLimit] = useState('100');
  const [perCustomerLimit, setPerCustomerLimit] = useState('1');
  const [saving, setSaving] = useState(false);

  const loadPromotions = () => {
    let isMounted = true;
    adminApi.getAllPromotions(0, 20)
      .then((res) => {
        if (!isMounted || !res.data) return;
        setPromotionsPage(res.data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  };

  useEffect(() => {
    loadPromotions();
  }, []);

  const handleCreatePromotion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim() || !name.trim()) {
      toast({ type: 'warning', title: 'Missing Information', message: 'Coupon code and campaign name are required.' });
      return;
    }

    setSaving(true);
    try {
      const today = new Date();
      const nextMonth = new Date();
      nextMonth.setMonth(nextMonth.getMonth() + 1);

      await adminApi.createPromotion({
        code: code.trim().toUpperCase(),
        name: name.trim(),
        promotionType: promoType,
        discountValue: parseFloat(discountValue) || 0,
        minimumSpend: parseFloat(minimumSpend) || 0,
        maximumDiscountCap: maximumDiscountCap ? parseFloat(maximumDiscountCap) : undefined,
        totalUsageLimit: totalUsageLimit ? parseInt(totalUsageLimit, 10) : undefined,
        perCustomerLimit: perCustomerLimit ? parseInt(perCustomerLimit, 10) : undefined,
        startDate: today.toISOString(),
        endDate: nextMonth.toISOString(),
      });

      toast({
        type: 'success',
        title: 'Promotion Created',
        message: `Campaign code ${code.trim().toUpperCase()} is now live.`,
      });
      setCreateModalOpen(false);
      setCode('');
      setName('');
      loadPromotions();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to create promotion';
      toast({ type: 'error', title: 'Creation Failed', message: msg });
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async (id: string, promoCode: string) => {
    try {
      await adminApi.deactivatePromotion(id);
      toast({ type: 'info', title: 'Deactivated', message: `Campaign ${promoCode} deactivated.` });
      loadPromotions();
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Deactivation failed.' });
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            Coupons &amp; Promotional Campaigns
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Owner campaign management: configure percentage discounts, flat waivers, usage caps, and minimum order rules.
          </p>
        </div>

        <Button variant="primary" onClick={() => setCreateModalOpen(true)}>
          <Plus size={16} style={{ marginRight: '6px' }} />
          Create Campaign
        </Button>
      </div>

      {/* Promos Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Promotional Rules</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !promotionsPage || promotionsPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No promotional campaigns configured.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Coupon Code</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Campaign Name</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Type &amp; Benefit</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Min Spend</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Usage</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {promotionsPage.content.map((p) => (
                <tr key={p.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--wine)' }}>
                    {p.code}
                  </td>
                  <td style={{ padding: '14px 16px', fontWeight: 500, color: 'var(--ink)' }}>
                    {p.name}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <span style={{ fontSize: '12px', fontWeight: 600 }}>
                      {p.promotionType === 'PERCENTAGE'
                        ? `${p.discountValue}% Off (Cap: ₹${p.maximumDiscountCap || 'None'})`
                        : p.promotionType === 'FLAT_AMOUNT'
                        ? `₹${p.discountValue} Flat Discount`
                        : p.promotionType.replace('_', ' ')}
                    </span>
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink)' }}>
                    ₹{p.minimumSpend.toLocaleString('en-IN')}
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                    {p.currentUsageCount} / {p.totalUsageLimit || '∞'}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant={p.active ? 'success' : 'neutral'}>
                      {p.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    {p.active && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDeactivate(p.id, p.code)}
                      >
                        Deactivate
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Create Promotion Modal */}
      {createModalOpen && (
        <Dialog
          isOpen={true}
          onClose={() => setCreateModalOpen(false)}
          title="Create Promotional Coupon"
          description="Define a promotional discount code with atomic customer limits and spend thresholds."
          maxWidth="640px"
        >
          <form onSubmit={handleCreatePromotion} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <TextField
                label="Coupon Code"
                placeholder="e.g. SURATFESTIVE"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                required
              />

              <TextField
                label="Campaign Title"
                placeholder="e.g. Diwali Silk Festival"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <Select
              label="Promotion Type"
              value={promoType}
              onChange={(e) => setPromoType(e.target.value as PromotionType)}
              options={PROMO_TYPE_OPTIONS}
              required
            />

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px' }}>
              <TextField
                label="Discount Value (% or ₹)"
                type="number"
                value={discountValue}
                onChange={(e) => setDiscountValue(e.target.value)}
                required
              />

              <TextField
                label="Min Order Spend (₹)"
                type="number"
                value={minimumSpend}
                onChange={(e) => setMinimumSpend(e.target.value)}
                required
              />

              <TextField
                label="Max Discount Cap (₹)"
                type="number"
                placeholder="e.g. 500"
                value={maximumDiscountCap}
                onChange={(e) => setMaximumDiscountCap(e.target.value)}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <TextField
                label="Total Campaign Limit"
                type="number"
                value={totalUsageLimit}
                onChange={(e) => setTotalUsageLimit(e.target.value)}
              />

              <TextField
                label="Per-Customer Limit"
                type="number"
                value={perCustomerLimit}
                onChange={(e) => setPerCustomerLimit(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setCreateModalOpen(false)} disabled={saving}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={saving}>
                Launch Campaign
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
