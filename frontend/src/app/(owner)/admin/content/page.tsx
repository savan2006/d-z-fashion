'use client';

import React, { useState } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle2,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

interface HeroBanner {
  id: string;
  title: string;
  subtitle: string;
  ctaText: string;
  targetUrl: string;
  imageUrl: string;
  active: boolean;
  displayOrder: number;
}

const INITIAL_BANNERS: HeroBanner[] = [
  {
    id: 'banner-1',
    title: 'Surat Royal Zari Silk Sarees',
    subtitle: 'Handwoven festive heirlooms from heritage Gujarat master looms',
    ctaText: 'Explore Silk Collection',
    targetUrl: '/category/sarees',
    imageUrl: 'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=1200&q=80',
    active: true,
    displayOrder: 1,
  },
  {
    id: 'banner-2',
    title: 'Contemporary Festive Kurtas',
    subtitle: 'Chanderi cotton silhouettes with intricate thread needlework',
    ctaText: 'Shop Kurtas',
    targetUrl: '/category/kurtas',
    imageUrl: 'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=1200&q=80',
    active: true,
    displayOrder: 2,
  },
];

export default function OwnerContentManagementPage() {
  const [banners, setBanners] = useState<HeroBanner[]>(INITIAL_BANNERS);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // New Banner Modal
  const [isAddingBanner, setIsAddingBanner] = useState(false);
  const [title, setTitle] = useState('');
  const [subtitle, setSubtitle] = useState('');
  const [ctaText, setCtaText] = useState('Shop Collection');
  const [targetUrl, setTargetUrl] = useState('/category/sarees');
  const [imageUrl, setImageUrl] = useState('');
  const [displayOrder] = useState(3);

  const handleCreateBanner = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !imageUrl.trim()) return;

    const newBanner: HeroBanner = {
      id: `banner-${Date.now()}`,
      title: title.trim(),
      subtitle: subtitle.trim(),
      ctaText: ctaText.trim(),
      targetUrl: targetUrl.trim(),
      imageUrl: imageUrl.trim(),
      active: true,
      displayOrder,
    };

    setBanners((prev) => [...prev, newBanner]);
    setFeedback({
      type: 'success',
      text: `Storefront hero banner "${newBanner.title}" published successfully.`,
    });

    setIsAddingBanner(false);
    setTitle('');
    setSubtitle('');
    setImageUrl('');
  };

  const handleToggleBanner = (id: string) => {
    setBanners((prev) =>
      prev.map((b) => (b.id === id ? { ...b, active: !b.active } : b))
    );
  };

  const handleDeleteBanner = (id: string) => {
    setBanners((prev) => prev.filter((b) => b.id !== id));
    setFeedback({
      type: 'success',
      text: 'Hero banner removed from active storefront rotation.',
    });
  };

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
            Storefront Banners & Merchandising Media
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Configure promotional hero carousels and featured visual highlights. Media assets are delivered via Cloudinary CDN.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsAddingBanner(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Add Hero Banner
        </Button>
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

      {/* Banners List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {banners.map((banner) => (
          <div
            key={banner.id}
            style={{
              backgroundColor: '#ffffff',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
              padding: '16px 20px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '16px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div
                style={{
                  width: '90px',
                  height: '60px',
                  borderRadius: 'var(--radius-sm)',
                  overflow: 'hidden',
                  backgroundColor: 'var(--paper-warm)',
                  backgroundImage: `url(${banner.imageUrl})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  flexShrink: 0,
                }}
              />
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                  <span style={{ fontWeight: 600, fontSize: '14px', color: 'var(--ink)' }}>{banner.title}</span>
                  <Badge variant={banner.active ? 'success' : 'secondary'} size="sm">
                    {banner.active ? 'Active' : 'Inactive'}
                  </Badge>
                  <span style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>Order: #{banner.displayOrder}</span>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginBottom: '4px' }}>
                  {banner.subtitle}
                </div>
                <div style={{ fontSize: '11px', color: 'var(--wine)' }}>
                  CTA: &ldquo;{banner.ctaText}&rdquo; &rarr; {banner.targetUrl}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleToggleBanner(banner.id)}
              >
                {banner.active ? 'Deactivate' : 'Activate'}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                style={{ color: 'var(--error)' }}
                onClick={() => handleDeleteBanner(banner.id)}
              >
                <Trash2 size={14} />
              </Button>
            </div>
          </div>
        ))}
      </div>

      {/* Add Banner Modal */}
      {isAddingBanner && (
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
                Add Storefront Hero Banner
              </h2>
              <button
                onClick={() => setIsAddingBanner(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateBanner}>
              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Banner Headline *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Surat Organza Silk Celebration"
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

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Subtitle
                </label>
                <input
                  type="text"
                  placeholder="e.g. Handcrafted by Surat weavers with authentic zari work."
                  value={subtitle}
                  onChange={(e) => setSubtitle(e.target.value)}
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
                    Button Call to Action
                  </label>
                  <input
                    type="text"
                    value={ctaText}
                    onChange={(e) => setCtaText(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Target Link
                  </label>
                  <input
                    type="text"
                    value={targetUrl}
                    onChange={(e) => setTargetUrl(e.target.value)}
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

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Cloudinary Image URL *
                </label>
                <input
                  type="url"
                  required
                  placeholder="https://res.cloudinary.com/..."
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
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
                <Button variant="outline" type="button" onClick={() => setIsAddingBanner(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit">
                  Publish Banner
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
