<script>
  let { 
    currentPage = 1, 
    totalPages = 1, 
    onPageChange = () => {},
    hasMore = false,
    nextToken = null
  } = $props();

  function goToPage(page) {
    if (page >= 1 && page <= totalPages) {
      onPageChange(page);
    }
  }

  function loadMore() {
    if (nextToken) {
      onPageChange(currentPage + 1, nextToken);
    }
  }
</script>

{#if totalPages > 1 || hasMore}
  <div class="flex items-center justify-center gap-2 mt-8">
    <!-- If using traditional pagination -->
    {#if totalPages > 1}
      <button 
        onclick={() => goToPage(currentPage - 1)}
        disabled={currentPage <= 1}
        class="px-4 py-2 rounded-lg border border-[var(--color-sand)] hover:bg-[var(--color-sand)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Go to previous page"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>

      {#each Array.from({length: Math.min(5, totalPages)}, (_, i) => {
        const start = Math.max(1, currentPage - 2);
        return Math.min(start + i, totalPages);
      }).filter((v, i, a) => a.indexOf(v) === i) as page}
        <button 
          onclick={() => goToPage(page)}
          class="w-10 h-10 rounded-lg font-medium transition-colors {page === currentPage ? 'bg-[var(--color-terracotta)] text-white' : 'border border-[var(--color-sand)] hover:bg-[var(--color-sand)]'}"
          aria-label="Go to page {page}"
          aria-current={page === currentPage ? 'page' : undefined}
        >
          {page}
        </button>
      {/each}

      <button 
        onclick={() => goToPage(currentPage + 1)}
        disabled={currentPage >= totalPages}
        class="px-4 py-2 rounded-lg border border-[var(--color-sand)] hover:bg-[var(--color-sand)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Go to next page"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </button>
    {/if}

    <!-- If using cursor-based pagination -->
    {#if hasMore && nextToken}
      <button 
        onclick={loadMore}
        class="btn btn-primary"
      >
        Load More
      </button>
    {/if}
  </div>
{/if}

