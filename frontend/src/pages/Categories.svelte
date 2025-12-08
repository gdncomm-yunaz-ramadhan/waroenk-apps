<script>
  import { onMount, onDestroy } from 'svelte';
  import { categoriesApi } from '../lib/api/index.js';
  import { Loading, EmptyState } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let categories = $state([]);
  let loading = $state(true);
  let error = $state(null);
  let totalMatch = $state(0);
  let nextToken = $state(null);
  let loadingMore = $state(false);
  const pageSize = 24;
  
  // Lazy load
  let loadMoreRef = $state(null);
  let observer = null;

  async function loadCategories(cursor = null) {
    if (cursor) {
      loadingMore = true;
    } else {
      loading = true;
    }
    error = null;
    
    try {
      const res = await categoriesApi.filter({
        size: pageSize,
        cursor: cursor
      });
      
      // Handle both response formats: { data, next_cursor } and { contents, next_token }
      const newCategories = res.data || res.contents || [];
      
      if (cursor) {
        categories = [...categories, ...newCategories];
      } else {
        categories = newCategories;
      }
      
      totalMatch = res.total || res.total_match || categories.length;
      nextToken = res.next_cursor || res.next_token || null;
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
      loadingMore = false;
    }
  }

  function setupIntersectionObserver() {
    if (loadMoreRef) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextToken && !loadingMore && !loading) {
            loadCategories(nextToken);
          }
        },
        { threshold: 0.1, rootMargin: '100px' }
      );
      observer.observe(loadMoreRef);
    }
  }

  function cleanupObserver() {
    if (observer) {
      observer.disconnect();
      observer = null;
    }
  }

  onMount(() => {
    loadCategories();
  });
  
  onDestroy(() => {
    cleanupObserver();
  });
  
  // Re-setup observer when ref changes
  $effect(() => {
    cleanupObserver();
    if (loadMoreRef) {
      setupIntersectionObserver();
    }
  });

  function handleCategoryClick(category) {
    // Navigate to search page with category name prefilled
    navigate(`/search?q=${encodeURIComponent(category.name)}`);
  }
</script>

<svelte:head>
  <title>Categories - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <!-- Page Header -->
  <div class="bg-[var(--color-surface)] border-b border-[var(--color-border)]">
    <div class="container py-10">
      <h1 class="text-3xl font-bold text-[var(--color-text)] mb-2 tracking-tight">
        Browse Categories
      </h1>
      <p class="text-[var(--color-text-muted)]">
        Explore our product categories and find what you're looking for
      </p>

      <!-- Results Count -->
      {#if !loading && totalMatch > 0}
        <p class="text-sm text-[var(--color-text-muted)] mt-4">
          Showing {categories.length} of {totalMatch} categories
        </p>
      {/if}
    </div>
  </div>

  <!-- Categories Grid -->
  <div class="container py-10">
    {#if loading && categories.length === 0}
      <Loading text="Loading categories..." />
    {:else if error}
      <EmptyState title="Failed to load categories" message={error} icon="error" />
    {:else if categories.length === 0}
      <EmptyState 
        title="No categories found" 
        message="No categories are available at the moment."
        icon="search"
      />
    {:else}
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
        {#each categories as category, i}
          <button 
            onclick={() => handleCategoryClick(category)}
            class="group flex flex-col items-center p-6 rounded-2xl bg-[var(--color-surface)] hover:bg-[var(--color-primary)]/5 border border-[var(--color-border)] hover:border-[var(--color-primary)]/30 transition-all duration-300 animate-fade-in"
            style="opacity: 0; animation-delay: {Math.min(i * 30, 300)}ms;"
          >
            <!-- Icon -->
            <div class="w-16 h-16 rounded-2xl overflow-hidden bg-[var(--color-border-light)] mb-4 transition-transform duration-300 group-hover:scale-110">
              {#if category.icon_url}
                <img 
                  src={category.icon_url} 
                  alt={category.name}
                  class="w-full h-full object-cover"
                  loading="lazy"
                />
              {:else}
                <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-primary)]/5">
                  <svg class="w-8 h-8 text-[var(--color-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                </div>
              {/if}
            </div>

            <!-- Name -->
            <h3 class="text-sm font-semibold text-[var(--color-text)] text-center group-hover:text-[var(--color-primary)] transition-colors">
              {category.name}
            </h3>
            
            <!-- Product count if available -->
            {#if category.product_count}
              <p class="text-xs text-[var(--color-text-muted)] mt-1">
                {category.product_count} products
              </p>
            {/if}
          </button>
        {/each}
      </div>

      <!-- Lazy load sentinel -->
      {#if nextToken}
        <div bind:this={loadMoreRef} class="text-center mt-10 py-4">
          {#if loadingMore}
            <div class="flex items-center justify-center gap-2 text-[var(--color-text-muted)]">
              <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>Loading more...</span>
            </div>
          {:else}
            <span class="text-[var(--color-text-muted)] text-sm">Scroll for more</span>
          {/if}
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .animate-fade-in {
    animation: fadeIn 0.4s ease-out forwards;
  }
</style>


