'use client';

import React, { useState } from 'react';
import { Dialog } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { TextField } from '@/components/ui/TextField';
import { useToast } from '@/components/ui/Toast';
import { customerApi } from '@/lib/api/customer';
import { Star, Camera } from 'lucide-react';

export interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  productId: string;
  orderId: string;
  orderItemId: string;
  productName: string;
  onReviewSubmitted?: () => void;
}

export const ReviewModal: React.FC<ReviewModalProps> = ({
  isOpen,
  onClose,
  productId,
  orderId,
  orderItemId,
  productName,
  onReviewSubmitted,
}) => {
  const { toast } = useToast();
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [title, setTitle] = useState('');
  const [comment, setComment] = useState('');
  const [photoUrl, setPhotoUrl] = useState('');
  const [photoUrls, setPhotoUrls] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const handleAddPhoto = () => {
    if (!photoUrl.trim()) return;
    if (photoUrls.includes(photoUrl.trim())) return;
    setPhotoUrls([...photoUrls, photoUrl.trim()]);
    setPhotoUrl('');
  };

  const handleRemovePhoto = (index: number) => {
    setPhotoUrls(photoUrls.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) {
      toast({ type: 'warning', title: 'Review Required', message: 'Please share your thoughts on the product.' });
      return;
    }

    setSubmitting(true);
    try {
      await customerApi.createProductReview(productId, {
        orderId,
        orderItemId,
        rating,
        title: title.trim() || undefined,
        comment: comment.trim(),
        photoUrls: photoUrls.length > 0 ? photoUrls : undefined,
      });

      toast({
        type: 'success',
        title: 'Review Submitted',
        message: 'Thank you for reviewing your verified purchase!',
      });
      if (onReviewSubmitted) onReviewSubmitted();
      onClose();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Failed to submit review. Please try again.';
      toast({ type: 'error', title: 'Submission Failed', message: msg });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title="Write a Verified Purchase Review"
      description={`Share your thoughts on ${productName}`}
    >
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Star Rating Selector */}
        <div>
          <label className="form-label" style={{ display: 'block', marginBottom: '8px' }}>
            Rating <span style={{ color: 'var(--wine)' }}>*</span>
          </label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {[1, 2, 3, 4, 5].map((star) => {
              const active = (hoverRating !== null ? hoverRating : rating) >= star;
              return (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(null)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: '4px',
                    color: active ? '#b8860b' : 'var(--line-strong)',
                    transition: 'transform 0.15s ease',
                  }}
                  aria-label={`${star} star${star > 1 ? 's' : ''}`}
                >
                  <Star size={24} fill={active ? '#b8860b' : 'none'} />
                </button>
              );
            })}
            <span style={{ fontSize: '13px', color: 'var(--ink-muted)', marginLeft: '8px' }}>
              {rating === 5 && 'Outstanding Quality'}
              {rating === 4 && 'Very Good'}
              {rating === 3 && 'Average'}
              {rating === 2 && 'Below Expectations'}
              {rating === 1 && 'Poor'}
            </span>
          </div>
        </div>

        {/* Headline / Title */}
        <TextField
          label="Headline (Optional)"
          placeholder="e.g. Gorgeous Surat silk weave and rich wine colour!"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        {/* Detailed Comments */}
        <div>
          <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
            Your Review <span style={{ color: 'var(--wine)' }}>*</span>
          </label>
          <textarea
            className="form-input"
            rows={4}
            placeholder="Describe the fabric feel, drape, stitching, fit, and your experience..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            required
            style={{ width: '100%', resize: 'vertical' }}
          />
        </div>

        {/* Optional Photos */}
        <div>
          <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
            Add Photo URLs (Optional)
          </label>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
            <input
              type="url"
              className="form-input"
              placeholder="https://example.com/photo.jpg"
              value={photoUrl}
              onChange={(e) => setPhotoUrl(e.target.value)}
              style={{ flex: 1 }}
            />
            <Button type="button" variant="outline" size="sm" onClick={handleAddPhoto}>
              <Camera size={14} style={{ marginRight: '6px' }} />
              Add
            </Button>
          </div>

          {photoUrls.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px' }}>
              {photoUrls.map((url, i) => (
                <div
                  key={i}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '4px 10px',
                    backgroundColor: 'var(--surface-subtle)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '12px',
                  }}
                >
                  <span style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {url}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleRemovePhoto(i)}
                    style={{ background: 'none', border: 'none', color: 'var(--wine)', cursor: 'pointer', padding: 0 }}
                  >
                    &times;
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
          <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={submitting}>
            Submit Review
          </Button>
        </div>
      </form>
    </Dialog>
  );
};
