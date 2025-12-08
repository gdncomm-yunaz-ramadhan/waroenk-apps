<script>
  import { cartStore } from '../stores/cart.js';
  import { toastStore } from '../stores/toast.js';

  let { product, index = 0 } = $props();
  let isAdding = $state(false);
  let imageLoaded = $state(false);
  let imageError = $state(false);

  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }

  function handleImageLoad() {
    imageLoaded = true;
  }

  function handleImageError() {
    imageError = true;
    imageLoaded = true;
  }

  async function addToCart(e) {
    e.preventDefault();
    e.stopPropagation();
    
    if (isAdding) return;
    isAdding = true;
    
    try {
      const response = await cartStore.addItem({
        sku: product.sku || product.sub_sku,
        subSku: product.sub_sku || product.sku, // variant sub-SKU for stock validation
        quantity: 1
      });
      
      // Check if response indicates success (AddCartItemResponse has success field)
      if (response?.success === false) {
        toastStore.error(response.message || 'Failed to add to cart');
      } else {
        toastStore.success('Added to cart');
      }
    } catch (e) {
      console.error('Failed to add to cart:', e);
      toastStore.error(e.message || 'Failed to add to cart');
    } finally {
      isAdding = false;
    }
  }
</script>

<a 
  href="/product/{product.sub_sku || product.sku || product.id}"
  class="card card-interactive group animate-fade-in stagger-{(index % 8) + 1}"
  style="opacity: 0;"
>
  <!-- Image Container -->
  <div class="aspect-square overflow-hidden bg-[var(--color-bg)] relative rounded-t-[var(--card-radius)]">
    {#if product.thumbnail && !imageError}
      <!-- Loading skeleton -->
      {#if !imageLoaded}
        <div class="absolute inset-0 skeleton"></div>
      {/if}
      <img 
        src={product.thumbnail} 
        alt={product.title}
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110 {imageLoaded ? 'opacity-100' : 'opacity-0'}"
        loading="lazy"
        onload={handleImageLoad}
        onerror={handleImageError}
      />
    {:else}
      <div class="w-full h-full flex items-center justify-center text-[var(--color-text-muted)]">
        <svg class="w-16 h-16 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </div>
    {/if}
    
    <!-- Stock Badge -->
    {#if product.in_stock === false}
      <div class="absolute top-3 left-3">
        <span class="badge badge-error">Out of Stock</span>
      </div>
    {/if}

    <!-- Quick Add Button - Spotify-style play button */-->
    <button 
      onclick={addToCart}
      disabled={isAdding}
      class="play-button absolute bottom-3 right-3 {isAdding ? 'opacity-100 scale-95' : ''}"
      title="Add to cart"
      aria-label="Add to cart"
    >
      {#if isAdding}
        <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
      {:else}
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
      {/if}
    </button>
  </div>

  <!-- Content -->
  <div class="p-4">
    <!-- Merchant -->
    {#if product.merchant_name}
      <p class="text-xs text-[var(--color-text-muted)] font-medium mb-1.5 truncate">
        {product.merchant_name}
      </p>
    {/if}

    <!-- Title -->
    <h3 class="text-sm font-semibold text-[var(--color-text)] line-clamp-2 mb-3 min-h-[2.5rem] group-hover:text-[var(--color-primary)] transition-colors">
      {product.title}
    </h3>

    <!-- Price -->
    <span class="text-base font-bold text-[var(--color-text)]">
      {formatPrice(product.price)}
    </span>
  </div>
</a>

<style>
  .line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
</style>
