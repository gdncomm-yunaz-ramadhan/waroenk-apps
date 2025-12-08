import { API_BASE_URL, REQUEST_TIMEOUT, DEFAULT_HEADERS } from './config.js';
import { authStore } from '../stores/auth.js';
import { get } from 'svelte/store';

/**
 * Lightweight API client for REST communication with automatic token refresh
 */
class ApiClient {
  constructor() {
    this.baseUrl = API_BASE_URL;
    this.timeout = REQUEST_TIMEOUT;
    this.isRefreshing = false;
    this.refreshSubscribers = [];
  }

  /**
   * Get authorization headers if user is logged in
   */
  getAuthHeaders() {
    const auth = get(authStore);
    if (auth.token) {
      return { 'Authorization': `Bearer ${auth.token}` };
    }
    return {};
  }

  /**
   * Build query string from params object
   */
  buildQueryString(params) {
    const filtered = Object.entries(params || {})
      .filter(([_, v]) => v !== undefined && v !== null && v !== '');
    
    if (filtered.length === 0) return '';
    
    return '?' + filtered
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&');
  }

  /**
   * Subscribe to token refresh completion
   */
  subscribeToRefresh(callback) {
    this.refreshSubscribers.push(callback);
  }

  /**
   * Notify all subscribers that refresh is complete
   */
  onRefreshComplete(success) {
    this.refreshSubscribers.forEach(callback => callback(success));
    this.refreshSubscribers = [];
  }

  /**
   * Attempt to refresh the access token
   */
  async refreshAccessToken() {
    const auth = get(authStore);
    if (!auth.refreshToken) {
      return false;
    }

    try {
      const response = await fetch(`${this.baseUrl}/user/refresh-token`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify({ refresh_token: auth.refreshToken })
      });

      if (!response.ok) {
        return false;
      }

      const data = await response.json();
      
      // Update tokens in store
      authStore.updateTokens(data.access_token, data.refresh_token);
      return true;
    } catch (error) {
      console.error('Token refresh failed:', error);
      return false;
    }
  }

  /**
   * Handle 401 error - attempt token refresh or logout
   */
  async handle401(originalRequest) {
    const auth = get(authStore);
    
    // If no refresh token or this was already a retry, logout
    if (!auth.refreshToken || originalRequest.isRetry) {
      this.forceLogout();
      return null;
    }

    // If already refreshing, wait for it
    if (this.isRefreshing) {
      return new Promise((resolve) => {
        this.subscribeToRefresh((success) => {
          if (success) {
            resolve(this.retryRequest(originalRequest));
          } else {
            resolve(null);
          }
        });
      });
    }

    // Start refresh
    this.isRefreshing = true;
    const success = await this.refreshAccessToken();
    this.isRefreshing = false;
    this.onRefreshComplete(success);

    if (success) {
      return this.retryRequest(originalRequest);
    } else {
      this.forceLogout();
      return null;
    }
  }

  /**
   * Retry the original request with new token
   */
  async retryRequest(originalRequest) {
    return this.request(
      originalRequest.method,
      originalRequest.endpoint,
      { ...originalRequest.options, isRetry: true }
    );
  }

  /**
   * Force logout and redirect to login
   */
  forceLogout() {
    authStore.logout();
    // Redirect to login if not already there
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      const currentPath = window.location.pathname + window.location.search;
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}&expired=true`;
    }
  }

  /**
   * Make HTTP request with timeout and automatic token refresh
   */
  async request(method, endpoint, options = {}) {
    const { body, params, requiresAuth = false, headers = {}, skipAuthRefresh = false, isRetry = false } = options;
    
    const url = `${this.baseUrl}${endpoint}${this.buildQueryString(params)}`;
    
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);

    try {
      const requestHeaders = {
        ...DEFAULT_HEADERS,
        ...headers,
        ...(requiresAuth ? this.getAuthHeaders() : {})
      };

      const response = await fetch(url, {
        method,
        headers: requestHeaders,
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      // Parse response
      const contentType = response.headers.get('content-type');
      let data = null;
      
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      }

      // Handle 401 Unauthorized - attempt token refresh
      if (response.status === 401 && requiresAuth && !skipAuthRefresh) {
        const retryResult = await this.handle401({
          method,
          endpoint,
          options: { body, params, requiresAuth, headers }
        });
        
        if (retryResult !== null) {
          return retryResult;
        }
        
        // If we get here, refresh failed - throw the error
        throw new ApiError(
          'Session expired. Please login again.',
          401,
          data
        );
      }

      if (!response.ok) {
        throw new ApiError(
          data?.message || data?.details || `Request failed with status ${response.status}`,
          response.status,
          data
        );
      }

      return data;
    } catch (error) {
      clearTimeout(timeoutId);
      
      if (error.name === 'AbortError') {
        throw new ApiError('Request timeout', 504);
      }
      
      if (error instanceof ApiError) {
        throw error;
      }
      
      throw new ApiError(error.message || 'Network error', 0);
    }
  }

  // HTTP method shortcuts
  get(endpoint, options) {
    return this.request('GET', endpoint, options);
  }

  post(endpoint, body, options = {}) {
    return this.request('POST', endpoint, { ...options, body });
  }

  put(endpoint, body, options = {}) {
    return this.request('PUT', endpoint, { ...options, body });
  }

  delete(endpoint, options) {
    return this.request('DELETE', endpoint, options);
  }
}

/**
 * Custom API Error class
 */
export class ApiError extends Error {
  constructor(message, status, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

// Export singleton instance
export const api = new ApiClient();

