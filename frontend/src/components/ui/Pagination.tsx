import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className = '',
}) => {
  if (totalPages <= 1) return null;

  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      pages.push(1);
      if (currentPage > 3) pages.push('...');

      const start = Math.max(2, currentPage - 1);
      const end = Math.min(totalPages - 1, currentPage + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (currentPage < totalPages - 2) pages.push('...');
      pages.push(totalPages);
    }
    return pages;
  };

  return (
    <nav aria-label="Pagination Navigation" className={`flex items-center justify-between gap-4 ${className}`} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', padding: '16px 0' }}>
      <button
        type="button"
        disabled={currentPage <= 1}
        onClick={() => onPageChange(currentPage - 1)}
        className="btn btn-secondary btn-sm"
        aria-label="Previous page"
        style={{ padding: '6px 10px' }}
      >
        <ChevronLeft size={16} />
        <span style={{ fontSize: 'var(--text-body-xs)' }}>Previous</span>
      </button>

      <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
        {getPageNumbers().map((item, index) => {
          if (item === '...') {
            return (
              <span key={`dots-${index}`} style={{ padding: '0 6px', color: 'var(--ink-subtle)' }}>
                ...
              </span>
            );
          }

          const pageNum = Number(item);
          const isActive = pageNum === currentPage;

          return (
            <button
              key={pageNum}
              type="button"
              onClick={() => onPageChange(pageNum)}
              aria-current={isActive ? 'page' : undefined}
              className={`btn btn-sm ${isActive ? 'btn-primary' : 'btn-ghost'}`}
              style={{ minWidth: '32px', padding: '4px 8px' }}
            >
              {pageNum}
            </button>
          );
        })}
      </div>

      <button
        type="button"
        disabled={currentPage >= totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        className="btn btn-secondary btn-sm"
        aria-label="Next page"
        style={{ padding: '6px 10px' }}
      >
        <span style={{ fontSize: 'var(--text-body-xs)' }}>Next</span>
        <ChevronRight size={16} />
      </button>
    </nav>
  );
};
