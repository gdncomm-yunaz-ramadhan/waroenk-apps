import { api } from './client.js';

/**
 * Brands API service
 */
export const brandsApi = {
  /**
   * Get brand by ID
   * @param {string} id - Brand ID
   */
  async getById(id) {
    return api.get('/brand', { params: { id } });
  },

  /**
   * Filter brands
   * @param {Object} filters - Filter parameters
   * @param {string} [filters.name] - Name search
   * @param {string} [filters.slug] - Slug filter
   * @param {number} filters.size - Page size
   * @param {string} [filters.cursor] - Pagination cursor
   */
  async filter(filters) {
    return api.get('/brand/filter', { params: filters });
  }
};









