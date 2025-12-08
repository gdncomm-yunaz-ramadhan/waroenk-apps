import { writable, derived, get } from 'svelte/store';
import { cartApi } from '../api/index.js';
import { authStore, isAuthenticated } from './auth.js';

/**
 * Cart store - Server-only implementation
 * Cart data is stored on the server, not in localStorage
 * Requires authentication for all operations
 */

/**
 * Check if user is authenticated
 */
function isLoggedIn() {
  const auth = get(authStore);
  return auth && auth.token && auth.user;
}

/**
 * Get current user ID
 */
function getUserId() {
  const auth = get(authStore);
  return auth?.user?.id || auth?.user?.userId || null;
}

/**
 * Convert server cart data to local format
 * Backend returns snake_case fields in proto format
 */
function serverToLocal(serverCart) {
  if (!serverCart || !serverCart.items) return [];
  return serverCart.items.map(item => ({
    sku: item.sku,
    subSku: item.sub_sku || item.subSku || item.sku,
    title: item.title || item.name || '',
    price: item.price_snapshot || item.priceSnapshot || item.price || 0,
    image: item.image_url || item.imageUrl || '',
    quantity: item.quantity || item.qtyRequested || 1,
    availableStock: item.available_stock_snapshot || item.availableStockSnapshot || 0
  }));
}

/**
 * Extract cart data from various response formats
 * Handles both direct CartData and wrapped responses (AddCartItemResponse, etc.)
 */
function extractCartData(response) {
  // If response has a 'cart' field (AddCartItemResponse, BulkAddCartItemsResponse)
  if (response && response.cart) {
    return response.cart;
  }
  // Direct CartData response
  return response;
}

/**
 * Create cart store
 */
function createCartStore() {
  const { subscribe, set, update } = writable([]);
  
  // Loading state
  let loading = false;
  let initialized = false;

  return {
    subscribe,

    /**
     * Initialize cart from server
     * Should be called when user logs in or app initializes
     */
    async init() {
      if (!isLoggedIn()) {
        set([]);
        initialized = true;
        return;
      }

      if (loading) return;
      loading = true;

      try {
        const userId = getUserId();
        if (!userId) {
          set([]);
          return;
        }

        const serverCart = await cartApi.get(userId);
        if (serverCart) {
          const items = serverToLocal(serverCart);
          set(items);
        } else {
          set([]);
        }
      } catch (e) {
        console.error('Failed to load cart from server:', e);
        set([]);
      } finally {
        loading = false;
        initialized = true;
      }
    },

    /**
     * Add item to cart (server only)
     * Requires authentication
     * @param {Object} item - Item to add
     * @param {string} item.sku - Product SKU
     * @param {string} item.subSku - Variant sub-SKU (for stock validation, defaults to sku)
     * @param {number} item.quantity - Quantity to add
     */
    async addItem(item) {
      if (!isLoggedIn()) {
        // Redirect to login instead of throwing error
        if (typeof window !== 'undefined') {
          const currentPath = window.location.pathname;
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
        }
        throw new Error('redirect');
      }

      const userId = getUserId();
      if (!userId) {
        throw new Error('User ID not found');
      }

      try {
        const response = await cartApi.addItem({
          userId: userId,
          sku: item.sku,
          subSku: item.subSku || item.sku, // fallback to sku if subSku not provided
          quantity: item.quantity || 1
        });

        // Handle AddCartItemResponse format (cart is nested)
        const cartData = extractCartData(response);
        if (cartData) {
          const items = serverToLocal(cartData);
          set(items);
        }
        
        // Return response for caller to check success/message if needed
        return response;
      } catch (e) {
        // Check if it's a 401 error - redirect to login
        if (e.status === 401) {
          if (typeof window !== 'undefined') {
            const currentPath = window.location.pathname;
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
          }
          throw new Error('redirect');
        }
        console.error('Failed to add item to cart:', e);
        throw e;
      }
    },

    /**
     * Update item quantity (server only)
     */
    async updateQuantity(sku, quantity) {
      if (!isLoggedIn()) {
        console.warn('User must be logged in to update cart');
        return;
      }

      const userId = getUserId();
      if (!userId) return;

      try {
        if (quantity <= 0) {
          // Remove item if quantity is 0 or less
          await this.removeItem(sku);
        } else {
          const response = await cartApi.updateItem({
            userId: userId,
            sku,
            quantity
          });

          // Handle AddCartItemResponse format (cart is nested)
          const cartData = extractCartData(response);
          if (cartData) {
            const items = serverToLocal(cartData);
            set(items);
          }
        }
      } catch (e) {
        console.error('Failed to update cart item:', e);
        throw e;
      }
    },

    /**
     * Remove item from cart (server only)
     */
    async removeItem(sku) {
      if (!isLoggedIn()) {
        console.warn('User must be logged in to remove items from cart');
        return;
      }

      const userId = getUserId();
      if (!userId) return;

      try {
        const serverCart = await cartApi.removeItem({
          userId: userId,
          sku
        });

        if (serverCart) {
          const items = serverToLocal(serverCart);
          set(items);
        }
      } catch (e) {
        console.error('Failed to remove item from cart:', e);
        throw e;
      }
    },

    /**
     * Clear entire cart (server only)
     */
    async clear() {
      if (!isLoggedIn()) {
        set([]);
        return;
      }

      const userId = getUserId();
      if (!userId) {
        set([]);
        return;
      }

      try {
        await cartApi.clear(userId);
        set([]);
      } catch (e) {
        console.error('Failed to clear cart:', e);
        throw e;
      }
    },

    /**
     * Refresh cart from server
     * Useful for syncing after operations
     */
    async refresh() {
      await this.init();
    },

    /**
     * Sync cart on login
     * Called when user logs in
     */
    async syncOnLogin() {
      await this.init();
    },

    /**
     * Clear local cart on logout
     */
    clearOnLogout() {
      set([]);
      initialized = false;
    },

    /**
     * Check if cart is initialized
     */
    isInitialized() {
      return initialized;
    },

    /**
     * Check if cart is loading
     */
    isLoading() {
      return loading;
    }
  };
}

export const cartStore = createCartStore();

// Derived store for cart item count
export const cartCount = derived(cartStore, $cart => 
  $cart.reduce((total, item) => total + item.quantity, 0)
);

// Derived store for cart total
export const cartTotal = derived(cartStore, $cart =>
  $cart.reduce((total, item) => total + (item.price * item.quantity), 0)
);

// Subscribe to auth changes to sync cart
if (typeof window !== 'undefined') {
  authStore.subscribe(auth => {
    if (auth && auth.token && auth.user) {
      // User logged in, sync cart
      cartStore.syncOnLogin();
    } else {
      // User logged out, clear cart
      cartStore.clearOnLogout();
    }
  });
}
