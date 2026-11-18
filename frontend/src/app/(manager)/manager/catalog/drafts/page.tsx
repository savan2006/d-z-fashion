'use client';

import React, { useEffect, useState, useTransition } from 'react';
import {
  Shirt,
  Plus,
  Send,
  CheckCircle2,
  Search,
  Lock,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  AdminProductSummary,
  CreateProductDraftRequest,
} from '@/lib/api/admin';

export default function ManagerProductDraftsPage() {
  const [products, setProducts] = useState<AdminProductSummary[]>([]);
  const [categories, setCategories] = useState<{ id: string; name: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // New Draft Modal State (STRICTLY NO PRICE FIELDS!)
  const [isCreatingDraft, setIsCreatingDraft] = useState(false);
  const [title, setTitle] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [fabrication, setFabrication] = useState('');
  const [careInstructions, setCareInstructions] = useState('');
  const [occasion, setOccasion] = useState('Festive & Wedding');
  const [variants, setVariants] = useState<Array<{ size: string; colour: string; sku: string }>>([
    { size: 'M', colour: 'Maroon', sku: 'SRT-MAR-M' },
  ]);
  const [isSubmittingDraft, startDraftTransition] = useTransition();

  // Revision Proposal Modal State (STRICTLY NO PRICE FIELDS!)
  const [revisionProduct, setRevisionProduct] = useState<AdminProductSummary | null>(null);
  const [revisedTitle, setRevisedTitle] = useState('');
  const [revisedDescription, setRevisedDescription] = useState('');
  const [revisedFabrication, setRevisedFabrication] = useState('');
  const [revisedCareInstructions, setRevisedCareInstructions] = useState('');
  const [revisedOccasion, setRevisedOccasion] = useState('');
  const [isSubmittingRevision, startRevisionTransition] = useTransition();

  const loadData = React.useCallback(() => {
    Promise.all([
      adminApi.getCategoryTree().catch(() => null),
    ])
      .then(([catRes]) => {
        if (catRes?.data) {
          setCategories(catRes.data as unknown as { id: string; name: string }[]);
          if (catRes.data.length > 0 && !categoryId) {
            setCategoryId((catRes.data[0] as { id: string }).id);
          }
        }
        // Mock / initial manager-accessible products list
        setProducts([
          {
            id: 'prod-101',
            title: 'Surat Handloom Chanderi Silk Saree',
            slug: 'surat-handloom-chanderi-silk-saree',
            categoryName: 'Sarees',
            categorySlug: 'sarees',
            status: 'PUBLISHED',
            fabrication: 'Pure Chanderi Silk with Zari Border',
            occasion: 'Festive & Wedding',
            totalVariants: 4,
            hasPendingRevision: false,
            createdAt: '2026-11-10T10:00:00Z',
            updatedAt: '2026-11-15T12:00:00Z',
          },
          {
            id: 'prod-102',
            title: 'Royal Anarkali Kurta Set',
            slug: 'royal-anarkali-kurta-set',
            categoryName: 'Kurtas & Suits',
            categorySlug: 'kurtas',
            status: 'PENDING_REVISION',
            fabrication: 'Mulmul Cotton with Hand Embroidery',
            occasion: 'Festive',
            totalVariants: 3,
            hasPendingRevision: true,
            createdAt: '2026-11-12T14:00:00Z',
            updatedAt: '2026-11-17T09:00:00Z',
          },
          {
            id: 'prod-103',
            title: 'Banarasi Brocade Dupatta',
            slug: 'banarasi-brocade-dupatta',
            categoryName: 'Dupattas',
            categorySlug: 'dupattas',
            status: 'DRAFT',
            fabrication: 'Banarasi Katan Silk',
            occasion: 'Ceremony',
            totalVariants: 2,
            hasPendingRevision: false,
            createdAt: '2026-11-18T05:00:00Z',
            updatedAt: '2026-11-18T05:00:00Z',
          },
        ]);
      })
      .finally(() => setLoading(false));
  }, [categoryId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAddVariant = () => {
    setVariants((prev) => [...prev, { size: 'L', colour: 'Gold', sku: `SRT-VAR-${prev.length + 1}` }]);
  };

  const handleRemoveVariant = (index: number) => {
    setVariants((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleCreateDraftSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !categoryId || !fabrication.trim() || variants.length === 0) return;

    startDraftTransition(async () => {
      try {
        const payload: CreateProductDraftRequest = {
          title: title.trim(),
          categoryId,
          description: description.trim(),
          fabrication: fabrication.trim(),
          careInstructions: careInstructions.trim() || undefined,
          occasion: occasion.trim() || undefined,
          variants,
        };

        await adminApi.createProductDraft(payload);
        setFeedback({
          type: 'success',
          text: `Draft "${payload.title}" created successfully. It is saved in DRAFT status and ready for revision submission to Owner.`,
        });

        setIsCreatingDraft(false);
        setTitle('');
        setDescription('');
        setFabrication('');
        setCareInstructions('');
        loadData();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to create product draft.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleOpenRevisionModal = (prod: AdminProductSummary) => {
    setRevisionProduct(prod);
    setRevisedTitle(prod.title);
    setRevisedDescription('');
    setRevisedFabrication(prod.fabrication || '');
    setRevisedCareInstructions('');
    setRevisedOccasion(prod.occasion || 'Festive');
  };

  const handleSubmitRevisionForm = (e: React.FormEvent) => {
    e.preventDefault();
    if (!revisionProduct) return;

    startRevisionTransition(async () => {
      try {
        const proposedChanges: Record<string, unknown> = {};
        if (revisedTitle.trim() && revisedTitle !== revisionProduct.title) {
          proposedChanges.title = revisedTitle.trim();
        }
        if (revisedDescription.trim()) {
          proposedChanges.description = revisedDescription.trim();
        }
        if (revisedFabrication.trim() && revisedFabrication !== revisionProduct.fabrication) {
          proposedChanges.fabrication = revisedFabrication.trim();
        }
        if (revisedCareInstructions.trim()) {
          proposedChanges.careInstructions = revisedCareInstructions.trim();
        }
        if (revisedOccasion.trim() && revisedOccasion !== revisionProduct.occasion) {
          proposedChanges.occasion = revisedOccasion.trim();
        }

        if (Object.keys(proposedChanges).length === 0) {
          setFeedback({
            type: 'error',
            text: 'Please modify at least one field before submitting revision for Owner review.',
          });
          return;
        }

        await adminApi.submitRevision(revisionProduct.id, proposedChanges);

        setFeedback({
          type: 'success',
          text: `Revision submitted for "${revisionProduct.title}". Sent to Owner review queue for diff review and approval.`,
        });

        setRevisionProduct(null);
        loadData();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to submit product revision.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const filteredProducts = products.filter(
    (p) =>
      p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.categoryName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (p.fabrication && p.fabrication.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
            Garment Drafts & Revision Proposals
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Draft new catalog styles and propose editorial adjustments. Final pricing and publication are strictly governed by the Owner.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsCreatingDraft(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Create Garment Draft
        </Button>
      </div>

      {/* Permission Notice */}
      <div
        style={{
          padding: '12px 16px',
          backgroundColor: 'var(--paper-warm)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid var(--line)',
          marginBottom: '20px',
          fontSize: '12px',
          color: 'var(--ink-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}
      >
        <Lock size={15} style={{ color: 'var(--wine)' }} />
        <span>
          <strong>Operational Policy:</strong> Managers curate garment specifications, weave details, and measurements. Retail prices, publication, and deletions are reserved for Owner review.
        </span>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          role="status"
          style={{
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '20px',
            fontSize: '13px',
            backgroundColor: feedback.type === 'success' ? 'var(--color-success-bg)' : 'var(--error-soft)',
            color: feedback.type === 'success' ? 'var(--color-success)' : 'var(--error)',
            border: `1px solid ${feedback.type === 'success' ? '#bbf7d0' : '#fecaca'}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={16} />
            <span>{feedback.text}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
          >
            &times;
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div style={{ marginBottom: '20px', position: 'relative', maxWidth: '360px' }}>
        <Search
          size={14}
          style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
        />
        <input
          type="text"
          placeholder="Search garment title or fabrication..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{
            width: '100%',
            padding: '6px 10px 6px 32px',
            fontSize: '13px',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
          }}
        />
      </div>

      {/* Product List Table (Manager Zero-Finance: NO PRICES!) */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredProducts.length === 0 ? (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '48px 24px',
            textAlign: 'center',
            color: 'var(--ink-muted)',
          }}
        >
          <Shirt size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No garment styles found
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Create a new draft to propose a collection piece for Owner merchandising.
          </p>
        </div>
      ) : (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            overflowX: 'auto',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--paper-warm)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Garment Title</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Category</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Fabrication & Weave</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Variants</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((prod) => (
                <tr key={prod.id} style={{ borderBottom: '1px solid var(--line)' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    <div>{prod.title}</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 400 }}>
                      Slug: {prod.slug}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px' }}>{prod.categoryName}</td>
                  <td style={{ padding: '12px 16px', color: 'var(--ink-muted)' }}>
                    {prod.fabrication || 'Handloom Silk'}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 500 }}>
                    {prod.totalVariants} variants
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <Badge
                        variant={
                          prod.status === 'PUBLISHED'
                            ? 'success'
                            : prod.status === 'PENDING_REVISION'
                            ? 'warning'
                            : 'secondary'
                        }
                        size="sm"
                      >
                        {prod.status === 'PENDING_REVISION' ? 'Revision in Owner Review' : prod.status}
                      </Badge>
                      {prod.hasPendingRevision && (
                        <span style={{ fontSize: '11px', color: '#b45309', fontWeight: 500 }}>
                          Pending Owner Approval
                        </span>
                      )}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleOpenRevisionModal(prod)}
                    >
                      <Send size={13} style={{ marginRight: '4px' }} />
                      Propose Revision
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Product Draft Modal (Strictly Zero Finance!) */}
      {isCreatingDraft && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '560px',
              width: '100%',
              padding: '28px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Create Garment Draft (Manager Submission)
              </h2>
              <button
                onClick={() => setIsCreatingDraft(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '12px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Submit garment craft details. Pricing and publishing are withheld until Owner authorization.
            </p>

            <form onSubmit={handleCreateDraftSubmit}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Product Title *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Surat Organza Zari Embroidered Saree"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Category *
                  </label>
                  <select
                    required
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: '#ffffff',
                    }}
                  >
                    <option value="">Select category...</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                    <option value="cat-sarees">Sarees</option>
                    <option value="cat-kurtas">Kurtas & Suits</option>
                    <option value="cat-dupattas">Dupattas</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Occasion
                  </label>
                  <input
                    type="text"
                    value={occasion}
                    onChange={(e) => setOccasion(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Fabrication & Weave Details *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 100% Pure Mulberry Silk, Surat Handloom Zari"
                  value={fabrication}
                  onChange={(e) => setFabrication(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Garment Description
                </label>
                <textarea
                  rows={2}
                  placeholder="Detailed aesthetic description of craft and fall..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600 }}>Variants (Size / Colour / SKU)</label>
                  <button
                    type="button"
                    onClick={handleAddVariant}
                    style={{
                      fontSize: '12px',
                      color: 'var(--wine)',
                      fontWeight: 600,
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                    }}
                  >
                    + Add Size Variant
                  </button>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {variants.map((v, idx) => (
                    <div key={idx} style={{ display: 'grid', gridTemplateColumns: '80px 1fr 1fr 32px', gap: '8px', alignItems: 'center' }}>
                      <input
                        type="text"
                        placeholder="Size"
                        value={v.size}
                        onChange={(e) => {
                          const val = e.target.value;
                          setVariants((prev) => prev.map((item, i) => (i === idx ? { ...item, size: val } : item)));
                        }}
                        style={{ padding: '6px 8px', fontSize: '12px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}
                      />
                      <input
                        type="text"
                        placeholder="Colour"
                        value={v.colour}
                        onChange={(e) => {
                          const val = e.target.value;
                          setVariants((prev) => prev.map((item, i) => (i === idx ? { ...item, colour: val } : item)));
                        }}
                        style={{ padding: '6px 8px', fontSize: '12px', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}
                      />
                      <input
                        type="text"
                        placeholder="SKU Code"
                        value={v.sku}
                        onChange={(e) => {
                          const val = e.target.value.toUpperCase();
                          setVariants((prev) => prev.map((item, i) => (i === idx ? { ...item, sku: val } : item)));
                        }}
                        style={{ padding: '6px 8px', fontSize: '12px', fontFamily: 'monospace', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}
                      />
                      {variants.length > 1 && (
                        <button
                          type="button"
                          onClick={() => handleRemoveVariant(idx)}
                          style={{ color: 'var(--error)', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 700 }}
                        >
                          &times;
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', borderTop: '1px solid var(--line)', paddingTop: '16px' }}>
                <Button variant="outline" type="button" onClick={() => setIsCreatingDraft(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingDraft}>
                  Save Garment Draft
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Propose Revision Modal (Strictly Zero Finance!) */}
      {revisionProduct && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '520px',
              width: '100%',
              padding: '28px',
              border: '1px solid var(--line)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Propose Revision — {revisionProduct.title}
              </h2>
              <button
                onClick={() => setRevisionProduct(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <p style={{ fontSize: '12px', color: 'var(--ink-muted)', margin: '0 0 16px' }}>
              Submit content changes for Owner review. A diff will be generated comparing live catalog values with proposed changes.
            </p>

            <form onSubmit={handleSubmitRevisionForm}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Updated Title
                </label>
                <input
                  type="text"
                  value={revisedTitle}
                  onChange={(e) => setRevisedTitle(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Updated Fabrication
                </label>
                <input
                  type="text"
                  value={revisedFabrication}
                  onChange={(e) => setRevisedFabrication(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Updated Occasion
                </label>
                <input
                  type="text"
                  value={revisedOccasion}
                  onChange={(e) => setRevisedOccasion(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Care Instructions
                </label>
                <input
                  type="text"
                  placeholder="e.g. Dry Clean Only. Avoid direct sunlight."
                  value={revisedCareInstructions}
                  onChange={(e) => setRevisedCareInstructions(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setRevisionProduct(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingRevision}>
                  Submit for Owner Review
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
