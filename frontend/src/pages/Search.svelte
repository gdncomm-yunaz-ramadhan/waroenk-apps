<script>
  import { onMount, onDestroy } from 'svelte';
  import { searchApi } from '../lib/api/index.js';
  import { ProductCard, MerchantCard, Loading, EmptyState } from '../lib/components/index.js';

  let searchParams = new URLSearchParams(window.location.search);
  let query = $state(searchParams.get('q') || '');
  
  let activeTab = $state('all');
  let products = $state([]);
  let merchants = $state([]);
  let loading = $state(true);
  let error = $state(null);
  let totalProducts = $state(0);
  let totalMerchants = $state(0);
  let totalDocumentsProducts = $state(0);
  let totalDocumentsMerchants = $state(0);
  let elapsedTime = $state(0);
  let nextTokenProducts = $state(null);
  let nextTokenMerchants = $state(null);
  let loadingMore = $state(false);
  const pageSize = 12;
  
  // Lazy load observer
  let productsEndRef = $state(null);
  let merchantsEndRef = $state(null);
  let productsObserver = null;
  let merchantsObserver = null;

  async function search(cursor = null) {
    loading = true;
    error = null;

    try {
      const startTime = performance.now();
      const res = await searchApi.combined({
        query: query,
        size: pageSize,
        cursor: cursor
      });
      const endTime = performance.now();
      elapsedTime = Math.round(endTime - startTime);

      products = res.products?.contents || [];
      merchants = res.merchants?.contents || [];
      totalProducts = res.products?.total_match || 0;
      totalMerchants = res.merchants?.total_match || 0;
      totalDocumentsProducts = res.products?.total_document || totalProducts;
      totalDocumentsMerchants = res.merchants?.total_document || totalMerchants;
      nextTokenProducts = res.products?.next_token || null;
      nextTokenMerchants = res.merchants?.next_token || null;
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  function setupIntersectionObservers() {
    // Observer for products - works on 'all' and 'products' tabs
    if (productsEndRef) {
      productsObserver = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextTokenProducts && !loadingMore) {
            loadMoreProducts();
          }
        },
        { threshold: 0.1, rootMargin: '100px' }
      );
      productsObserver.observe(productsEndRef);
    }
    
    // Observer for merchants - works on 'all' and 'merchants' tabs
    if (merchantsEndRef) {
      merchantsObserver = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextTokenMerchants && !loadingMore) {
            loadMoreMerchants();
          }
        },
        { threshold: 0.1, rootMargin: '100px' }
      );
      merchantsObserver.observe(merchantsEndRef);
    }
  }

  function cleanupObservers() {
    if (productsObserver) {
      productsObserver.disconnect();
      productsObserver = null;
    }
    if (merchantsObserver) {
      merchantsObserver.disconnect();
      merchantsObserver = null;
    }
  }

  onMount(() => {
    if (query) {
      search();
    } else {
      loading = false;
    }
  });
  
  onDestroy(() => {
    cleanupObservers();
  });
  
  // Re-setup observers when refs change
  $effect(() => {
    cleanupObservers();
    if (productsEndRef || merchantsEndRef) {
      setupIntersectionObservers();
    }
  });

  function handleSearch(e) {
    e.preventDefault();
    if (query.trim()) {
      window.history.pushState({}, '', `/search?q=${encodeURIComponent(query.trim())}`);
      search();
    }
  }

  async function loadMoreProducts() {
    if (!nextTokenProducts || loadingMore) return;
    loadingMore = true;
    
    try {
      const res = await searchApi.products({
        query: query,
        size: pageSize,
        cursor: nextTokenProducts
      });
      
      products = [...products, ...(res.contents || [])];
      nextTokenProducts = res.next_token || null;
    } catch (e) {
      console.error('Failed to load more products:', e);
    } finally {
      loadingMore = false;
    }
  }

  async function loadMoreMerchants() {
    if (!nextTokenMerchants || loadingMore) return;
    loadingMore = true;
    
    try {
      const res = await searchApi.merchants({
        query: query,
        size: pageSize,
        cursor: nextTokenMerchants
      });
      
      merchants = [...merchants, ...(res.contents || [])];
      nextTokenMerchants = res.next_token || null;
    } catch (e) {
      console.error('Failed to load more merchants:', e);
    } finally {
      loadingMore = false;
    }
  }
</script>

<svelte:head>
  <title>{query ? `Search: ${query}` : 'Search'} - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <!-- Search Header -->
  <div class="bg-[var(--color-surface)] border-b border-[var(--color-border)]">
    <div class="container py-12">
      <h1 class="text-3xl font-bold text-[var(--color-text)] mb-3 tracking-tight">
        {#if query}
          Search Results
        {:else}
          Search
        {/if}
      </h1>
      {#if query}
        <p class="text-[var(--color-text-muted)] mb-8">
          Showing results for "<span class="text-[var(--color-text)] font-medium">{query}</span>"
        </p>
      {:else}
        <p class="text-[var(--color-text-muted)] mb-8">
          Find products and merchants
        </p>
      {/if}

      <!-- Search Form - Spotify style rounded -->
      <form onsubmit={handleSearch} class="max-w-2xl">
        <div class="relative flex items-center">
          <input
            type="text"
            bind:value={query}
            placeholder="What do you want to find?"
            class="input input-lg pr-28"
          />
          <button 
            type="submit"
            class="absolute right-1.5 btn btn-primary btn-sm"
          >
            Search
          </button>
        </div>
      </form>
    </div>
  </div>

  <!-- Search Info & Tabs -->
  {#if !loading && (products.length > 0 || merchants.length > 0)}
    <div class="bg-white border-b border-[var(--color-border)]">
      <div class="container py-6">
        <!-- Search Result Info -->
        <div class="mb-6 text-sm text-[var(--color-text-muted)]">
          Found <span class="font-semibold text-[var(--color-text)]">{totalProducts + totalMerchants}</span> results 
          out of <span class="font-semibold text-[var(--color-text)]">{totalDocumentsProducts + totalDocumentsMerchants}</span> documents 
          in <span class="font-semibold text-[var(--color-primary)]">{elapsedTime} ms</span>
        </div>
        
        <!-- Tabs -->
        <div class="flex gap-3">
          <button 
            onclick={() => activeTab = 'all'}
            class="chip {activeTab === 'all' ? 'chip-active' : ''}"
          >
            All
            <span class="text-xs opacity-60">({totalProducts + totalMerchants})</span>
          </button>
          <button 
            onclick={() => activeTab = 'products'}
            class="chip {activeTab === 'products' ? 'chip-active' : ''}"
          >
            Products
            <span class="text-xs opacity-60">({totalProducts})</span>
          </button>
          <button 
            onclick={() => activeTab = 'merchants'}
            class="chip {activeTab === 'merchants' ? 'chip-active' : ''}"
          >
            Merchants
            <span class="text-xs opacity-60">({totalMerchants})</span>
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Results -->
  <div class="container py-12">
    {#if loading}
      <Loading text="Searching..." />
    {:else if !query}
      <EmptyState 
        title="Start your search" 
        message="Enter a search term above to discover products and merchants."
        icon="search"
      />
    {:else if error}
      <EmptyState title="Search failed" message={error} icon="error" />
    {:else if products.length === 0 && merchants.length === 0}
      <EmptyState 
        title="No results found" 
        message="Try different keywords or browse our collections."
        icon="search"
      />
    {:else}
      <!-- Products Section -->
      {#if (activeTab === 'all' || activeTab === 'products') && products.length > 0}
        <section class="mb-16">
          <div class="flex items-center justify-between mb-8">
            <h2 class="text-xl font-bold text-[var(--color-text)]">
              {activeTab === 'all' ? 'Products' : `Products (${totalProducts})`}
            </h2>
          </div>
          
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
            {#each products as product, i}
              <ProductCard {product} index={i} />
            {/each}
          </div>

          <!-- Lazy load sentinel for products - always visible when there's more to load -->
          {#if nextTokenProducts && activeTab !== 'merchants'}
            <div bind:this={productsEndRef} class="text-center mt-10 py-4">
              {#if loadingMore}
                <div class="flex items-center justify-center gap-2 text-[var(--color-text-muted)]">
                  <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  <span>Loading more products...</span>
                </div>
              {:else}
                <span class="text-[var(--color-text-muted)] text-sm">Scroll for more products</span>
              {/if}
            </div>
          {/if}
        </section>
      {/if}

      <!-- Merchants Section -->
      {#if (activeTab === 'all' || activeTab === 'merchants') && merchants.length > 0}
        <section>
          <div class="flex items-center justify-between mb-8">
            <h2 class="text-xl font-bold text-[var(--color-text)]">
              {activeTab === 'all' ? 'Merchants' : `Merchants (${totalMerchants})`}
            </h2>
          </div>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
            {#each merchants as merchant, i}
              <MerchantCard {merchant} index={i} />
            {/each}
          </div>

          <!-- Lazy load sentinel for merchants - always visible when there's more to load -->
          {#if nextTokenMerchants && activeTab !== 'products'}
            <div bind:this={merchantsEndRef} class="text-center mt-10 py-4">
              {#if loadingMore}
                <div class="flex items-center justify-center gap-2 text-[var(--color-text-muted)]">
                  <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  <span>Loading more merchants...</span>
                </div>
              {:else}
                <span class="text-[var(--color-text-muted)] text-sm">Scroll for more merchants</span>
              {/if}
            </div>
          {/if}
        </section>
      {/if}
    {/if}
  </div>
</div>
