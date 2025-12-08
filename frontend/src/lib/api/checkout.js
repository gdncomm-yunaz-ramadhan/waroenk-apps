import { api } from './client.js';

/**
 * Checkout API service
 * Communicates with the Checkout microservice via API Gateway
 * Field names use snake_case to match gRPC proto contract
 */
export const checkoutApi = {
  /**
   * Prepare checkout - bulk lock inventory and create checkout session
   * This is the new preferred method (replaces validate)
   * @param {string} userId - User ID
   * @returns {Promise<Object>} PrepareCheckoutResponse with checkout data and lock summary
   */
  async prepare(userId) {
    return api.post('/checkout/prepare', { user_id: userId }, { requiresAuth: true });
  },

  /**
   * Validate cart and reserve inventory for checkout
   * @deprecated Use prepare() instead
   * Creates a checkout session with reserved items
   */
  async validate() {
    return api.post('/checkout/validate', {}, { requiresAuth: true });
  },

  /**
   * Get checkout by ID
   * @param {string} checkoutId - Checkout ID
   */
  async getById(checkoutId) {
    return api.get(`/checkout/${checkoutId}`, { requiresAuth: true });
  },

  /**
   * Finalize checkout with shipping address
   * @param {string} checkoutId - Checkout ID
   * @param {string} [addressId] - Existing address ID to use
   * @param {Object} [newAddress] - New address data (if not using existing)
   */
  async finalize(checkoutId, addressId = null, newAddress = null) {
    const payload = {};
    if (addressId) {
      payload.address_id = addressId;
    }
    if (newAddress) {
      payload.new_address = newAddress;
    }
    return api.post(`/checkout/${checkoutId}/finalize`, payload, { requiresAuth: true });
  },

  /**
   * Pay checkout (simulate payment)
   * @param {string} checkoutId - Checkout ID
   */
  async pay(checkoutId) {
    return api.post(`/checkout/${checkoutId}/pay`, {}, { requiresAuth: true });
  },

  /**
   * Cancel/invalidate checkout and release reservations
   * @param {string} checkoutId - Checkout ID to cancel
   */
  async cancel(checkoutId) {
    return api.post(`/checkout/${checkoutId}/cancel`, {}, { requiresAuth: true });
  },

  /**
   * Filter checkouts with pagination
   * @param {Object} params - Filter parameters
   * @param {string} [params.status] - Filter by status (WAITING, PAID, EXPIRED, CANCELLED)
   * @param {number} [params.size] - Results per page
   * @param {string} [params.cursor] - Pagination cursor
   */
  async filter(params = {}) {
    return api.get('/checkouts', { params, requiresAuth: true });
  }
};
