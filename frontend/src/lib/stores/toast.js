import { writable } from 'svelte/store';

/**
 * Toast notification store
 */
function createToastStore() {
  const { subscribe, update } = writable([]);
  
  let idCounter = 0;

  return {
    subscribe,

    /**
     * Show a toast notification
     * @param {string} message - Toast message
     * @param {string} type - Toast type: 'success' | 'error' | 'warning' | 'info'
     * @param {number} duration - Duration in ms (default 3000)
     */
    show(message, type = 'info', duration = 3000) {
      const id = ++idCounter;
      
      update(toasts => [...toasts, { id, message, type }]);
      
      if (duration > 0) {
        setTimeout(() => {
          this.dismiss(id);
        }, duration);
      }
      
      return id;
    },

    /**
     * Show success toast
     */
    success(message, duration) {
      return this.show(message, 'success', duration);
    },

    /**
     * Show error toast
     */
    error(message, duration) {
      return this.show(message, 'error', duration);
    },

    /**
     * Show warning toast
     */
    warning(message, duration) {
      return this.show(message, 'warning', duration);
    },

    /**
     * Dismiss a toast
     */
    dismiss(id) {
      update(toasts => toasts.filter(t => t.id !== id));
    },

    /**
     * Clear all toasts
     */
    clear() {
      update(() => []);
    }
  };
}

export const toastStore = createToastStore();

