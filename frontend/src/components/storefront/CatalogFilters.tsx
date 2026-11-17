'use client';

import React from 'react';
import { Category, ProductFilterParams } from '@/lib/api/catalog';
import { Select, SelectOption } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { RotateCcw, Filter } from 'lucide-react';

export interface CatalogFiltersProps {
  categories: Category[];
  filters: ProductFilterParams;
  onFilterChange: (updated: Partial<ProductFilterParams>) => void;
  onReset: () => void;
  totalResults?: number;
}

const SIZES = ['XS', 'S', 'M', 'L', 'XL', 'XXL'];

const COLOURS = [
  'Red',
  'Pink',
  'Blue',
  'Green',
  'Maroon',
  'Navy',
  'Black',
  'Gold',
  'Ivory',
  'Wine',
  'Mustard',
];

const FABRICS = [
  'Cotton',
  'Silk',
  'Rayon',
  'Chanderi',
  'Georgette',
  'Organza',
  'Velvet',
  'Linen',
];

const OCCASIONS = [
  'Festive',
  'Daily Wear',
  'Wedding',
  'Office',
  'Casual',
  'Party',
];

const SORT_OPTIONS: SelectOption[] = [
  { value: 'createdAt,desc', label: 'Newest Arrivals' },
  { value: 'basePrice,asc', label: 'Price: Low to High' },
  { value: 'basePrice,desc', label: 'Price: High to Low' },
  { value: 'name,asc', label: 'Name: A to Z' },
];

const PRICE_RANGES = [
  { label: 'All Prices', min: undefined, max: undefined },
  { label: 'Under ₹1,500', min: undefined, max: 1500 },
  { label: '₹1,500 – ₹3,000', min: 1500, max: 3000 },
  { label: '₹3,000 – ₹5,000', min: 3000, max: 5000 },
  { label: 'Over ₹5,000', min: 5000, max: undefined },
];

export const CatalogFilters: React.FC<CatalogFiltersProps> = ({
  categories,
  filters,
  onFilterChange,
  onReset,
  totalResults,
}) => {
  // Category dropdown options
  const categoryOptions: SelectOption[] = [
    { value: '', label: 'All Garment Categories' },
    ...categories.map((c) => ({
      value: c.slug,
      label: c.name,
    })),
  ];

  // Helper to count active filters
  const activeCount = [
    filters.categorySlug,
    filters.minPrice !== undefined || filters.maxPrice !== undefined,
    filters.size,
    filters.colour,
    filters.fabric,
    filters.occasion,
    filters.search,
  ].filter(Boolean).length;

  return (
    <aside
      className="catalog-filters"
      aria-label="Filter collection"
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '24px',
        padding: '24px',
        backgroundColor: '#ffffff',
        border: '1px solid var(--line)',
        borderRadius: 'var(--radius-sm)',
      }}
    >
      {/* Filter Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid var(--line)',
          paddingBottom: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter size={16} color="var(--wine)" />
          <h2
            className="font-serif"
            style={{ margin: 0, fontSize: 'var(--text-heading-sm)', fontWeight: 500 }}
          >
            Refine Catalog
          </h2>
          {activeCount > 0 && (
            <span
              style={{
                fontSize: '11px',
                backgroundColor: 'var(--paper-deep)',
                color: 'var(--ink)',
                padding: '2px 6px',
                borderRadius: '10px',
                fontWeight: 600,
              }}
            >
              {activeCount}
            </span>
          )}
        </div>

        {activeCount > 0 && (
          <button
            type="button"
            onClick={onReset}
            className="btn btn-ghost"
            style={{
              padding: '4px 8px',
              minHeight: 'auto',
              fontSize: '11px',
              color: 'var(--wine)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <RotateCcw size={12} />
            <span>Reset</span>
          </button>
        )}
      </div>

      {totalResults !== undefined && (
        <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)' }}>
          Showing {totalResults} {totalResults === 1 ? 'garment' : 'garments'}
        </p>
      )}

      {/* Sort By */}
      <div>
        <label
          htmlFor="catalog-sort"
          className="eyebrow"
          style={{ display: 'block', marginBottom: '8px' }}
        >
          Sort Order
        </label>
        <Select
          id="catalog-sort"
          options={SORT_OPTIONS}
          value={filters.sort || 'createdAt,desc'}
          onChange={(e) => onFilterChange({ sort: e.target.value, page: 0 })}
        />
      </div>

      {/* Categories */}
      {categories.length > 0 && (
        <div>
          <label
            htmlFor="catalog-category"
            className="eyebrow"
            style={{ display: 'block', marginBottom: '8px' }}
          >
            Category
          </label>
          <Select
            id="catalog-category"
            options={categoryOptions}
            value={filters.categorySlug || ''}
            onChange={(e) =>
              onFilterChange({
                categorySlug: e.target.value || undefined,
                page: 0,
              })
            }
          />
        </div>
      )}

      {/* Price Range */}
      <div>
        <span className="eyebrow" style={{ display: 'block', marginBottom: '10px' }}>
          Price Range (Pre-GST)
        </span>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {PRICE_RANGES.map((range, idx) => {
            const isSelected =
              filters.minPrice === range.min && filters.maxPrice === range.max;
            return (
              <button
                key={idx}
                type="button"
                onClick={() =>
                  onFilterChange({
                    minPrice: range.min,
                    maxPrice: range.max,
                    page: 0,
                  })
                }
                style={{
                  textAlign: 'left',
                  padding: '6px 10px',
                  fontSize: '13px',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: isSelected ? 'var(--paper-deep)' : 'transparent',
                  color: isSelected ? 'var(--wine)' : 'var(--ink)',
                  fontWeight: isSelected ? 600 : 400,
                  border: isSelected ? '1px solid var(--rose)' : '1px solid transparent',
                  cursor: 'pointer',
                  transition: 'background-color var(--transition-fast)',
                }}
              >
                {range.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Sizes */}
      <div>
        <span className="eyebrow" style={{ display: 'block', marginBottom: '8px' }}>
          Size
        </span>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
          {SIZES.map((s) => {
            const isSelected = filters.size === s;
            return (
              <button
                key={s}
                type="button"
                onClick={() =>
                  onFilterChange({
                    size: isSelected ? undefined : s,
                    page: 0,
                  })
                }
                style={{
                  minWidth: '40px',
                  padding: '6px 10px',
                  fontSize: '12px',
                  fontWeight: isSelected ? 600 : 400,
                  backgroundColor: isSelected ? 'var(--wine)' : '#ffffff',
                  color: isSelected ? '#ffffff' : 'var(--ink)',
                  border: `1px solid ${isSelected ? 'var(--wine)' : 'var(--line)'}`,
                  borderRadius: 'var(--radius-sm)',
                  cursor: 'pointer',
                }}
              >
                {s}
              </button>
            );
          })}
        </div>
      </div>

      {/* Colours */}
      <div>
        <label
          htmlFor="catalog-colour"
          className="eyebrow"
          style={{ display: 'block', marginBottom: '8px' }}
        >
          Colour Palette
        </label>
        <Select
          id="catalog-colour"
          options={[
            { value: '', label: 'All Colours' },
            ...COLOURS.map((c) => ({ value: c, label: c })),
          ]}
          value={filters.colour || ''}
          onChange={(e) =>
            onFilterChange({
              colour: e.target.value || undefined,
              page: 0,
            })
          }
        />
      </div>

      {/* Fabric */}
      <div>
        <label
          htmlFor="catalog-fabric"
          className="eyebrow"
          style={{ display: 'block', marginBottom: '8px' }}
        >
          Fabric &amp; Weave
        </label>
        <Select
          id="catalog-fabric"
          options={[
            { value: '', label: 'All Fabrics' },
            ...FABRICS.map((f) => ({ value: f, label: f })),
          ]}
          value={filters.fabric || ''}
          onChange={(e) =>
            onFilterChange({
              fabric: e.target.value || undefined,
              page: 0,
            })
          }
        />
      </div>

      {/* Occasion */}
      <div>
        <label
          htmlFor="catalog-occasion"
          className="eyebrow"
          style={{ display: 'block', marginBottom: '8px' }}
        >
          Occasion
        </label>
        <Select
          id="catalog-occasion"
          options={[
            { value: '', label: 'All Occasions' },
            ...OCCASIONS.map((o) => ({ value: o, label: o })),
          ]}
          value={filters.occasion || ''}
          onChange={(e) =>
            onFilterChange({
              occasion: e.target.value || undefined,
              page: 0,
            })
          }
        />
      </div>

      {/* Mobile/Sidebar Apply / Reset actions */}
      <div style={{ paddingTop: '8px', borderTop: '1px solid var(--line)', display: 'flex', gap: '8px' }}>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onReset}
          style={{ flex: 1 }}
        >
          Clear All
        </Button>
      </div>
    </aside>
  );
};
