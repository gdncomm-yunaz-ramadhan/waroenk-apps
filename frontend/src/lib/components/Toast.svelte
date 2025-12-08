<script>
  import { toastStore } from '../stores/toast.js';

  const iconMap = {
    success: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />`,
    error: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />`,
    warning: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />`,
    info: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />`
  };

  const colorMap = {
    success: 'bg-[var(--color-success)]',
    error: 'bg-[var(--color-error)]',
    warning: 'bg-[var(--color-warning)]',
    info: 'bg-[var(--color-primary)]'
  };

  const bgMap = {
    success: 'bg-[var(--color-success)]/5 border-[var(--color-success)]/20',
    error: 'bg-[var(--color-error)]/5 border-[var(--color-error)]/20',
    warning: 'bg-[var(--color-warning)]/5 border-[var(--color-warning)]/20',
    info: 'bg-[var(--color-primary)]/5 border-[var(--color-primary)]/20'
  };
</script>

<div class="fixed bottom-6 right-6 z-[100] space-y-3">
  {#each $toastStore as toast (toast.id)}
    <div 
      class="flex items-center gap-3 px-4 py-3 bg-[var(--color-text)] text-white rounded-full shadow-xl animate-fade-in-scale min-w-[260px] max-w-[380px]"
      style="opacity: 0;"
      role="alert"
    >
      <div class="{colorMap[toast.type]} p-1.5 rounded-full flex-shrink-0">
        <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          {@html iconMap[toast.type]}
        </svg>
      </div>
      <p class="flex-1 text-sm font-medium">{toast.message}</p>
      <button 
        onclick={() => toastStore.dismiss(toast.id)}
        class="text-white/60 hover:text-white transition-colors flex-shrink-0 p-1 hover:bg-white/10 rounded-full"
        aria-label="Dismiss"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  {/each}
</div>
