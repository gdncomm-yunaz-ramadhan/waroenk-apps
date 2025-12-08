<script>
  let { category, index = 0 } = $props();
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
  href="/products?category={category.id}"
  class="group flex flex-col items-center p-4 rounded-2xl bg-[var(--color-surface)] hover:bg-[var(--color-primary)]/5 border border-transparent hover:border-[var(--color-primary)]/20 transition-all duration-300 animate-fade-in stagger-{(index % 8) + 1}"
  style="opacity: 0;"
>
  <!-- Icon -->
  <div class="w-12 h-12 rounded-2xl overflow-hidden bg-[var(--color-border-light)] mb-3 transition-transform duration-300 group-hover:scale-110 relative">
    {#if category.icon_url && !imageError}
      <!-- Loading skeleton -->
      {#if !imageLoaded}
        <div class="absolute inset-0 skeleton rounded-2xl"></div>
      {/if}
      <img 
        src={category.icon_url} 
        alt={category.name}
        class="w-full h-full object-cover {imageLoaded ? 'opacity-100' : 'opacity-0'}"
        loading="lazy"
        onload={handleImageLoad}
        onerror={handleImageError}
      />
    {:else}
      <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-primary)]/5">
        <svg class="w-6 h-6 text-[var(--color-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      </div>
    {/if}
  </div>

  <!-- Name -->
  <h3 class="text-xs font-semibold text-[var(--color-text)] text-center group-hover:text-[var(--color-primary)] transition-colors truncate w-full">
    {category.name}
  </h3>
</a>
