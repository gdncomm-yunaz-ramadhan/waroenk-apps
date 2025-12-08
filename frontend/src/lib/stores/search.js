import { writable } from 'svelte/store';

/**
 * Create search state store
 */
function createSearchStore() {
  const { subscribe, set, update } = writable({
    query: '',
    results: null,
    loading: false,
    error: null
  });

  return {
    subscribe,

    /**
     * Set search query
     */
    setQuery(query) {
      update(state => ({ ...state, query }));
    },

    /**
     * Set loading state
     */
    setLoading(loading) {
      update(state => ({ ...state, loading, error: null }));
    },

    /**
     * Set search results
     */
    setResults(results) {
      update(state => ({ ...state, results, loading: false, error: null }));
    },

    /**
     * Set error
     */
    setError(error) {
      update(state => ({ ...state, error, loading: false }));
    },

    /**
     * Reset search state
     */
    reset() {
      set({ query: '', results: null, loading: false, error: null });
    }
  };
}

export const searchStore = createSearchStore();

