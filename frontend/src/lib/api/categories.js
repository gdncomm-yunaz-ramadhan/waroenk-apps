import { api } from './client.js';

/**
 * Categories API service
 */
export const categoriesApi = {
  /**
   * Get category by ID
   * @param {string} id - Category ID
   */
  async getById(id) {
    return api.get('/category', { params: { id } });
  },

  /**
   * Filter categories
   * @param {Object} filters - Filter parameters
   * @param {string} [filters.name] - Name search
   * @param {string} [filters.slug] - Slug filter
   * @param {string} [filters.parent_id] - Parent category ID
   * @param {number} filters.size - Page size
   * @param {string} [filters.cursor] - Pagination cursor
   */
  async filter(filters) {
    return api.get('/category/filter', { params: filters });
  }
};

