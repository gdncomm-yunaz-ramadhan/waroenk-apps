import { api } from './client.js';

/**
 * Products API service
 */
export const productsApi = {
  /**
   * Get product by ID (from catalog/postgres)
   * @param {string} id - Product ID
   */
  async getById(id) {
    return api.get('/product', { params: { id } });
  },

  /**
   * Get product by SKU (from catalog/postgres)
   * @param {string} sku - Product SKU
   */
  async getBySku(sku) {
    return api.get('/product/by-sku', { params: { sku } });
  },

  /**
   * Get verbose product details by SKU or subSku (from Typesense/search)
   * Returns complete product info with merchant, brand, category, inventory, and variant images.
   * @param {string} id - Product SKU or subSku
   */
  async getDetails(id) {
    const response = await api.get('/product/details', { params: { id } });
    return response.product; // Extract product from { product, took } response
  },

  /**
   * Filter products
   * @param {Object} filters - Filter parameters
   * @param {string} [filters.title] - Title search
   * @param {string} [filters.sku] - SKU filter
   * @param {string} [filters.merchant_code] - Merchant code filter
   * @param {string} [filters.category_id] - Category ID filter
   * @param {string} [filters.brand_id] - Brand ID filter
   * @param {number} filters.size - Page size
   * @param {string} [filters.cursor] - Pagination cursor
   */
  async filter(filters) {
    return api.get('/product/filter', { params: filters });
  },

  /**
   * Get product summary by multiple subSkus
   * @param {string[]} subSkus - Array of subSkus to lookup
   */
  async getSummary(subSkus) {
    return api.post('/product/summary', { subSkus });
  }
};

