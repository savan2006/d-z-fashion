import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProductCard } from '@/components/storefront/ProductCard';
import { CatalogFilters } from '@/components/storefront/CatalogFilters';
import { ProductGallery } from '@/components/storefront/ProductGallery';
import { VariantSelector } from '@/components/storefront/VariantSelector';
import { SizeChartModal } from '@/components/storefront/SizeChartModal';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/components/ui/Toast';
import { ProductSummary, ProductVariant, MediaAsset, Category } from '@/lib/api/catalog';

// Mock Next.js navigation
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
  }),
  usePathname: () => '/collections',
  useSearchParams: () => new URLSearchParams(),
}));

// Mock Next.js Image component
vi.mock('next/image', () => ({
  default: ({ fill, priority, ...props }: any) => {
    // eslint-disable-next-line @next/next/no-img-element
    return <img {...props} alt={props.alt || ''} />;
  },
}));

// Mock catalogApi
vi.mock('@/lib/api/catalog', async () => {
  const actual = await vi.importActual<any>('@/lib/api/catalog');
  return {
    ...actual,
    catalogApi: {
      ...actual.catalogApi,
      getVariantStock: vi.fn().mockImplementation((variantId: string) => {
        if (variantId === 'var-soldout') {
          return Promise.resolve({
            data: {
              variantId: 'var-soldout',
              sku: 'SKU-SOLDOUT',
              physicalQty: 0,
              reservedQty: 0,
              availableQty: 0,
              quarantinedQty: 0,
              lowStockThreshold: 3,
            },
            status: 200,
            ok: true,
          });
        }
        return Promise.resolve({
          data: {
            variantId: 'var-instock',
            sku: 'SKU-INSTOCK',
            physicalQty: 10,
            reservedQty: 2,
            availableQty: 8,
            quarantinedQty: 0,
            lowStockThreshold: 3,
          },
          status: 200,
          ok: true,
        });
      }),
    },
  };
});

describe('Component 12 — Public Storefront & Catalog Browsing', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  const mockProduct: ProductSummary = {
    id: 'prod-001',
    categoryName: 'Festive Wear',
    name: 'Surat Chanderi Anarkali Suit',
    slug: 'surat-chanderi-anarkali-suit',
    fabric: 'Pure Chanderi Silk',
    occasion: 'Festive & Wedding',
    defaultPrice: 3850,
    primaryImageUrl: 'https://res.cloudinary.com/dzfashion/image/upload/v1/anarkali.jpg',
    status: 'PUBLISHED',
  };

  const mockCategories: Category[] = [
    { id: 'cat-1', name: 'Indian Wear', slug: 'indian-wear', active: true, displayOrder: 1 },
    { id: 'cat-2', name: 'Western Wear', slug: 'western-wear', active: true, displayOrder: 2 },
    { id: 'cat-3', name: 'Dresses', slug: 'dresses', active: true, displayOrder: 3 },
  ];

  describe('ProductCard Component', () => {
    it('renders garment name, category, fabric badge and formatted INR price with excl. GST disclosure', () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <ProductCard product={mockProduct} />
          </ToastProvider>
        </AuthProvider>
      );

      expect(screen.getByText('Surat Chanderi Anarkali Suit')).toBeDefined();
      expect(screen.getByText('Festive Wear')).toBeDefined();
      expect(screen.getByText('Pure Chanderi Silk')).toBeDefined();
      // Price contains ₹ and 3,850
      expect(screen.getByText(/3,850/)).toBeDefined();
      expect(screen.getByText('excl. GST')).toBeDefined();
    });

    it('enforces unauthenticated user redirection to login when quick bag action is clicked', async () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <ProductCard product={mockProduct} />
          </ToastProvider>
        </AuthProvider>
      );

      const bagButton = screen.getByRole('button', { name: /Select options for/i });
      fireEvent.click(bagButton);

      expect(mockPush).toHaveBeenCalledWith(
        `/login?redirect=${encodeURIComponent('/products/surat-chanderi-anarkali-suit')}`
      );
    });

    it('renders editorial fallback when no product image is uploaded', () => {
      const productWithoutImage: ProductSummary = {
        ...mockProduct,
        primaryImageUrl: null,
      };

      render(
        <AuthProvider>
          <ToastProvider>
            <ProductCard product={productWithoutImage} />
          </ToastProvider>
        </AuthProvider>
      );

      expect(screen.getByText('FESTIVE WEAR • SURAT')).toBeDefined();
    });
  });

  describe('CatalogFilters Component', () => {
    it('renders database-driven category options, prices, sizes, and sorts', () => {
      const onFilterChange = vi.fn();
      const onReset = vi.fn();

      render(
        <CatalogFilters
          categories={mockCategories}
          filters={{ sort: 'createdAt,desc', page: 0 }}
          onFilterChange={onFilterChange}
          onReset={onReset}
          totalResults={24}
        />
      );

      expect(screen.getByText('Refine Catalog')).toBeDefined();
      expect(screen.getByText('Showing 24 garments')).toBeDefined();
      expect(screen.getByText('All Garment Categories')).toBeDefined();
      expect(screen.getByText('Under ₹1,500')).toBeDefined();
      expect(screen.getByText('XS')).toBeDefined();
      expect(screen.getByText('XL')).toBeDefined();
    });

    it('calls onFilterChange when size chip is selected', () => {
      const onFilterChange = vi.fn();
      const onReset = vi.fn();

      render(
        <CatalogFilters
          categories={mockCategories}
          filters={{ sort: 'createdAt,desc', page: 0 }}
          onFilterChange={onFilterChange}
          onReset={onReset}
        />
      );

      const sizeMBtn = screen.getByText('M');
      fireEvent.click(sizeMBtn);

      expect(onFilterChange).toHaveBeenCalledWith({ size: 'M', page: 0 });
    });

    it('calls onReset when Clear All button is clicked', () => {
      const onFilterChange = vi.fn();
      const onReset = vi.fn();

      render(
        <CatalogFilters
          categories={mockCategories}
          filters={{ sort: 'createdAt,desc', size: 'M', page: 0 }}
          onFilterChange={onFilterChange}
          onReset={onReset}
        />
      );

      const resetBtn = screen.getByText('Reset');
      fireEvent.click(resetBtn);

      expect(onReset).toHaveBeenCalled();
    });
  });

  describe('ProductGallery Component', () => {
    const mockMedia: MediaAsset[] = [
      {
        id: 'med-1',
        secureUrl: 'https://res.cloudinary.com/dzfashion/image/upload/v1/front.jpg',
        altText: 'Front View',
        displayOrder: 1,
        primary: true,
      },
      {
        id: 'med-2',
        secureUrl: 'https://res.cloudinary.com/dzfashion/image/upload/v1/back.jpg',
        altText: 'Back View',
        displayOrder: 2,
        primary: false,
      },
      {
        id: 'med-3',
        variantId: 'var-red',
        secureUrl: 'https://res.cloudinary.com/dzfashion/image/upload/v1/red-variant.jpg',
        altText: 'Red Variant Detail',
        displayOrder: 3,
        primary: false,
      },
    ];

    it('renders primary viewer and secondary thumbnails with image counters', () => {
      render(
        <ProductGallery
          media={mockMedia}
          productName="Surat Anarkali"
        />
      );

      expect(screen.getAllByAltText('Front View').length).toBe(2);
      expect(screen.getByText('1 / 3')).toBeDefined();
    });

    it('filters gallery when variantId is selected with variant-specific media', () => {
      render(
        <ProductGallery
          media={mockMedia}
          productName="Surat Anarkali"
          selectedVariantId="var-red"
        />
      );

      expect(screen.getByAltText('Red Variant Detail')).toBeDefined();
    });

    it('renders editorial artwork placeholder when no images are provided', () => {
      render(
        <ProductGallery
          media={[]}
          productName="Surat Anarkali"
        />
      );

      expect(screen.getByText('D&Z / GALLERY')).toBeDefined();
    });
  });

  describe('VariantSelector & Live Inventory Stock', () => {
    const mockVariants: ProductVariant[] = [
      {
        id: 'var-instock',
        sku: 'DZ-ANAR-MAROON-M',
        size: 'M',
        colour: 'Maroon',
        priceOverride: null,
        effectivePrice: 3850,
        active: true,
      },
      {
        id: 'var-soldout',
        sku: 'DZ-ANAR-MAROON-L',
        size: 'L',
        colour: 'Maroon',
        priceOverride: 4100,
        effectivePrice: 4100,
        active: true,
      },
    ];

    it('displays live in-stock badge for available variants', async () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <VariantSelector
              variants={mockVariants}
              defaultPrice={3850}
              productSlug="anarkali"
              productName="Anarkali Suit"
            />
          </ToastProvider>
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByText(/In Ready Stock/i)).toBeDefined();
      });
      const addBtn = screen.getByRole('button', { name: /Add to Shopping Bag/i });
      expect((addBtn as HTMLButtonElement).disabled).toBe(false);
    });

    it('detects sold out variant, displays Sold Out badge, and disables Add to Shopping Bag button', async () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <VariantSelector
              variants={mockVariants}
              defaultPrice={3850}
              productSlug="anarkali"
              productName="Anarkali Suit"
            />
          </ToastProvider>
        </AuthProvider>
      );

      // Select Size L which points to var-soldout
      const sizeLBtn = screen.getByRole('button', { name: /Size L/i });
      fireEvent.click(sizeLBtn);

      await waitFor(() => {
        expect(screen.getByText(/Sold Out — Currently out of stock/i)).toBeDefined();
      });

      const addBtn = screen.getByRole('button', { name: /Sold Out/i });
      expect((addBtn as HTMLButtonElement).disabled).toBe(true);
    });

    it('redirects unauthenticated customers to login when adding an available variant to cart', async () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <VariantSelector
              variants={mockVariants}
              defaultPrice={3850}
              productSlug="anarkali"
              productName="Anarkali Suit"
            />
          </ToastProvider>
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByText(/In Ready Stock/i)).toBeDefined();
      });

      const addBtn = screen.getByRole('button', { name: /Add to Shopping Bag/i });
      fireEvent.click(addBtn);

      expect(mockPush).toHaveBeenCalledWith(
        `/login?redirect=${encodeURIComponent('/products/anarkali')}`
      );
    });

    it('opens SizeChartModal when Size Guide button is clicked', () => {
      render(
        <AuthProvider>
          <ToastProvider>
            <VariantSelector
              variants={mockVariants}
              defaultPrice={3850}
              productSlug="anarkali"
              productName="Anarkali Suit"
              categoryName="Anarkali"
            />
          </ToastProvider>
        </AuthProvider>
      );

      const guideBtn = screen.getByRole('button', { name: /Size Guide/i });
      fireEvent.click(guideBtn);

      expect(screen.getByText('Anarkali Size Guide')).toBeDefined();
      expect(screen.getByText('Centimeters')).toBeDefined();
    });
  });

  describe('SizeChartModal Component', () => {
    it('switches units between inches and centimeters accurately', () => {
      const onClose = vi.fn();
      render(
        <SizeChartModal
          isOpen={true}
          onClose={onClose}
          categoryName="Suit"
        />
      );

      expect(screen.getByText('Suit Size Guide')).toBeDefined();
      expect(screen.getByText('Bust (inches)')).toBeDefined();

      const cmBtn = screen.getByText('Centimeters');
      fireEvent.click(cmBtn);

      expect(screen.getByText('Bust (cm)')).toBeDefined();
    });
  });
});
