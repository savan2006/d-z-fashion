'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Sparkles,
  Mail,
  Lock,
  ShoppingBag,
} from 'lucide-react';
import { BrandLogo } from '@/components/brand/BrandLogo';
import { EditorialPlaceholder } from '@/components/brand/EditorialPlaceholder';
import { Button, LinkButton } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { TextField } from '@/components/ui/TextField';
import { Select } from '@/components/ui/Select';
import { Checkbox } from '@/components/ui/Checkbox';
import { Dialog } from '@/components/ui/Dialog';
import { useToast } from '@/components/ui/Toast';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/Table';
import { Pagination } from '@/components/ui/Pagination';
import { EmptyState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import { ErrorState } from '@/components/ui/ErrorState';

export default function DesignSystemPage() {
  const { toast } = useToast();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(2);
  const [checkboxVal, setCheckboxVal] = useState(true);

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--paper)', color: 'var(--ink)' }}>
      {/* Top Banner */}
      <div style={{ backgroundColor: 'var(--wine)', color: '#fffaf4', padding: '10px 24px', fontSize: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>D &amp; Z FASHION &bull; LIVING DESIGN SYSTEM &bull; COMPONENT 11</span>
        <Link href="/" style={{ color: '#fffaf4', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
          <span>Return to Store</span> &rarr;
        </Link>
      </div>

      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '48px 24px' }}>
        {/* Header */}
        <div style={{ marginBottom: '48px', borderBottom: '1px solid var(--line)', paddingBottom: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
            <Badge variant="wine" icon={<Sparkles size={12} />}>Editorial Brand Identity</Badge>
          </div>
          <h1 className="font-serif" style={{ margin: '0 0 12px', fontSize: 'var(--text-display-lg)', fontWeight: 400 }}>
            D &amp; Z Fashion Design System
          </h1>
          <p style={{ margin: 0, fontSize: 'var(--text-body-lg)', color: 'var(--ink-muted)', maxWidth: '720px', lineHeight: 1.6 }}>
            A cohesive, accessible, and restrained design foundation for women’s and girls’ fashion.
            Unifies the public storefront, Owner administration, and Surat warehouse operational shells.
          </p>

          <div style={{ display: 'flex', gap: '12px', marginTop: '24px', flexWrap: 'wrap' }}>
            <LinkButton href="/" variant="primary" size="sm">
              Public Storefront (/)
            </LinkButton>
            <LinkButton href="/admin" variant="secondary" size="sm">
              Owner Dashboard (/admin)
            </LinkButton>
            <LinkButton href="/manager" variant="secondary" size="sm">
              Manager Dashboard (/manager)
            </LinkButton>
          </div>
        </div>

        {/* Section 1: Brand Assets & Placeholders */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">01 &bull; Identity &amp; Editorial Artwork</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Brand Assets &amp; Placeholders
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px', marginBottom: '24px' }}>
            <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
              <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase', marginBottom: '16px' }}>
                Original SVG Wordmark
              </div>
              <div style={{ padding: '20px', backgroundColor: 'var(--paper-warm)', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-sm)', marginBottom: '12px' }}>
                <BrandLogo variant="wordmark" width={220} height={70} asLink={false} />
              </div>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)' }}>
                Authoritative brand wordmark with Surat elegance. Custom vector typography.
              </p>
            </div>

            <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
              <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase', marginBottom: '16px' }}>
                Original SVG Monogram
              </div>
              <div style={{ padding: '20px', backgroundColor: 'var(--paper-warm)', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-sm)', marginBottom: '12px' }}>
                <BrandLogo variant="monogram" width={70} height={70} asLink={false} />
              </div>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)' }}>
                Favicon and compact application emblem with intertwined D &amp; Z geometry.
              </p>
            </div>
          </div>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--ink-subtle)', textTransform: 'uppercase', marginBottom: '16px' }}>
              Editorial Campaign Artwork Placeholder
            </div>
            <EditorialPlaceholder
              index="D&Z / EDITORIAL-SPECIMEN"
              caption="RESTRAINED SILHOUETTES &bull; SURAT ATELIER"
              minHeight="360px"
              showNotice={true}
            />
          </div>
        </section>

        {/* Section 2: Color Palette */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">02 &bull; Color Tokens</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Editorial Palette &amp; Semantics
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--paper)', borderBottom: '1px solid var(--line)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Paper / Ivory</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#fcfbf8 &bull; --paper</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--paper-deep)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Paper Deep</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#eee8df &bull; --paper-deep</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--ink)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Ink / Charcoal</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#24211e &bull; --ink</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--wine)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Wine / Burgundy</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#6d2836 &bull; --wine</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--rose)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Muted Rose</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#c48b9f &bull; --rose</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--stone)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Warm Stone</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#8a8278 &bull; --stone</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--color-success)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Success</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#1b7340 &bull; --success</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--color-warning)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Warning</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#b45309 &bull; --warning</div>
              </div>
            </div>

            <div style={{ border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', overflow: 'hidden' }}>
              <div style={{ height: '70px', backgroundColor: 'var(--color-error)' }} />
              <div style={{ padding: '12px', backgroundColor: '#ffffff' }}>
                <div style={{ fontWeight: 600, fontSize: '12px' }}>Error / Alert</div>
                <div style={{ fontSize: '11px', color: 'var(--ink-subtle)', fontFamily: 'var(--font-mono)' }}>#b91c1c &bull; --error</div>
              </div>
            </div>
          </div>
        </section>

        {/* Section 3: Typography System */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">03 &bull; Typography Scale</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Editorial Serif &amp; Clean Sans
          </h2>

          <div style={{ padding: '32px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Display XL &bull; Playfair Display</span>
              <div className="font-serif" style={{ fontSize: 'var(--text-display-xl)', lineHeight: 1.05 }}>
                Thoughtful Everyday Style
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Display MD &bull; Playfair Display</span>
              <div className="font-serif" style={{ fontSize: 'var(--text-display-md)', lineHeight: 1.15 }}>
                Surat Textile Heritage &amp; Craft
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Heading LG &bull; Playfair Display</span>
              <div className="font-serif" style={{ fontSize: 'var(--text-heading-lg)' }}>
                Curated Garments for Women and Girls
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Body LG &bull; System Sans</span>
              <div style={{ fontSize: 'var(--text-body-lg)', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
                Each garment is designed with balance, sampled in our Surat atelier, and inspected prior to dispatch.
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--ink-subtle)' }}>Body SM &amp; Eyebrow &bull; Uppercase Tracking</span>
              <div className="eyebrow">A Considered Wardrobe &bull; Pan-India Delivery</div>
            </div>
          </div>
        </section>

        {/* Section 4: Buttons & Interactions */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">04 &bull; Buttons &amp; Action States</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Button Primitives
          </h2>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', marginBottom: '20px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', marginBottom: '16px' }}>Variants</div>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
              <Button variant="primary">Primary Wine</Button>
              <Button variant="secondary">Secondary Paper</Button>
              <Button variant="outline">Outline</Button>
              <Button variant="ghost">Ghost</Button>
              <Button variant="destructive">Destructive</Button>
            </div>
          </div>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', marginBottom: '20px' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', marginBottom: '16px' }}>Sizes</div>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
              <Button size="sm">Small (32px)</Button>
              <Button size="md">Medium (40px)</Button>
              <Button size="lg">Large (48px)</Button>
            </div>
          </div>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', marginBottom: '16px' }}>States &amp; Icons</div>
            <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
              <Button isLoading={true}>Processing</Button>
              <Button disabled={true}>Disabled State</Button>
              <Button startIcon={<ShoppingBag size={16} />}>With Start Icon</Button>
              <Button variant="outline" endIcon={<ArrowLeft size={16} style={{ transform: 'rotate(180deg)' }} />}>
                With End Icon
              </Button>
            </div>
          </div>
        </section>

        {/* Section 5: Form Controls */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">05 &bull; Form Fields &amp; Inputs</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Form Primitives
          </h2>

          <div style={{ padding: '28px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
              <TextField
                label="Full Name"
                placeholder="Priya Patel"
                required={true}
                helperText="Enter your name as it appears on official records"
              />

              <TextField
                label="Email Address"
                placeholder="priya@example.com"
                startIcon={<Mail size={16} />}
                required={true}
              />

              <TextField
                label="Password Field (With Error)"
                placeholder="••••••••"
                startIcon={<Lock size={16} />}
                error="Password must be at least 8 characters with one number"
                defaultValue="secret"
              />

              <Select
                label="Garment Category"
                required={true}
                placeholder="Select category..."
                options={[
                  { value: 'kurti', label: 'Anarkali & Straight Kurtis' },
                  { value: 'dress', label: 'Midi & Maxi Dresses' },
                  { value: 'coords', label: 'Co-ord Sets' },
                  { value: 'cotton', label: 'Everyday Cotton Essentials' },
                ]}
                helperText="Dynamic categories managed by Owner"
              />
            </div>

            <div style={{ marginTop: '16px', borderTop: '1px solid var(--line-subtle)', paddingTop: '20px' }}>
              <Checkbox
                label="Opt-in to SMS and WhatsApp parcel tracking dispatch alerts"
                description="Receive instant shipment updates directly from our Surat warehouse."
                checked={checkboxVal}
                onChange={(e) => setCheckboxVal(e.target.checked)}
              />
              <Checkbox
                label="Disabled terms acceptance checkbox"
                description="Terms are locked during checkout calculation."
                disabled={true}
                checked={true}
              />
            </div>
          </div>
        </section>

        {/* Section 6: Badges & Statuses */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">06 &bull; Badges &amp; Status Indicators</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Badges
          </h2>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <Badge variant="neutral">Neutral: Draft</Badge>
            <Badge variant="wine">Wine: Owner Approved</Badge>
            <Badge variant="success">Success: Delivered</Badge>
            <Badge variant="warning">Warning: Low Stock</Badge>
            <Badge variant="error">Error: Damaged Quarantine</Badge>
            <Badge variant="stone">Stone: Surat Ready</Badge>
          </div>
        </section>

        {/* Section 7: Feedback, Toasts & Dialogs */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">07 &bull; Overlays &amp; Ephemeral Feedback</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Dialog &amp; Toast System
          </h2>

          <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
              <Button
                variant="primary"
                onClick={() => setDialogOpen(true)}
              >
                Open Accessible Modal Dialog
              </Button>

              <Button
                variant="outline"
                onClick={() =>
                  toast({
                    type: 'success',
                    title: 'Stock Reserved Successfully',
                    message: 'Surat warehouse physical inventory atomically locked for checkout.',
                  })
                }
              >
                Trigger Success Toast
              </Button>

              <Button
                variant="outline"
                onClick={() =>
                  toast({
                    type: 'warning',
                    title: 'Price Shift Detected',
                    message: 'Kurtis item price updated. Confirmation required before payment.',
                  })
                }
              >
                Trigger Warning Toast
              </Button>

              <Button
                variant="outline"
                onClick={() =>
                  toast({
                    type: 'error',
                    title: 'Delivery PIN Code Not Serviceable',
                    message: 'COD is restricted for this postal code under Owner policy.',
                  })
                }
              >
                Trigger Error Toast
              </Button>
            </div>
          </div>

          {/* Interactive Dialog Demo */}
          <Dialog
            isOpen={dialogOpen}
            onClose={() => setDialogOpen(false)}
            title="Approve Garment Revision"
            description="Owner authorization checkpoint for catalogue modifications."
            actions={
              <>
                <Button variant="ghost" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  onClick={() => {
                    setDialogOpen(false);
                    toast({
                      type: 'success',
                      title: 'Revision Approved',
                      message: 'Product listing is now published to live storefront.',
                    });
                  }}
                >
                  Confirm Publication
                </Button>
              </>
            }
          >
            <p style={{ fontSize: '13px', color: 'var(--ink-muted)', lineHeight: 1.6 }}>
              You are approving Manager revision for <strong style={{ color: 'var(--ink)' }}>Cotton Straight Kurti (SKU: KURTI-COT-01)</strong>.
              The base price will remain ₹1,499. The item will become immediately visible in the public catalog.
            </p>
          </Dialog>
        </section>

        {/* Section 8: Skeletons, Empty & Error States */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">08 &bull; Loading, Empty &amp; Error Foundations</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            State Primitives
          </h2>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
            {/* Shimmer Skeletons */}
            <div style={{ padding: '24px', backgroundColor: '#ffffff', border: '1px solid var(--line)', borderRadius: 'var(--radius-sm)' }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink-subtle)', marginBottom: '16px' }}>
                Shimmer Loading Skeletons
              </div>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '16px' }}>
                <Skeleton variant="circle" width={44} height={44} />
                <div style={{ flex: 1 }}>
                  <Skeleton variant="text" width="60%" height={14} style={{ marginBottom: '6px' }} />
                  <Skeleton variant="text" width="40%" height={12} />
                </div>
              </div>
              <Skeleton variant="rect" width="100%" height={120} style={{ marginBottom: '12px' }} />
              <Skeleton variant="text" width="80%" height={14} />
            </div>

            {/* Error State */}
            <div>
              <ErrorState
                title="Service Communication Interrupted"
                message="Unable to reach inventory service gateway. Idempotent retry is available."
                onRetry={() => {
                  toast({
                    type: 'info',
                    title: 'Retrying Connection',
                    message: 'Contacting Surat warehouse node with correlation ID...',
                  });
                }}
              />
            </div>
          </div>

          <div style={{ marginTop: '24px' }}>
            <EmptyState
              title="No Claimed Orders in Personal Queue"
              description="Claim new unassigned orders from the Surat warehouse morning queue to begin physical picking."
              action={<Button size="sm" variant="primary">View Shared Queue</Button>}
            />
          </div>
        </section>

        {/* Section 9: Table & Pagination */}
        <section style={{ marginBottom: '64px' }}>
          <p className="eyebrow">09 &bull; Data Table &amp; Pagination</p>
          <h2 className="font-serif" style={{ margin: '0 0 24px', fontSize: 'var(--text-heading-xl)' }}>
            Responsive Table Specimen
          </h2>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>SKU / Variant</TableHead>
                <TableHead>Garment Name</TableHead>
                <TableHead>Size / Color</TableHead>
                <TableHead>Ready Stock</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow>
                <TableCell style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>KURTI-ANARK-S</TableCell>
                <TableCell>Anarkali Silk Kurti</TableCell>
                <TableCell>S / Wine</TableCell>
                <TableCell>24 units</TableCell>
                <TableCell><Badge variant="success">In Stock</Badge></TableCell>
              </TableRow>
              <TableRow>
                <TableCell style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>DRESS-FLOR-M</TableCell>
                <TableCell>Floral Tiered Midi Dress</TableCell>
                <TableCell>M / Rose</TableCell>
                <TableCell>3 units</TableCell>
                <TableCell><Badge variant="warning">Low Stock</Badge></TableCell>
              </TableRow>
              <TableRow>
                <TableCell style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>COORD-LINEN-L</TableCell>
                <TableCell>Linen Relaxed Co-ord</TableCell>
                <TableCell>L / Natural Stone</TableCell>
                <TableCell>0 units</TableCell>
                <TableCell><Badge variant="error">Sold Out</Badge></TableCell>
              </TableRow>
            </TableBody>
          </Table>

          <Pagination
            currentPage={currentPage}
            totalPages={5}
            onPageChange={(p) => setCurrentPage(p)}
          />
        </section>
      </div>
    </div>
  );
}
