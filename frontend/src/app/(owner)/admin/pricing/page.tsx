'use client';

import React, { useEffect, useState } from 'react';
import { adminApi, AdminProductSummary } from '@/lib/api/admin';
import { catalogApi } from '@/lib/api/catalog';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { AlertTriangle } from 'lucide-react';

export default function AdminPricingPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [products, setProducts] = useState<AdminProductSummary[]>([]);

  // Price Edit Modal State
  const [selectedProduct, setSelectedProduct] = useState<AdminProductSummary | null>(null);
  const [basePrice, setBasePrice] = useState('');
  const [discountPrice, setDiscountPrice] = useState('');
  const [changeReason, setChangeReason] = useState('');
  const [saving, setSaving] = useState(false);

  const loadProducts = () => {
    let isMounted = true;
    catalogApi.getProducts({ pageSize: 50 })
      .then((res) => {
        if (!isMounted || !res.data?.content) return;
        setProducts(
          res.data.content.map((p) => ({
            id: p.id,
            title: p.name,
            slug: p.slug,
            categoryName: p.categoryName,
            categorySlug: p.slug,
            status: 'PUBLISHED',
            basePrice: p.defaultPrice,
            discountPrice: p.defaultPrice,
            fabrication: p.fabric || undefined,
            totalVariants: 4,
            hasPendingRevision: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }))
        );
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
    loadProducts();
  }, []);

  const handleOpenEdit = (p: AdminProductSummary) => {
    setSelectedProduct(p);
    setBasePrice(String(p.basePrice || ''));
    setDiscountPrice(p.discountPrice ? String(p.discountPrice) : '');
    setChangeReason('');
  };

  const handleSavePrice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) return;
    if (!basePrice || parseFloat(basePrice) <= 0) {
      toast({ type: 'warning', title: 'Invalid Price', message: 'Please enter a valid base price.' });
      return;
    }
    if (!changeReason.trim()) {
      toast({ type: 'warning', title: 'Reason Required', message: 'Audit compliance requires a reason for price modification.' });
      return;
    }

    setSaving(true);
    try {
      await adminApi.updateProductPrice(selectedProduct.id, {
        basePrice: parseFloat(basePrice),
        discountPrice: discountPrice ? parseFloat(discountPrice) : undefined,
        changeReason: changeReason.trim(),
      });
      toast({
        type: 'success',
        title: 'Price Updated',
        message: `Updated pricing for ${selectedProduct.title}. Active customer carts will acknowledge shift before checkout.`,
      });
      setSelectedProduct(null);
      loadProducts();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to update pricing';
      toast({ type: 'error', title: 'Update Error', message: msg });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Price Management
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Exclusive Owner price controls. Updates trigger authoritative price shift confirmation gates for customer shopping bags.
        </p>
      </div>

      {/* Warning Invariant Notice */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          backgroundColor: '#fffbeb',
          border: '1px solid #fde68a',
          borderRadius: 'var(--radius-sm)',
          fontSize: '13px',
          color: '#92400e',
        }}
      >
        <AlertTriangle size={20} color="#b45309" />
        <div>
          <strong>Owner-Only Security Invariant:</strong> Operations Managers are strictly prohibited from viewing or editing garment pricing. All price edits are recorded in the immutable audit log with your mandatory rationale.
        </div>
      </div>

      {/* Products Pricing Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Collection Pricing</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : products.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No products in catalog.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Category</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Current Base Price</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Discount Price</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>GST Implication</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => {
                const effectivePrice = p.discountPrice || p.basePrice || 0;
                const gstRate = effectivePrice <= 1000 ? 5 : 12;
                return (
                  <tr key={p.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      {p.title}
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                      {p.categoryName}
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      ₹{p.basePrice?.toLocaleString('en-IN', { minimumFractionDigits: 2 }) || '—'}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {p.discountPrice ? (
                        <span style={{ color: 'var(--color-success)', fontWeight: 600 }}>
                          ₹{p.discountPrice.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--ink-muted)' }}>None</span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant="neutral">{gstRate}% GST (Surat Fabric)</Badge>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <Button variant="outline" size="sm" onClick={() => handleOpenEdit(p)}>
                        Update Price
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Edit Price Modal */}
      {selectedProduct && (
        <Dialog
          isOpen={true}
          onClose={() => setSelectedProduct(null)}
          title={`Update Pricing: ${selectedProduct.title}`}
          description="Adjust base and discount prices. Customers with this garment in their shopping bag will be required to confirm the price change before completing checkout."
        >
          <form onSubmit={handleSavePrice} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <TextField
                label="Base Price (₹ excl. GST)"
                type="number"
                value={basePrice}
                onChange={(e) => setBasePrice(e.target.value)}
                required
              />

              <TextField
                label="Promotional Discount Price (₹ Optional)"
                type="number"
                placeholder="Leave blank for regular price"
                value={discountPrice}
                onChange={(e) => setDiscountPrice(e.target.value)}
              />
            </div>

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Audit Change Reason <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. Surat textile raw material inflation, seasonal clearance markdown..."
                value={changeReason}
                onChange={(e) => setChangeReason(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setSelectedProduct(null)} disabled={saving}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={saving}>
                Save Price Adjustment
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
