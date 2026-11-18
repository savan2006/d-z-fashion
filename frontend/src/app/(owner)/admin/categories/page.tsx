'use client';

import React, { useEffect, useState } from 'react';
import { adminApi } from '@/lib/api/admin';
import { catalogApi, Category } from '@/lib/api/catalog';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { Plus, ShieldCheck } from 'lucide-react';

export default function AdminCategoriesPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [categories, setCategories] = useState<Category[]>([]);

  // Modal State
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [description, setDescription] = useState('');
  const [hsnCode, setHsnCode] = useState('5208'); // Indian cotton/silk fabric HSN
  const [gstRatePercent, setGstRatePercent] = useState('5');
  const [saving, setSaving] = useState(false);

  const loadCategories = () => {
    let isMounted = true;
    catalogApi.getCategories()
      .then((res) => {
        if (!isMounted || !res.data) return;
        setCategories(res.data);
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
    loadCategories();
  }, []);

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setSaving(true);
    try {
      const generatedSlug = slug.trim() || name.trim().toLowerCase().replace(/\s+/g, '-');
      await adminApi.createCategory({
        name: name.trim(),
        slug: generatedSlug,
        description: description.trim() || undefined,
        hsnCode: hsnCode.trim() || undefined,
        gstRatePercent: parseInt(gstRatePercent, 10) || 5,
      });

      toast({
        type: 'success',
        title: 'Category Created',
        message: `Category "${name}" added to Surat ethnic catalog taxonomy.`,
      });
      setCreateModalOpen(false);
      setName('');
      setSlug('');
      setDescription('');
      loadCategories();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to create category';
      toast({ type: 'error', title: 'Creation Failed', message: msg });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            Categories &amp; Tax Classification
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Manage Surat ethnic taxonomy, display hierarchy, and Indian GST / HSN rate classification.
          </p>
        </div>

        <Button variant="primary" onClick={() => setCreateModalOpen(true)}>
          <Plus size={16} style={{ marginRight: '6px' }} />
          Add Category
        </Button>
      </div>

      {/* Tax Invariant Info Card */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '13px',
          color: 'var(--ink-secondary)',
        }}
      >
        <ShieldCheck size={20} color="var(--wine)" />
        <div>
          <strong>Surat Fabric GST Compliance:</strong> Under Indian textile GST law, apparel valued &le; ₹1,000 incurs 5% GST; apparel valued &gt; ₹1,000 incurs 12% GST. Categories bind default HSN codes for sequential billing.
        </div>
      </div>

      {/* Categories Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Categories ({categories.length})</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : categories.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No categories defined.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Category Name</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>URL Slug</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Description</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>HSN &amp; GST</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                    {c.name}
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                    /{c.slug}
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink-secondary)', maxWidth: '320px' }}>
                    {c.description || 'Authentic handcrafted Surat ethnic wear'}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant="neutral">HSN 5208 &bull; 5%/12% GST</Badge>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant="success">Active</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Create Category Modal */}
      {createModalOpen && (
        <Dialog
          isOpen={true}
          onClose={() => setCreateModalOpen(false)}
          title="Create Product Category"
          description="Define a new taxonomy category for Surat sarees, lehengas, kurtas, or co-ord sets."
        >
          <form onSubmit={handleCreateCategory} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="Category Name"
              placeholder="e.g. Surat Bandhani Sarees"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (!slug) setSlug(e.target.value.toLowerCase().replace(/\s+/g, '-'));
              }}
              required
            />

            <TextField
              label="URL Slug"
              placeholder="e.g. surat-bandhani-sarees"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
              required
            />

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <TextField
                label="HSN Classification Code"
                placeholder="e.g. 5208 / 5407"
                value={hsnCode}
                onChange={(e) => setHsnCode(e.target.value)}
              />

              <TextField
                label="Standard GST Rate %"
                type="number"
                value={gstRatePercent}
                onChange={(e) => setGstRatePercent(e.target.value)}
              />
            </div>

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Description
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="Category narrative for collection header..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setCreateModalOpen(false)} disabled={saving}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={saving}>
                Save Category
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
