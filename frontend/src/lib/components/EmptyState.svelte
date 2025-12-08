<script>
  let { 
    title = 'No results found', 
    message = 'Try adjusting your search or filters.',
    icon = 'search',
    action = null,
    actionLabel = ''
  } = $props();

  const icons = {
    search: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />`,
    cart: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />`,
    product: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />`,
    error: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />`
  };
</script>

<div class="text-center py-16 animate-fade-in" style="opacity: 0;">
  <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-[var(--color-border-light)] flex items-center justify-center">
    <svg class="w-10 h-10 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      {@html icons[icon] || icons.search}
    </svg>
  </div>
  <h3 class="text-lg font-semibold text-[var(--color-text)] mb-2">{title}</h3>
  <p class="text-sm text-[var(--color-text-muted)] max-w-sm mx-auto {action ? 'mb-6' : ''}">{message}</p>
  
  {#if action}
    <button onclick={action} class="btn btn-primary">
      {actionLabel}
    </button>
  {/if}
</div>
