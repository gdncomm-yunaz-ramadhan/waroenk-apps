import { writable, derived, get } from 'svelte/store';

const STORAGE_KEY = 'waroenk_auth';

/**
 * Load auth state from localStorage
 */
function loadAuthState() {
  if (typeof window === 'undefined') return { user: null, token: null, refreshToken: null };
  
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    console.error('Failed to load auth state:', e);
  }
  
  return { user: null, token: null, refreshToken: null };
}

/**
 * Save auth state to localStorage
 */
function saveAuthState(state) {
  if (typeof window === 'undefined') return;
  
  try {
    if (state.token) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch (e) {
    console.error('Failed to save auth state:', e);
  }
}

/**
 * Create auth store
 */
function createAuthStore() {
  const initialState = loadAuthState();
  const { subscribe, set, update } = writable(initialState);

  return {
    subscribe,
    
    /**
     * Set user and tokens after login
     */
    login(user, token, refreshToken = null) {
      const state = { user, token, refreshToken };
      saveAuthState(state);
      set(state);
    },

    /**
     * Clear auth state on logout
     */
    logout() {
      const state = { user: null, token: null, refreshToken: null };
      saveAuthState(state);
      set(state);
    },

    /**
     * Update tokens after refresh
     */
    updateTokens(token, refreshToken) {
      update(state => {
        const newState = { ...state, token, refreshToken };
        saveAuthState(newState);
        return newState;
      });
    },

    /**
     * Update user profile
     */
    updateUser(userData) {
      update(state => {
        const newState = { ...state, user: userData };
        saveAuthState(newState);
        return newState;
      });
    },

    /**
     * Get current refresh token
     */
    getRefreshToken() {
      const state = get({ subscribe });
      return state.refreshToken;
    },

    /**
     * Initialize store (call on app mount)
     */
    init() {
      const state = loadAuthState();
      set(state);
    }
  };
}

export const authStore = createAuthStore();

// Derived store for checking if user is logged in
export const isAuthenticated = derived(authStore, $auth => !!$auth.token);

// Derived store for getting user info
export const currentUser = derived(authStore, $auth => $auth.user);

