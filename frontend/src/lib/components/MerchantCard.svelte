<script>
  let { merchant, index = 0 } = $props();
  let imageLoaded = $state(false);
  let imageError = $state(false);

  function handleImageLoad() {
    imageLoaded = true;
  }

  function handleImageError() {
    imageError = true;
    imageLoaded = true;
  }
</script>

<a 
  href="/merchant/{merchant.code || merchant.id}"
  class="card card-interactive group p-5 animate-fade-in stagger-{(index % 8) + 1}"
  style="opacity: 0;"
>
  <div class="flex items-start gap-4">
    <!-- Avatar -->
    <div class="w-14 h-14 rounded-full overflow-hidden bg-[var(--color-border-light)] flex-shrink-0 ring-4 ring-[var(--color-border-light)] group-hover:ring-[var(--color-primary)]/20 transition-all duration-300 relative">
      {#if merchant.icon_url && !imageError}
        <!-- Loading skeleton -->
        {#if !imageLoaded}
          <div class="absolute inset-0 skeleton rounded-full"></div>
        {/if}
        <img 
          src={merchant.icon_url} 
          alt={merchant.name}
          class="w-full h-full object-cover {imageLoaded ? 'opacity-100' : 'opacity-0'}"
          loading="lazy"
          onload={handleImageLoad}
          onerror={handleImageError}
        />
      {:else}
        <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-primary-dark)]">
          <span class="text-xl font-bold text-white">
            {merchant.name?.charAt(0)?.toUpperCase() || 'M'}
          </span>
        </div>
      {/if}
    </div>

    <!-- Info -->
    <div class="flex-1 min-w-0">
      <h3 class="font-semibold text-[var(--color-text)] group-hover:text-[var(--color-primary)] transition-colors truncate mb-1">
        {merchant.name}
      </h3>
      
      {#if merchant.location}
        <p class="text-xs text-[var(--color-text-muted)] truncate flex items-center gap-1.5 mb-3">
          <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {merchant.location}
        </p>
      {/if}

      <span class="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--color-primary)]">
        View store
        <svg class="w-3.5 h-3.5 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </span>
    </div>
  </div>
</a>
