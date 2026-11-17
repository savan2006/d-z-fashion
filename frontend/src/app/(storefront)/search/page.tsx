'use client';

import React, { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { Search as SearchIcon, ArrowLeft, X, Sparkles } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { Pagination } from '@/components/ui/Pagination';
import { ProductCard } from '@/components/storefront/ProductCard';
import { ProductSummary, catalogApi } from '@/lib/api/catalog';

function SearchPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryParam = searchParams.get('q') || '';

  const [query, setQuery] = useState(queryParam);
  const [prevQueryParam, setPrevQueryParam] = useState(queryParam);
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Sync state with URL prop during render if queryParam changes
  if (queryParam !== prevQueryParam) {
    setPrevQueryParam(queryParam);
    setQuery(queryParam);
  }

  useEffect(() => {
    let isMounted = true;
    if (!queryParam.trim()) {
      return;
    }

    catalogApi
      .getProducts({
        search: queryParam.trim(),
        page: currentPage,
        pageSize: 12,
      })
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
        const msg = err instanceof Error ? err.message : 'Search service temporarily unavailable';
        setError(msg);
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [queryParam, currentPage]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      setLoading(true);
      setCurrentPage(0);
      router.push(`/search?q=${encodeURIComponent(query.trim())}`);
    }
  };

  const handlePageChange = (newPage: number) => {
    setLoading(true);
    setCurrentPage(newPage - 1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div
      style={{
        maxWidth: 'var(--container-max)',
        margin: '0 auto',
        padding: '40px clamp(20px, 4vw, 64px) 80px',
      }}
    >
      <Link
        href="/"
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          fontSize: '12px',
          color: 'var(--ink-muted)',
          marginBottom: '20px',
          textDecoration: 'none',
        }}
      >
        <ArrowLeft size={14} />
        <span>Return to Store</span>
      </Link>

      <div style={{ maxWidth: '680px', marginBottom: '40px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <Badge variant="wine" icon={<Sparkles size={12} />}>
            Search Surat Atelier
          </Badge>
        </div>

        <h1
          className="font-serif"
          style={{
            margin: '0 0 16px',
            fontSize: 'var(--text-display-md)',
            fontWeight: 400,
          }}
        >
          Discover Garments &amp; Fabrics
        </h1>

        <form onSubmit={handleSubmit} style={{ position: 'relative' }}>
          <div style={{ display: 'flex', gap: '8px' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search by garment title, Surat silk, rayon, occasion..."
                className="form-input"
                style={{
                  height: '48px',
                  paddingLeft: '44px',
                  paddingRight: query ? '40px' : '16px',
                  fontSize: '15px',
                }}
                autoFocus
              />
              <SearchIcon
                size={20}
                color="var(--ink-subtle)"
                style={{ position: 'absolute', left: '14px', top: '14px' }}
              />
              {query && (
                <button
                  type="button"
                  onClick={() => {
                    setQuery('');
                    setProducts([]);
                  }}
                  className="btn btn-ghost"
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '10px',
                    padding: '4px',
                    minHeight: 'auto',
                  }}
                  aria-label="Clear query"
                >
                  <X size={18} />
                </button>
              )}
            </div>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ padding: '0 24px', height: '48px', fontSize: '14px' }}
            >
              Search
            </button>
          </div>
        </form>

        {queryParam && !loading && (
          <p style={{ marginTop: '12px', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Found {totalElements} results for &ldquo;<strong>{queryParam}</strong>&rdquo;
          </p>
        )}
      </div>

      {/* Results Area */}
      {loading ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
            gap: '24px',
          }}
        >
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} height="360px" />
          ))}
        </div>
      ) : error ? (
        <ErrorState
          title="Search encountered an error"
          message={error}
          onRetry={() => {
            setLoading(true);
            setCurrentPage((p) => p);
          }}
        />
      ) : products.length > 0 ? (
        <div>
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
              currentPage={currentPage + 1}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          )}
        </div>
      ) : queryParam ? (
        <EmptyState
          title="No garments matched your search"
          description={`We couldn't find any clothing matching "${queryParam}". Try checking for spelling errors or searching by fabric like "silk", "cotton", or "festive".`}
          action={
            <Link href="/collections" className="btn btn-primary btn-sm">
              Browse All Collections
            </Link>
          }
        />
      ) : (
        <div
          style={{
            padding: '36px',
            backgroundColor: 'var(--paper-warm)',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--line)',
            textAlign: 'center',
          }}
        >
          <p style={{ margin: '0 0 16px', color: 'var(--ink-muted)', fontSize: '14px' }}>
            Popular searches:
          </p>
          <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', flexWrap: 'wrap' }}>
            {['Festive Kurtas', 'Cotton Co-ords', 'Surat Silk', 'Wedding Edit', 'Dresses'].map(
              (term) => (
                <button
                  key={term}
                  type="button"
                  onClick={() => {
                    setQuery(term);
                    setLoading(true);
                    router.push(`/search?q=${encodeURIComponent(term)}`);
                  }}
                  className="btn btn-secondary btn-sm"
                  style={{ borderRadius: 'var(--radius-pill)', padding: '6px 14px' }}
                >
                  {term}
                </button>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div style={{ maxWidth: 'var(--container-max)', margin: '0 auto', padding: '40px' }}>
          <Skeleton height="100px" style={{ marginBottom: '24px' }} />
          <Skeleton height="300px" />
        </div>
      }
    >
      <SearchPageContent />
    </Suspense>
  );
}
