'use client';

import React, { Suspense, useEffect, useState, useCallback, useTransition } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { ArrowLeft, Sparkles, SlidersHorizontal, Search as SearchIcon, X } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Pagination } from '@/components/ui/Pagination';
import { ProductCard } from '@/components/storefront/ProductCard';
import { CatalogFilters } from '@/components/storefront/CatalogFilters';
import { Category, ProductSummary, ProductFilterParams, catalogApi } from '@/lib/api/catalog';

function CollectionsContent() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [, startTransition] = useTransition();

  // Parse filters from URL
  const initialCategory = searchParams.get('category') || searchParams.get('cat') || undefined;
  const initialSearch = searchParams.get('q') || searchParams.get('search') || undefined;
  const initialMinPrice = searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined;
  const initialMaxPrice = searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined;
  const initialSize = searchParams.get('size') || undefined;
  const initialColour = searchParams.get('colour') || undefined;
  const initialFabric = searchParams.get('fabric') || undefined;
  const initialOccasion = searchParams.get('occasion') || undefined;
  const initialSort = searchParams.get('sort') || 'createdAt,desc';
  const initialPage = searchParams.get('page') ? Number(searchParams.get('page')) : 0;

  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  // Local search query input for debouncing
  const [searchInput, setSearchInput] = useState(initialSearch || '');

  // Active filter state
  const [filters, setFilters] = useState<ProductFilterParams>({
    categorySlug: initialCategory,
    search: initialSearch,
    minPrice: initialMinPrice,
    maxPrice: initialMaxPrice,
    size: initialSize,
    colour: initialColour,
    fabric: initialFabric,
    occasion: initialOccasion,
    sort: initialSort,
    page: initialPage,
    pageSize: 12,
  });

  // Sync state changes to URL query parameters
  const updateUrl = useCallback(
    (newFilters: ProductFilterParams) => {
      const params = new URLSearchParams();
      if (newFilters.categorySlug) params.set('category', newFilters.categorySlug);
      if (newFilters.search) params.set('q', newFilters.search);
      if (newFilters.minPrice !== undefined) params.set('minPrice', newFilters.minPrice.toString());
      if (newFilters.maxPrice !== undefined) params.set('maxPrice', newFilters.maxPrice.toString());
      if (newFilters.size) params.set('size', newFilters.size);
      if (newFilters.colour) params.set('colour', newFilters.colour);
      if (newFilters.fabric) params.set('fabric', newFilters.fabric);
      if (newFilters.occasion) params.set('occasion', newFilters.occasion);
      if (newFilters.sort && newFilters.sort !== 'createdAt,desc') params.set('sort', newFilters.sort);
      if (newFilters.page && newFilters.page > 0) params.set('page', newFilters.page.toString());

      startTransition(() => {
        router.push(`${pathname}?${params.toString()}`, { scroll: false });
      });
    },
    [pathname, router]
  );

  // Load Categories on Mount
  useEffect(() => {
    let isMounted = true;
    catalogApi
      .getCategories(false)
      .then((res) => {
        if (isMounted && res.data) {
          setCategories(res.data.filter((c) => c.active));
        }
      })
      .catch(() => {
        // Fallback silently if categories offline
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch Products asynchronously when filters change
  useEffect(() => {
    let isMounted = true;

    catalogApi
      .getProducts(filters)
      .then((res) => {
        if (!isMounted) return;
        if (res.data) {
          setProducts(res.data.content || []);
          setTotalElements(res.data.totalElements || 0);
          setTotalPages(res.data.totalPages || 1);
        }
        setError(null);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (!isMounted) return;
        const msg = err instanceof Error ? err.message : 'Unable to connect to product catalog';
        setError(msg);
        setProducts([]);
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [filters]);

  // Handle filter changes
  const handleFilterChange = (updated: Partial<ProductFilterParams>) => {
    setLoading(true);
    const next = { ...filters, ...updated, page: updated.page !== undefined ? updated.page : 0 };
    setFilters(next);
    updateUrl(next);
  };

  // Reset filters
  const handleResetFilters = () => {
    setLoading(true);
    const next: ProductFilterParams = {
      sort: 'createdAt,desc',
      page: 0,
      pageSize: 12,
    };
    setSearchInput('');
    setFilters(next);
    updateUrl(next);
  };

  // Debounced search submit
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleFilterChange({ search: searchInput.trim() || undefined, page: 0 });
  };

  // Find active category title if selected
  const activeCategory = categories.find((c) => c.slug === filters.categorySlug);

  return (
    <div
      style={{
        maxWidth: 'var(--container-max)',
        margin: '0 auto',
        padding: '36px clamp(20px, 4vw, 64px) 64px',
      }}
    >
      {/* Header & Breadcrumb */}
      <div style={{ marginBottom: '32px' }}>
        <Link
          href="/"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            marginBottom: '16px',
            textDecoration: 'none',
          }}
        >
          <ArrowLeft size={14} />
          <span>Back to Storefront</span>
        </Link>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <Badge variant="wine" icon={<Sparkles size={12} />}>
            Surat Garment Workshop
          </Badge>
        </div>

        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div>
            <h1
              className="font-serif"
              style={{
                margin: '0 0 8px',
                fontSize: 'var(--text-display-md)',
                fontWeight: 400,
                letterSpacing: '-0.03em',
              }}
            >
              {activeCategory ? activeCategory.name : 'Curated Garment Collections'}
            </h1>
            <p style={{ margin: 0, color: 'var(--ink-muted)', fontSize: 'var(--text-body-md)', maxWidth: '640px' }}>
              {activeCategory?.description ||
                'Thoughtfully designed women’s and girls’ clothing crafted in Surat with quality ready stock.'}
            </p>
          </div>

          {/* Mobile Filter Toggle */}
          <button
            type="button"
            onClick={() => setMobileFiltersOpen(!mobileFiltersOpen)}
            className="btn btn-secondary btn-sm md:hidden"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
          >
            <SlidersHorizontal size={14} />
            <span>Filters</span>
          </button>
        </div>
      </div>

      {/* Category Pills Bar */}
      {categories.length > 0 && (
        <div
          style={{
            display: 'flex',
            gap: '8px',
            overflowX: 'auto',
            paddingBottom: '12px',
            marginBottom: '24px',
            borderBottom: '1px solid var(--line)',
          }}
          aria-label="Category tabs"
        >
          <button
            type="button"
            onClick={() => handleFilterChange({ categorySlug: undefined, page: 0 })}
            className={`btn btn-sm ${!filters.categorySlug ? 'btn-primary' : 'btn-secondary'}`}
            style={{ borderRadius: 'var(--radius-pill)', padding: '6px 14px', minHeight: 'auto', flexShrink: 0 }}
          >
            All Collections
          </button>
          {categories.map((c) => {
            const isSelected = filters.categorySlug === c.slug;
            return (
              <button
                key={c.id || c.slug}
                type="button"
                onClick={() => handleFilterChange({ categorySlug: c.slug, page: 0 })}
                className={`btn btn-sm ${isSelected ? 'btn-primary' : 'btn-secondary'}`}
                style={{
                  borderRadius: 'var(--radius-pill)',
                  padding: '6px 14px',
                  minHeight: 'auto',
                  flexShrink: 0,
                }}
              >
                {c.name}
              </button>
            );
          })}
        </div>
      )}

      {/* Search Input Bar */}
      <form onSubmit={handleSearchSubmit} style={{ marginBottom: '28px' }}>
        <div
          style={{
            display: 'flex',
            gap: '8px',
            maxWidth: '560px',
            position: 'relative',
          }}
        >
          <div style={{ position: 'relative', flex: 1 }}>
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search garments by name, fabric, style..."
              className="form-input"
              style={{
                paddingLeft: '38px',
                paddingRight: searchInput ? '36px' : '14px',
                height: '42px',
              }}
              aria-label="Search garments"
            />
            <SearchIcon
              size={18}
              color="var(--ink-subtle)"
              style={{ position: 'absolute', left: '12px', top: '12px' }}
            />
            {searchInput && (
              <button
                type="button"
                onClick={() => {
                  setSearchInput('');
                  handleFilterChange({ search: undefined, page: 0 });
                }}
                className="btn btn-ghost"
                style={{
                  position: 'absolute',
                  right: '6px',
                  top: '6px',
                  padding: '4px',
                  minHeight: 'auto',
                }}
                aria-label="Clear search"
              >
                <X size={16} />
              </button>
            )}
          </div>
          <button type="submit" className="btn btn-primary" style={{ padding: '0 20px', height: '42px' }}>
            Search
          </button>
        </div>
      </form>

      {/* Main Two-Column Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(260px, 300px) 1fr',
          gap: '36px',
          alignItems: 'start',
        }}
      >
        {/* Desktop Sidebar Filters */}
        <div className="hidden md:block">
          <CatalogFilters
            categories={categories}
            filters={filters}
            onFilterChange={handleFilterChange}
            onReset={handleResetFilters}
            totalResults={totalElements}
          />
        </div>

        {/* Mobile Filters Drawer / Modal */}
        {mobileFiltersOpen && (
          <div
            className="dialog-backdrop md:hidden"
            onClick={() => setMobileFiltersOpen(false)}
            style={{ zIndex: 100 }}
          >
            <div
              style={{
                width: '85%',
                maxWidth: '360px',
                height: '100vh',
                backgroundColor: '#ffffff',
                overflowY: 'auto',
                padding: '20px',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '16px',
                }}
              >
                <h3 className="font-serif" style={{ margin: 0, fontSize: 'var(--text-heading-md)' }}>
                  Filters &amp; Sort
                </h3>
                <button
                  type="button"
                  onClick={() => setMobileFiltersOpen(false)}
                  className="btn btn-ghost"
                  style={{ padding: '6px', minHeight: 'auto' }}
                  aria-label="Close filters"
                >
                  <X size={20} />
                </button>
              </div>

              <CatalogFilters
                categories={categories}
                filters={filters}
                onFilterChange={(u) => {
                  handleFilterChange(u);
                  setMobileFiltersOpen(false);
                }}
                onReset={() => {
                  handleResetFilters();
                  setMobileFiltersOpen(false);
                }}
                totalResults={totalElements}
              />
            </div>
          </div>
        )}

        {/* Products Grid Area */}
        <div>
          {loading ? (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                gap: '24px',
              }}
            >
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} height="360px" />
              ))}
            </div>
          ) : error ? (
            <ErrorState
              title="Unable to load catalog"
              message={error}
              onRetry={() => {
                setLoading(true);
                setFilters({ ...filters });
              }}
              retryLabel="Reload garments"
            />
          ) : products.length === 0 ? (
            <EmptyState
              title="No garments match your criteria"
              description="Try resetting your filters or browsing another category from our Surat atelier."
              action={
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="btn btn-primary btn-sm"
                >
                  Clear All Filters
                </button>
              }
            />
          ) : (
            <>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                  gap: '24px',
                  marginBottom: '40px',
                }}
              >
                {products.map((product) => (
                  <ProductCard key={product.id || product.slug} product={product} />
                ))}
              </div>

              {totalPages > 1 && (
                <Pagination
                  currentPage={(filters.page || 0) + 1}
                  totalPages={totalPages}
                  onPageChange={(p) => handleFilterChange({ page: p - 1 })}
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function CollectionsPage() {
  return (
    <Suspense
      fallback={
        <div style={{ maxWidth: 'var(--container-max)', margin: '0 auto', padding: '40px' }}>
          <Skeleton height="80px" style={{ marginBottom: '24px' }} />
          <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr', gap: '32px' }}>
            <Skeleton height="400px" />
            <Skeleton height="400px" />
          </div>
        </div>
      }
    >
      <CollectionsContent />
    </Suspense>
  );
}
