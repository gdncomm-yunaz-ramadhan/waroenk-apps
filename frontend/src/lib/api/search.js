import { api } from './client.js';

/**
 * Search API service
 */
export const searchApi = {
  /**
   * Combined search for products and merchants
   * @param {Object} params - Search parameters
   * @param {string} params.query - Search query
   * @param {number} params.size - Results per page
   * @param {string} [params.cursor] - Pagination cursor
   * @param {string} [params.sort_by] - Sort field
   * @param {string} [params.sort_order] - Sort order (asc/desc)
   */
  async combined(params) {
    return api.get('/search', { params });
  },

  /**
   * Search products only
   * @param {Object} params - Search parameters
   */
  async products(params) {
    return api.get('/search/products', { params });
  },

  /**
   * Search merchants only
   * @param {Object} params - Search parameters
   */
  async merchants(params) {
    return api.get('/search/merchants', { params });
  }
};

