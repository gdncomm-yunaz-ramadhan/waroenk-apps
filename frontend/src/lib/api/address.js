import { api } from './client.js';

/**
 * Address API service
 */
export const addressApi = {
  /**
   * Get address by ID
   * @param {string} id - Address ID
   */
  async getById(id) {
    return api.get('/address', { params: { id }, requiresAuth: true });
  },

  /**
   * Filter user's addresses
   * @param {Object} filters - Filter parameters
   */
  async filter(filters = {}) {
    return api.get('/address/filter', { params: filters, requiresAuth: true });
  },

  /**
   * Create or update address
   * @param {Object} data - Address data
   */
  async upsert(data) {
    return api.post('/address', data, { requiresAuth: true });
  },

  /**
   * Delete address
   * @param {string} id - Address ID
   */
  async delete(id) {
    return api.delete('/address', { params: { id }, requiresAuth: true });
  },

  /**
   * Set address as default
   * @param {string} id - Address ID
   */
  async setDefault(id) {
    return api.put('/address/default', { address_id: id }, { requiresAuth: true });
  }
};

