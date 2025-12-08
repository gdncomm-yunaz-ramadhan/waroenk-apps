<script>
  import { onMount, onDestroy } from 'svelte';
  import { searchApi } from '../lib/api/index.js';
  import { MerchantCard, Loading, EmptyState } from '../lib/components/index.js';

  let searchParams = new URLSearchParams(window.location.search);
  
  let query = $state(searchParams.get('q') || '');
  let merchants = $state([]);
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

  async function loadMerchants(cursor = null) {
    if (cursor) {
      loadingMore = true;
    } else {
      loading = true;
    }
    error = null;
    
    try {
      const startTime = performance.now();
      const res = await searchApi.merchants({
        query: query,
        size: pageSize,
        cursor: cursor
      });
      const endTime = performance.now();
      
      if (!cursor) {
        elapsedTime = Math.round(endTime - startTime);
      }
      
      if (cursor) {
        merchants = [...merchants, ...(res.contents || [])];
      } else {
        merchants = res.contents || [];
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

  function setupIntersectionObserver() {
    if (loadMoreRef) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && nextToken && !loadingMore && !loading) {
            loadMerchants(nextToken);
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
    loadMerchants();
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

  function handleSearch(e) {
    e.preventDefault();
    window.history.pushState({}, '', `/merchants?q=${encodeURIComponent(query)}`);
    loadMerchants();
  }
</script>

<svelte:head>
  <title>Merchants - Waroenk</title>
</svelte:head>

<div class="min-h-screen">
  <!-- Page Header -->
  <div class="bg-white border-b border-[var(--color-sand)]">
    <div class="container py-8">
      <h1 class="font-display text-3xl font-semibold text-[var(--color-charcoal)] mb-6">
        {query ? `Merchants matching "${query}"` : 'All Merchants'}
      </h1>

      <!-- Search -->
      <form onsubmit={handleSearch} class="max-w-xl">
        <input
          type="text"
          bind:value={query}
          placeholder="Search merchants..."
          class="input"
        />
      </form>

      <!-- Results Count with Search Info -->
      {#if !loading && totalMatch > 0}
        <p class="text-sm text-[var(--color-text-muted)] mt-4">
          Found <span class="font-semibold text-[var(--color-text)]">{totalMatch}</span> merchants 
          out of <span class="font-semibold text-[var(--color-text)]">{totalDocument}</span> documents 
          in <span class="font-semibold text-[var(--color-primary)]">{elapsedTime} ms</span>
        </p>
      {/if}
    </div>
  </div>

  <!-- Merchants Grid -->
  <div class="container py-8">
    {#if loading && merchants.length === 0}
      <Loading text="Loading merchants..." />
    {:else if error}
      <EmptyState title="Failed to load merchants" message={error} icon="error" />
    {:else if merchants.length === 0}
      <EmptyState 
        title="No merchants found" 
        message="Try adjusting your search."
        icon="search"
      />
    {:else}
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
        {#each merchants as merchant, i}
          <MerchantCard {merchant} index={i} />
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

