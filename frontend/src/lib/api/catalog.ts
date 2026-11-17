import { api } from './client';
import { ApiResponse } from './types';

export interface Category {
  id: string;
  parentId?: string | null;
  name: string;
  slug: string;
  description?: string | null;
  active: boolean;
  displayOrder: number;
  children?: Category[];
}

export type ProductStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'PUBLISHED' | 'ARCHIVED';

export interface ProductSummary {
  id: string;
  categoryName: string;
  name: string;
  slug: string;
  fabric?: string | null;
  occasion?: string | null;
  defaultPrice: number;
  primaryImageUrl?: string | null;
  status: ProductStatus;
}

export interface ProductVariant {
  id: string;
  sku: string;
  size: string;
  colour: string;
  priceOverride?: number | null;
  effectivePrice: number;
  active: boolean;
}

export interface MediaAsset {
  id: string;
  variantId?: string | null;
  cloudinaryPublicId?: string;
  secureUrl: string;
  format?: string;
  width?: number | null;
  height?: number | null;
  altText?: string | null;
  displayOrder: number;
  primary: boolean;
}

export interface ProductDetail {
  id: string;
  categoryId: string;
  categoryName: string;
  categorySlug: string;
  name: string;
  slug: string;
  description: string;
  fabric?: string | null;
  occasion?: string | null;
  status: ProductStatus;
  defaultPrice: number;
  variants: ProductVariant[];
  media: MediaAsset[];
  createdAt?: string;
  updatedAt?: string;
}

export interface StockResponse {
  variantId: string;
  sku: string;
  physicalQty: number;
  reservedQty: number;
  availableQty: number;
  quarantinedQty: number;
  lowStockThreshold: number;
  updatedAt?: string;
}

export interface CatalogPageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ProductFilterParams {
  categoryId?: string;
  categorySlug?: string;
  minPrice?: number;
  maxPrice?: number;
  size?: string;
  colour?: string;
  fabric?: string;
  occasion?: string;
  search?: string;
  page?: number;
  pageSize?: number;
  sort?: string;
}

export const catalogApi = {
  getCategories: (includeInactive = false): Promise<ApiResponse<Category[]>> => {
    return api.get<Category[]>(`/api/v1/categories?includeInactive=${includeInactive}`);
  },

  getCategoryBySlug: (slug: string): Promise<ApiResponse<Category>> => {
    return api.get<Category>(`/api/v1/categories/slug/${encodeURIComponent(slug)}`);
  },

  getProducts: (params: ProductFilterParams = {}): Promise<ApiResponse<CatalogPageResponse<ProductSummary>>> => {
    const searchParams = new URLSearchParams();
    if (params.categoryId) searchParams.append('categoryId', params.categoryId);
    if (params.categorySlug) searchParams.append('categorySlug', params.categorySlug);
    if (params.minPrice !== undefined) searchParams.append('minPrice', params.minPrice.toString());
    if (params.maxPrice !== undefined) searchParams.append('maxPrice', params.maxPrice.toString());
    if (params.size) searchParams.append('size', params.size);
    if (params.colour) searchParams.append('colour', params.colour);
    if (params.fabric) searchParams.append('fabric', params.fabric);
    if (params.occasion) searchParams.append('occasion', params.occasion);
    if (params.search) searchParams.append('search', params.search);
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.pageSize !== undefined) searchParams.append('pageSize', params.pageSize.toString());
    if (params.sort) searchParams.append('sort', params.sort);

    const query = searchParams.toString();
    return api.get<CatalogPageResponse<ProductSummary>>(`/api/v1/products${query ? `?${query}` : ''}`);
  },

  getProductById: (id: string): Promise<ApiResponse<ProductDetail>> => {
    return api.get<ProductDetail>(`/api/v1/products/${id}`);
  },

  getProductBySlug: (slug: string): Promise<ApiResponse<ProductDetail>> => {
    return api.get<ProductDetail>(`/api/v1/products/slug/${encodeURIComponent(slug)}`);
  },

  getProductVariants: (productId: string): Promise<ApiResponse<ProductVariant[]>> => {
    return api.get<ProductVariant[]>(`/api/v1/products/${productId}/variants`);
  },

  getVariantStock: (variantId: string): Promise<ApiResponse<StockResponse>> => {
    return api.get<StockResponse>(`/api/v1/inventory/variants/${variantId}`);
  },

  getSkuStock: (sku: string): Promise<ApiResponse<StockResponse>> => {
    return api.get<StockResponse>(`/api/v1/inventory/sku/${encodeURIComponent(sku)}`);
  },
};
