<script>
  import { onMount, onDestroy } from 'svelte';
  import { merchantsApi, searchApi } from '../lib/api/index.js';
  import { Loading, EmptyState, ProductCard } from '../lib/components/index.js';

  let { id } = $props();
  
  let merchant = $state(null);
  let products = $state([]);
  let loading = $state(true);
  let loadingProducts = $state(true);
  let loadingMore = $state(false);
  let error = $state(null);
  let productQuery = $state('');
  let nextToken = $state(null);
  const pageSize = 12;
  
  // Lazy load
  let loadMoreRef = $state(null);
  let observer = null;

  onMount(async () => {
    try {
      // Use merchant by-code API (preferred)
      merchant = await merchantsApi.getByCode(id).catch(() => null);
      
      if (merchant) {
        await loadProducts();
      }
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  });
  
  onDestroy(() => {
    cleanupObserver();
  });

  function setupIntersectionObserver() {
    if (loadMoreRef) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextToken && !loadingMore && !loadingProducts) {
            loadMoreProducts();
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
  
  // Re-setup observer when ref changes
  $effect(() => {
    cleanupObserver();
    if (loadMoreRef) {
      setupIntersectionObserver();
    }
  });

  async function loadProducts(cursor = null) {
    if (cursor) {
      loadingMore = true;
    } else {
      loadingProducts = true;
    }
    
    try {
      // Use merchant code or name for searching products
      const searchQuery = productQuery || merchant?.name || '';
      const res = await searchApi.products({
        query: searchQuery,
        merchant_code: merchant?.code,
        size: pageSize,
        cursor: cursor
      });
      
      if (cursor) {
        products = [...products, ...(res.contents || [])];
      } else {
        products = res.contents || [];
      }
      
      nextToken = res.next_token || null;
    } catch (e) {
      console.error('Failed to load products:', e);
    } finally {
      loadingProducts = false;
      loadingMore = false;
    }
  }
  
  async function loadMoreProducts() {
    if (nextToken && !loadingMore) {
      loadProducts(nextToken);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    loadProducts();
  }

  function formatRating(rating) {
    return rating ? rating.toFixed(1) : 'N/A';
  }
</script>

<svelte:head>
  <title>{merchant?.name || 'Merchant'} - Waroenk</title>
</svelte:head>

<div class="min-h-screen">
  {#if loading}
    <Loading text="Loading merchant..." />
  {:else if error || !merchant}
    <div class="container py-12">
      <EmptyState 
        title="Merchant not found" 
        message={error || "The merchant you're looking for doesn't exist."} 
        icon="search" 
      />
      <div class="text-center mt-4">
        <a href="/merchants" class="btn btn-primary">Browse Merchants</a>
      </div>
    </div>
  {:else}
    <!-- Merchant Header -->
    <div class="bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-primary-dark)] text-white">
      <div class="container py-12 md:py-16">
        <div class="flex flex-col md:flex-row items-start md:items-center gap-6">
          <!-- Icon -->
          <div class="w-20 h-20 rounded-xl overflow-hidden bg-white/10 flex-shrink-0">
            {#if merchant.icon_url}
              <img 
                src={merchant.icon_url} 
                alt={merchant.name}
                class="w-full h-full object-cover"
                loading="lazy"
              />
            {:else}
              <div class="w-full h-full flex items-center justify-center">
                <span class="text-3xl font-bold">
                  {merchant.name?.charAt(0)?.toUpperCase() || 'M'}
                </span>
              </div>
            {/if}
          </div>

          <!-- Info -->
          <div class="flex-1">
            <h1 class="text-2xl font-semibold mb-2">{merchant.name}</h1>
            
            <div class="flex flex-wrap items-center gap-4 text-white/80 text-sm">
              {#if merchant.location}
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span>{merchant.location}</span>
                </div>
              {/if}
              
              {#if merchant.rating}
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4 text-yellow-300" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  <span class="font-medium">{formatRating(merchant.rating)}</span>
                </div>
              {/if}

              {#if merchant.contact?.phone}
                <div class="flex items-center gap-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                  </svg>
                  <span>{merchant.contact.phone}</span>
                </div>
              {/if}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Products Section -->
    <div class="container py-10 md:py-12">
      <!-- Search -->
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <h2 class="text-xl font-semibold text-[var(--color-text)]">
          Products
        </h2>
        <form onsubmit={handleSearch} class="w-full sm:w-auto">
          <input
            type="text"
            bind:value={productQuery}
            placeholder="Search products..."
            class="input w-full sm:w-72"
          />
        </form>
      </div>

      {#if loadingProducts && products.length === 0}
        <Loading text="Loading products..." />
      {:else if products.length === 0}
        <EmptyState 
          title="No products found" 
          message="This merchant doesn't have any products yet."
          icon="product"
        />
      {:else}
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
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
  {/if}
</div>

