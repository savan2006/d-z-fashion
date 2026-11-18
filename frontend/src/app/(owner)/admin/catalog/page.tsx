'use client';

import React, { useEffect, useState } from 'react';
import {
  adminApi,
  AdminProductSummary,
  ProductRevisionDiff,
} from '@/lib/api/admin';
import { catalogApi, Category } from '@/lib/api/catalog';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import {
  Plus,
  AlertCircle,
} from 'lucide-react';

function getProductStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'PUBLISHED':
      return 'success';
    case 'PENDING_REVISION':
      return 'warning';
    case 'DRAFT':
      return 'neutral';
    case 'ARCHIVED':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function AdminCatalogPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [products, setProducts] = useState<AdminProductSummary[]>([]);
  const [pendingRevisions, setPendingRevisions] = useState<ProductRevisionDiff[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');

  // Review Diff Modal State
  const [selectedRevision, setSelectedRevision] = useState<ProductRevisionDiff | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [reviewing, setReviewing] = useState(false);

  // Create Product Modal State
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newCategoryId, setNewCategoryId] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newFabrication, setNewFabrication] = useState('');
  const [newBasePrice, setNewBasePrice] = useState('2999');
  const [newDiscountPrice, setNewDiscountPrice] = useState('');
  const [creating, setCreating] = useState(false);

  const loadData = () => {
    let isMounted = true;
    Promise.all([
      catalogApi.getProducts({ pageSize: 50 }).catch(() => null),
      adminApi.getPendingRevisions().catch(() => null),
      catalogApi.getCategories().catch(() => null),
    ]).then(([prodRes, revRes, catRes]) => {
      if (!isMounted) return;
      if (prodRes?.data?.content) {
        setProducts(
          prodRes.data.content.map((p) => ({
            id: p.id,
            title: p.name,
            slug: p.slug,
            categoryName: p.categoryName,
            categorySlug: p.slug,
            status: 'PUBLISHED',
            basePrice: p.defaultPrice,
            discountPrice: p.defaultPrice,
            fabrication: p.fabric || undefined,
            occasion: p.occasion || undefined,
            totalVariants: 4,
            hasPendingRevision: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }))
        );
      }
      if (revRes?.data) setPendingRevisions(revRes.data);
      if (catRes?.data) setCategories(catRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleReviewRevision = async (status: 'APPROVED' | 'REJECTED') => {
    if (!selectedRevision) return;
    if (status === 'REJECTED' && !rejectionReason.trim()) {
      toast({ type: 'warning', title: 'Rejection Reason Required', message: 'Please provide feedback for the manager.' });
      return;
    }

    setReviewing(true);
    try {
      await adminApi.reviewRevision(selectedRevision.id, status, rejectionReason.trim() || undefined);
      toast({
        type: 'success',
        title: status === 'APPROVED' ? 'Revision Approved' : 'Revision Rejected',
        message: status === 'APPROVED' ? 'Product changes published to catalog.' : 'Feedback sent to staff.',
      });
      setSelectedRevision(null);
      setRejectionReason('');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to review revision';
      toast({ type: 'error', title: 'Review Failed', message: msg });
    } finally {
      setReviewing(false);
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !newCategoryId || !newFabrication.trim()) {
      toast({ type: 'warning', title: 'Missing Fields', message: 'Please complete all required fields.' });
      return;
    }

    setCreating(true);
    try {
      await adminApi.createProductOwner({
        title: newTitle.trim(),
        categoryId: newCategoryId,
        description: newDescription.trim(),
        fabrication: newFabrication.trim(),
        basePrice: parseFloat(newBasePrice) || 0,
        discountPrice: newDiscountPrice ? parseFloat(newDiscountPrice) : undefined,
        variants: [
          { size: 'S', colour: 'Surat Red', sku: `${newTitle.slice(0, 3).toUpperCase()}-RED-S` },
          { size: 'M', colour: 'Surat Red', sku: `${newTitle.slice(0, 3).toUpperCase()}-RED-M` },
          { size: 'L', colour: 'Surat Red', sku: `${newTitle.slice(0, 3).toUpperCase()}-RED-L` },
        ],
      });
      toast({ type: 'success', title: 'Garment Created', message: 'New piece added to Surat collection catalog.' });
      setCreateModalOpen(false);
      setNewTitle('');
      setNewDescription('');
      setNewFabrication('');
      loadData();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to create garment';
      toast({ type: 'error', title: 'Creation Failed', message: msg });
    } finally {
      setCreating(false);
    }
  };

  const handlePublishToggle = async (productId: string, isPublished: boolean) => {
    try {
      if (isPublished) {
        await adminApi.archiveProduct(productId);
        toast({ type: 'info', title: 'Archived', message: 'Product archived from active catalog.' });
      } else {
        await adminApi.publishProduct(productId);
        toast({ type: 'success', title: 'Published', message: 'Garment published live to storefront.' });
      }
      loadData();
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Status toggle failed.' });
    }
  };

  const filteredProducts = products.filter((p) => {
    if (filterStatus === 'ALL') return true;
    return p.status === filterStatus;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            Catalog &amp; Revisions
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Publish new ethnic collections, control garment lifecycle, and inspect manager draft revisions.
          </p>
        </div>

        <Button variant="primary" onClick={() => setCreateModalOpen(true)}>
          <Plus size={16} style={{ marginRight: '6px' }} />
          New Garment Creation
        </Button>
      </div>

      {/* Pending Revisions Diff Banner */}
      {pendingRevisions.length > 0 && (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid #fde68a',
            borderRadius: 'var(--radius-sm)',
            padding: '20px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
            <AlertCircle size={18} color="#b45309" />
            <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#92400e' }}>
              Pending Manager Revisions ({pendingRevisions.length})
            </h2>
          </div>
          <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#78350f' }}>
            Operations managers have proposed edits for the garments below. Review before approving to live catalog.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {pendingRevisions.map((rev) => (
              <div
                key={rev.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  backgroundColor: '#fffbeb',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid #fef3c7',
                }}
              >
                <div>
                  <span style={{ fontWeight: 600, fontSize: '13px' }}>{rev.productTitle}</span>
                  <span style={{ fontSize: '12px', color: 'var(--ink-muted)', marginLeft: '8px' }}>
                    Proposed by {rev.submittedBy} on {new Date(rev.submittedAt).toLocaleDateString('en-IN')}
                  </span>
                </div>
                <Button variant="outline" size="sm" onClick={() => setSelectedRevision(rev)}>
                  Compare Changes
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Products Listing Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--line-light)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Catalog Collection ({filteredProducts.length})</h2>

          <div style={{ display: 'flex', gap: '8px' }}>
            {['ALL', 'PUBLISHED', 'DRAFT', 'PENDING_REVISION'].map((st) => (
              <button
                key={st}
                type="button"
                onClick={() => setFilterStatus(st)}
                style={{
                  padding: '4px 10px',
                  fontSize: '12px',
                  borderRadius: 'var(--radius-xs)',
                  border: '1px solid var(--line)',
                  backgroundColor: filterStatus === st ? 'var(--wine)' : '#ffffff',
                  color: filterStatus === st ? '#ffffff' : 'var(--ink)',
                  cursor: 'pointer',
                  fontWeight: 500,
                }}
              >
                {st.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : filteredProducts.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No garments match the selected status.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment Title</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Category</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Fabrication</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Base Price</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((p) => {
                const isPublished = p.status === 'PUBLISHED';
                return (
                  <tr key={p.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                      {p.title}
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                      {p.categoryName}
                    </td>
                    <td style={{ padding: '14px 16px', color: 'var(--ink)' }}>
                      {p.fabrication || 'Pure Silk / Cotton'}
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--wine)' }}>
                      ₹{p.basePrice?.toLocaleString('en-IN') || '—'}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <Badge variant={getProductStatusVariant(p.status)}>
                        {p.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePublishToggle(p.id, isPublished)}
                      >
                        {isPublished ? 'Archive' : 'Publish'}
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Revision Diff Viewer Modal */}
      {selectedRevision && (
        <Dialog
          isOpen={true}
          onClose={() => setSelectedRevision(null)}
          title={`Review Revision: ${selectedRevision.productTitle}`}
          description={`Submitted by ${selectedRevision.submittedBy}. Compare proposed changes against live catalog.`}
          maxWidth="720px"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              {/* Live Values */}
              <div style={{ padding: '14px', backgroundColor: 'var(--surface-subtle)', borderRadius: 'var(--radius-sm)' }}>
                <h3 style={{ margin: '0 0 10px', fontSize: '13px', fontWeight: 600, color: 'var(--ink-muted)' }}>
                  Current Live Catalog Value
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
                  <div>
                    <strong>Title:</strong> {selectedRevision.liveValues?.title || '—'}
                  </div>
                  <div>
                    <strong>Fabrication:</strong> {selectedRevision.liveValues?.fabrication || '—'}
                  </div>
                  <div>
                    <strong>Description:</strong> {selectedRevision.liveValues?.description || '—'}
                  </div>
                </div>
              </div>

              {/* Proposed Values */}
              <div style={{ padding: '14px', backgroundColor: '#ecfdf5', borderRadius: 'var(--radius-sm)', border: '1px solid #a7f3d0' }}>
                <h3 style={{ margin: '0 0 10px', fontSize: '13px', fontWeight: 600, color: '#047857' }}>
                  Proposed Manager Updates
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px', color: '#065f46' }}>
                  <div>
                    <strong>Title:</strong> {selectedRevision.proposedChanges?.title || '—'}
                  </div>
                  <div>
                    <strong>Fabrication:</strong> {selectedRevision.proposedChanges?.fabrication || '—'}
                  </div>
                  <div>
                    <strong>Description:</strong> {selectedRevision.proposedChanges?.description || '—'}
                  </div>
                </div>
              </div>
            </div>

            {/* Rejection Reason textarea if rejecting */}
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Rejection Reason / Revision Feedback (Required if rejecting)
              </label>
              <textarea
                className="form-input"
                rows={2}
                placeholder="e.g. Ensure Surat zari specifications and accurate thread count are mentioned..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleReviewRevision('REJECTED')}
                isLoading={reviewing}
                style={{ color: 'var(--error)' }}
              >
                Reject Revision
              </Button>
              <Button
                type="button"
                variant="primary"
                onClick={() => handleReviewRevision('APPROVED')}
                isLoading={reviewing}
              >
                Approve &amp; Publish Changes
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Create Product Modal */}
      {createModalOpen && (
        <Dialog
          isOpen={true}
          onClose={() => setCreateModalOpen(false)}
          title="Create New Garment"
          description="Owner collection creation: specify Surat weave attributes and initial pricing."
          maxWidth="640px"
        >
          <form onSubmit={handleCreateProduct} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="Garment Title"
              placeholder="e.g. Surat Bandhani Silk Saree"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              required
            />

            <Select
              label="Category"
              value={newCategoryId}
              onChange={(e) => setNewCategoryId(e.target.value)}
              options={[
                { value: '', label: '— Select Collection Category —' },
                ...categories.map((c) => ({ value: c.id, label: c.name })),
              ]}
              required
            />

            <TextField
              label="Fabrication / Material"
              placeholder="e.g. 100% Pure Surat Chanderi Silk with Golden Zari"
              value={newFabrication}
              onChange={(e) => setNewFabrication(e.target.value)}
              required
            />

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <TextField
                label="Base Price (₹ excl. GST)"
                type="number"
                value={newBasePrice}
                onChange={(e) => setNewBasePrice(e.target.value)}
                required
              />

              <TextField
                label="Discount Price (₹ Optional)"
                type="number"
                placeholder="e.g. 2499"
                value={newDiscountPrice}
                onChange={(e) => setNewDiscountPrice(e.target.value)}
              />
            </div>

            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Description
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="Garment details, drape, styling notes..."
                value={newDescription}
                onChange={(e) => setNewDescription(e.target.value)}
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setCreateModalOpen(false)} disabled={creating}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={creating}>
                Create &amp; Initialize
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
