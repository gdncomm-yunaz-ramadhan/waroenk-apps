<script>
  import { onMount, onDestroy } from 'svelte';
  import { searchApi, categoriesApi } from '../lib/api/index.js';
  import { ProductCard, Loading, EmptyState } from '../lib/components/index.js';

  let searchParams = new URLSearchParams(window.location.search);
  
  let query = $state(searchParams.get('q') || '');
  let selectedCategory = $state(searchParams.get('category') || '');
  let products = $state([]);
  let categories = $state([]);
  let loading = $state(true);
  let loadingMore = $state(false);
  let error = $state(null);
  let totalMatch = $state(0);
  let totalDocument = $state(0);
  let elapsedTime = $state(0);
  let nextToken = $state(null);
  const pageSize = 12;
  
  // Lazy load
  let loadMoreRef = $state(null);
  let observer = null;

  async function loadProducts(cursor = null) {
    if (cursor) {
      loadingMore = true;
    } else {
      loading = true;
    }
    error = null;
    
    try {
      const startTime = performance.now();
      
      // Build search params with category filter
      const searchParams = {
        query: query,
        size: pageSize,
        cursor: cursor
      };
      
      // Add category filter if selected (using category name, not ID)
      if (selectedCategory) {
        searchParams.category = selectedCategory;
      }
      
      const res = await searchApi.products(searchParams);
      const endTime = performance.now();
      
      if (!cursor) {
        elapsedTime = Math.round(endTime - startTime);
      }
      
      if (cursor) {
        products = [...products, ...(res.contents || [])];
      } else {
        products = res.contents || [];
      }
      
      totalMatch = res.total_match || 0;
      totalDocument = res.total_document || totalMatch;
      nextToken = res.next_token || null;
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
      loadingMore = false;
    }
  }

  async function loadCategories() {
    try {
      const res = await categoriesApi.filter({ size: 20 });
      categories = res.data || [];
    } catch (e) {
      console.error('Failed to load categories:', e);
    }
  }

  function setupIntersectionObserver() {
    if (loadMoreRef) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextToken && !loadingMore && !loading) {
            loadProducts(nextToken);
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
    loadProducts();
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
  
  // React to category changes
  $effect(() => {
    // Only trigger if selectedCategory changes (after initial mount)
    const currentCat = selectedCategory;
    if (typeof currentCat === 'string') {
      // Update URL with category
      const url = new URL(window.location.href);
      if (currentCat) {
        url.searchParams.set('category', currentCat);
      } else {
        url.searchParams.delete('category');
      }
      if (query) {
        url.searchParams.set('q', query);
      } else {
        url.searchParams.delete('q');
      }
      window.history.replaceState({}, '', url.toString());
    }
  });

  function handleSearch(e) {
    e.preventDefault();
    loadProducts();
  }
  
  function handleCategoryChange() {
    // Reset and reload products when category changes
    loadProducts();
  }
</script>

<svelte:head>
  <title>Products - Waroenk</title>
</svelte:head>

<div class="min-h-screen">
  <!-- Page Header -->
  <div class="bg-white border-b border-[var(--color-border)]">
    <div class="container py-6">
      <h1 class="text-2xl font-bold text-[var(--color-text)] mb-5">
        {query ? `Results for "${query}"` : 'All Products'}
      </h1>

      <!-- Search & Filters Bar -->
      <div class="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
        <!-- Search Input -->
        <form onsubmit={handleSearch} class="flex-1">
          <div class="search-input-container">
            <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              bind:value={query}
              placeholder="Search products..."
              class="search-input"
            />
            {#if query}
              <button 
                type="button" 
                onclick={() => { query = ''; loadProducts(); }}
                class="search-clear-btn"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            {/if}
          </div>
        </form>

        <!-- Category Dropdown -->
        <div class="category-dropdown-container">
          <svg class="category-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
          </svg>
          <select
            bind:value={selectedCategory}
            onchange={handleCategoryChange}
            class="category-dropdown"
          >
            <option value="">All Categories</option>
            {#each categories as cat}
              <option value={cat.id}>{cat.name}</option>
            {/each}
          </select>
          <svg class="dropdown-chevron" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>

      <!-- Results Count with Search Info -->
      {#if !loading && totalMatch > 0}
        <p class="text-sm text-[var(--color-text-muted)] mt-4">
          Found <span class="font-semibold text-[var(--color-text)]">{totalMatch.toLocaleString()}</span> products 
          out of <span class="font-semibold text-[var(--color-text)]">{totalDocument.toLocaleString()}</span> documents 
          in <span class="font-semibold text-[var(--color-primary)]">{elapsedTime} ms</span>
        </p>
      {/if}
    </div>
  </div>

  <!-- Products Grid -->
  <div class="container py-6">
    {#if loading && products.length === 0}
      <Loading text="Loading products..." />
    {:else if error}
      <EmptyState title="Failed to load products" message={error} icon="error" />
    {:else if products.length === 0}
      <EmptyState 
        title="No products found" 
        message="Try adjusting your search"
        icon="product"
      />
    {:else}
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {#each products as product, i}
          <ProductCard {product} index={i} />
        {/each}
      </div>

      <!-- Lazy load sentinel -->
      {#if nextToken}
        <div bind:this={loadMoreRef} class="text-center mt-8 py-4">
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
  /* Search Input Container */
  .search-input-container {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    background: var(--color-bg);
    border: 1.5px solid var(--color-border);
    border-radius: 12px;
    padding: 0.75rem 1rem;
    transition: all 0.2s ease;
  }
  
  .search-input-container:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1);
    background: var(--color-surface);
  }
  
  .search-icon {
    width: 1.25rem;
    height: 1.25rem;
    color: var(--color-text-muted);
    flex-shrink: 0;
  }
  
  .search-input-container:focus-within .search-icon {
    color: var(--color-primary);
  }
  
  .search-input {
    flex: 1;
    border: none;
    background: transparent;
    outline: none;
    font-size: 0.9375rem;
    color: var(--color-text);
    font-family: var(--font-sans);
  }
  
  .search-input::placeholder {
    color: var(--color-text-muted);
  }
  
  .search-clear-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.25rem;
    background: var(--color-border-light);
    border: none;
    border-radius: 50%;
    color: var(--color-text-muted);
    cursor: pointer;
    transition: all 0.15s ease;
  }
  
  .search-clear-btn:hover {
    background: var(--color-border);
    color: var(--color-text);
  }
  
  /* Category Dropdown */
  .category-dropdown-container {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: var(--color-bg);
    border: 1.5px solid var(--color-border);
    border-radius: 12px;
    padding: 0.75rem 0.875rem;
    position: relative;
    min-width: 160px;
    max-width: 200px;
    transition: all 0.2s ease;
  }
  
  .category-dropdown-container:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(20, 184, 166, 0.1);
    background: var(--color-surface);
  }
  
  .category-icon {
    width: 1rem;
    height: 1rem;
    color: var(--color-text-muted);
    flex-shrink: 0;
  }
  
  .category-dropdown {
    flex: 1;
    appearance: none;
    -webkit-appearance: none;
    border: none;
    background: transparent;
    outline: none;
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--color-text);
    font-family: var(--font-sans);
    cursor: pointer;
    padding-right: 1.25rem;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .category-dropdown option {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 180px;
  }
  
  .dropdown-chevron {
    position: absolute;
    right: 0.75rem;
    width: 1rem;
    height: 1rem;
    color: var(--color-text-muted);
    pointer-events: none;
  }
  
  @media (max-width: 639px) {
    .category-dropdown-container {
      max-width: 100%;
      min-width: 100%;
    }
  }
</style>
