import { api } from './client.js';

/**
 * Cart API service
 * Communicates with the Cart microservice via API Gateway
 * 
 * All cart operations require authentication and use the user's session
 * Field names use snake_case to match gRPC proto contract
 */
export const cartApi = {
  /**
   * Get user's cart from server
   * @param {string} userId - User ID
   * @returns {Promise<Object>} Cart data with items
   */
  async get(userId) {
    if (!userId) {
      throw new Error('User ID is required');
    }
    return api.get(`/cart/${userId}`, { requiresAuth: true });
  },

  /**
   * Add item to cart
   * @param {Object} item - Cart item data
   * @param {string} item.userId - User ID
   * @param {string} item.sku - Product SKU
   * @param {string} item.subSku - Variant sub-SKU (for stock validation)
   * @param {number} item.quantity - Quantity to add
   * @returns {Promise<Object>} Add cart item response with success status and available stock
   */
  async addItem(item) {
    return api.post('/cart/add', {
      user_id: item.userId,
      sku: item.sku,
      sub_sku: item.subSku || item.sku, // fallback to sku if subSku not provided
      quantity: item.quantity || 1
    }, { requiresAuth: true });
  },

  /**
   * Add multiple items to cart (bulk)
   * @param {string} userId - User ID
   * @param {Array} items - Array of cart items with sku, subSku, and quantity
   * @returns {Promise<Object>} Bulk add response with success statuses
   */
  async bulkAddItems(userId, items) {
    return api.post('/cart/bulk-add', { 
      user_id: userId,
      items: items.map(item => ({
        sku: item.sku,
        sub_sku: item.subSku || item.sku, // fallback to sku if subSku not provided
        quantity: item.quantity || 1
      }))
    }, { requiresAuth: true });
  },

  /**
   * Update cart item quantity
   * @param {Object} params - Update parameters
   * @param {string} params.userId - User ID
   * @param {string} params.sku - Product SKU
   * @param {number} params.quantity - New quantity
   * @returns {Promise<Object>} Updated cart data
   */
  async updateItem(params) {
    return api.put('/cart/update', {
      user_id: params.userId,
      sku: params.sku,
      quantity: params.quantity
    }, { requiresAuth: true });
  },

  /**
   * Remove item from cart
   * @param {Object} params - Remove parameters
   * @param {string} params.userId - User ID
   * @param {string} params.sku - Product SKU to remove
   * @returns {Promise<Object>} Updated cart data
   */
  async removeItem(params) {
    return api.post('/cart/remove', {
      user_id: params.userId,
      sku: params.sku
    }, { requiresAuth: true });
  },

  /**
   * Remove multiple items from cart (bulk)
   * @param {string} userId - User ID
   * @param {Array<string>} skus - Array of SKUs to remove
   * @returns {Promise<Object>} Updated cart data
   */
  async bulkRemoveItems(userId, skus) {
    return api.post('/cart/bulk-remove', {
      user_id: userId,
      skus
    }, { requiresAuth: true });
  },

  /**
   * Clear entire cart
   * @param {string} userId - User ID
   * @returns {Promise<Object>} Success response
   */
  async clear(userId) {
    if (!userId) {
      throw new Error('User ID is required');
    }
    return api.delete(`/cart/${userId}`, { requiresAuth: true });
  }
};
