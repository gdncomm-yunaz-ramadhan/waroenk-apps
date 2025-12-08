import { api } from './client.js';

/**
 * Merchants API service
 */
export const merchantsApi = {
  /**
   * Get merchant by ID
   * @param {string} id - Merchant ID
   */
  async getById(id) {
    return api.get('/merchant', { params: { id } });
  },

  /**
   * Get merchant by code
   * @param {string} code - Merchant code
   */
  async getByCode(code) {
    return api.get('/merchant/by-code', { params: { code } });
  },

  /**
   * Filter merchants
   * @param {Object} filters - Filter parameters
   * @param {string} [filters.name] - Name search
   * @param {string} [filters.code] - Merchant code filter
   * @param {number} filters.size - Page size
   * @param {string} [filters.cursor] - Pagination cursor
   */
  async filter(filters) {
    return api.get('/merchant/filter', { params: filters });
  }
};

