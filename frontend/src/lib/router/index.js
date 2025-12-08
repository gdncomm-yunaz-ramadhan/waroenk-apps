import { writable } from 'svelte/store';

// Current path store
function createRouter() {
  const { subscribe, set } = writable(
    typeof window !== 'undefined' ? window.location.pathname + window.location.search : '/'
  );

  if (typeof window !== 'undefined') {
    // Listen for popstate (back/forward navigation)
    window.addEventListener('popstate', () => {
      set(window.location.pathname + window.location.search);
    });

    // Intercept link clicks for client-side navigation
    document.addEventListener('click', (e) => {
      const link = e.target.closest('a');
      if (!link) return;

      const href = link.getAttribute('href');
      if (!href) return;

      // Only handle internal links
      if (href.startsWith('/') && !href.startsWith('//')) {
        // Don't intercept if modifier keys are pressed
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
        
        // Don't intercept if link has target="_blank"
        if (link.target === '_blank') return;

        e.preventDefault();
        
        // Navigate
        if (window.location.pathname + window.location.search !== href) {
          window.history.pushState({}, '', href);
          set(href);
        }
      }
    });
  }

  return {
    subscribe,
    
    /**
     * Navigate to a path
     * @param {string} path - The path to navigate to
     * @param {boolean} replace - Replace current history entry instead of pushing
     */
    navigate(path, replace = false) {
      if (replace) {
        window.history.replaceState({}, '', path);
      } else {
        window.history.pushState({}, '', path);
      }
      set(path);
    },

    /**
     * Go back in history
     */
    back() {
      window.history.back();
    },

    /**
     * Go forward in history
     */
    forward() {
      window.history.forward();
    }
  };
}

export const router = createRouter();
export const currentPath = { subscribe: router.subscribe };

/**
 * Navigate programmatically
 * @param {string} path - The path to navigate to
 * @param {boolean} replace - Replace current history entry
 */
export function navigate(path, replace = false) {
  router.navigate(path, replace);
}

/**
 * Match a path against a pattern
 * Supports simple patterns like /product/:id
 * @param {string} pattern - The route pattern
 * @param {string} path - The current path (may include query string)
 * @returns {object|null} - Matched params or null
 */
export function matchPath(pattern, path) {
  // Remove query string for matching
  const pathWithoutQuery = path.split('?')[0];

  // Handle exact match
  if (pattern === pathWithoutQuery) {
    return {};
  }

  // Handle wildcard
  if (pattern === '*') {
    return {};
  }

  // Handle parameterized routes
  const patternParts = pattern.split('/').filter(Boolean);
  const pathParts = pathWithoutQuery.split('/').filter(Boolean);

  if (patternParts.length !== pathParts.length) {
    return null;
  }

  const params = {};

  for (let i = 0; i < patternParts.length; i++) {
    const patternPart = patternParts[i];
    const pathPart = pathParts[i];

    if (patternPart.startsWith(':')) {
      // This is a parameter
      params[patternPart.slice(1)] = pathPart;
    } else if (patternPart !== pathPart) {
      // Parts don't match
      return null;
    }
  }

  return params;
}
