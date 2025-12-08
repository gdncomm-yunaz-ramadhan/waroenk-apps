<script>
  import { productsApi, searchApi } from '../lib/api/index.js';
  import { cartStore } from '../lib/stores/cart.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { isAuthenticated } from '../lib/stores/auth.js';
  import { Loading, EmptyState, ProductCard } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let { id } = $props();
  
  let product = $state(null);
  let selectedVariant = $state(null);
  let relatedProducts = $state([]);
  let loading = $state(true);
  let error = $state(null);
  let quantity = $state(1);
  let selectedMediaIndex = $state(0);
  let isVideoPlaying = $state(false);
  let addingToCart = $state(false);
  let descriptionExpanded = $state(false);

  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }

  // Navigate to search with query
  function navigateToSearch(query) {
    navigate(`/search?q=${encodeURIComponent(query)}`);
  }

  // Check if media is a video
  function isVideo(media) {
    if (!media) return false;
    const type = media.type?.toLowerCase() || '';
    return type.includes('video') || 
           media.url?.match(/\.(mp4|webm|ogg|mov)$/i);
  }

  // Check if media is an image
  function isImage(media) {
    if (!media) return false;
    const type = media.type?.toLowerCase() || '';
    return type.includes('image') || 
           media.url?.match(/\.(jpg|jpeg|png|gif|webp|svg)$/i) ||
           (!isVideo(media) && media.url); // Default to image if not video
  }

  // Filter valid media (images and videos only)
  function getValidMedia(medias) {
    if (!medias) return [];
    return medias
      .filter(m => isImage(m) || isVideo(m))
      .sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0));
  }

  // Load product data - called on mount and when id changes
  async function loadProduct(productId) {
    loading = true;
    error = null;
    product = null;
    selectedVariant = null;
    relatedProducts = [];
    quantity = 1;
    selectedMediaIndex = 0;
    isVideoPlaying = false;

    try {
      // Get product details from the API
      const response = await productsApi.getDetails(productId);
      product = response;
      
      // Select the variant marked as selected, or first one
      if (product?.variants?.length > 0) {
        selectedVariant = product.variants.find(v => v.is_selected) 
          || product.variants[0];
      }

      // Load related products based on category
      const categoryName = product?.category?.name;
      if (categoryName) {
        const res = await searchApi.products({
          query: categoryName,
          size: 4
        }).catch(() => ({ contents: [] }));
        relatedProducts = (res.contents || []).filter(p => 
          p.sku !== product.sku && p.sub_sku !== selectedVariant?.sub_sku
        );
      }
    } catch (e) {
      error = e.message || 'Failed to load product';
    } finally {
      loading = false;
    }
  }

  // React to id changes - load product when id changes
  $effect(() => {
    if (id) {
      loadProduct(id);
    }
  });

  function selectVariant(variant) {
    // Navigate to the new variant URL - this will update the id prop and trigger reload
    if (variant.sub_sku && variant.sub_sku !== id) {
      navigate(`/product/${variant.sub_sku}`);
    }
  }

  async function addToCart() {
    if (!product || !selectedVariant) return;
    
    // Require login
    if (!$isAuthenticated) {
      toastStore.info('Please login to add items to cart');
      navigate(`/login?redirect=/product/${id}`);
      return;
    }

    if (addingToCart) return;
    addingToCart = true;
    
    try {
      const response = await cartStore.addItem({
        sku: product.sku || selectedVariant.sub_sku,
        subSku: selectedVariant.sub_sku, // variant sub-SKU for stock validation
        quantity: quantity
      });
      
      // Check if response indicates success (AddCartItemResponse has success field)
      if (response?.success === false) {
        toastStore.error(response.message || 'Failed to add to cart');
      } else {
        toastStore.success(`Added ${quantity} item(s) to cart`);
      }
    } catch (e) {
      console.error('Failed to add to cart:', e);
      toastStore.error(e.message || 'Failed to add item to cart');
    } finally {
      addingToCart = false;
    }
  }

  // Computed: valid media list
  let validMedia = $derived(getValidMedia(product?.medias));
  
  // Computed: current media
  let currentMedia = $derived(validMedia[selectedMediaIndex] || null);
  
  // Computed: has discount
  let hasDiscount = $derived(product?.price?.discount && product.price.discount > 0);
  
  // Computed: discounted price
  let discountedPrice = $derived(
    hasDiscount 
      ? (product.price.price - product.price.discount) 
      : product?.price?.price
  );

  // Reset video playing state when media changes
  $effect(() => {
    if (selectedMediaIndex >= 0) {
      isVideoPlaying = false;
    }
  });
</script>

<svelte:head>
  <title>{product?.title || 'Product'} - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  {#if loading}
    <Loading text="Loading product..." />
  {:else if error || !product}
    <div class="container py-12">
      <EmptyState 
        title="Product not found" 
        message={error || "This product doesn't exist"} 
        icon="product" 
      />
      <div class="text-center mt-4">
        <a href="/products" class="btn btn-primary">Browse Products</a>
      </div>
    </div>
  {:else}
    <!-- Breadcrumb with Category -->
    <div class="bg-white border-b border-[var(--color-border)]">
      <div class="container py-5">
        <nav class="flex items-center gap-2 text-sm text-[var(--color-text-muted)] flex-wrap">
          <a href="/" class="hover:text-[var(--color-primary)] transition-colors">Home</a>
          <span class="text-[var(--color-border)]">/</span>
          <a href="/products" class="hover:text-[var(--color-primary)] transition-colors">Products</a>
          {#if product.category}
            <span class="text-[var(--color-border)]">/</span>
            <button 
              onclick={() => navigateToSearch(product.category.name)}
              class="hover:text-[var(--color-primary)] transition-colors cursor-pointer"
            >
              {product.category.name}
            </button>
          {/if}
          <span class="text-[var(--color-border)]">/</span>
          <span class="text-[var(--color-text)] font-medium truncate max-w-[200px]">{product.title}</span>
        </nav>
      </div>
    </div>

    <!-- Product Detail -->
    <div class="container py-8 md:py-12">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12">
        
        <!-- Media Carousel -->
        <div class="space-y-4">
          <!-- Main Media Display -->
          <div class="aspect-square bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden relative group">
            {#if currentMedia}
              {#if isVideo(currentMedia)}
                <!-- Video Player -->
                <div class="w-full h-full relative">
                  {#if isVideoPlaying}
                    <video 
                      src={currentMedia.url}
                      controls
                      autoplay
                      class="w-full h-full object-contain bg-black"
                    >
                      <track kind="captions" />
                    </video>
                  {:else}
                    <!-- Video Thumbnail with Play Button -->
                    <div class="w-full h-full flex items-center justify-center bg-gray-900 relative">
                      {#if selectedVariant?.thumbnail}
                        <img 
                          src={selectedVariant.thumbnail} 
                          alt={product.title}
                          class="w-full h-full object-cover opacity-60"
                        />
                      {/if}
                      <button 
                        onclick={() => isVideoPlaying = true}
                        class="absolute inset-0 flex items-center justify-center"
                        aria-label="Play video"
                      >
                        <div class="w-20 h-20 rounded-full bg-white/90 flex items-center justify-center shadow-xl hover:scale-110 transition-transform">
                          <svg class="w-8 h-8 text-[var(--color-text)] ml-1" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M8 5v14l11-7z" />
                          </svg>
                        </div>
                      </button>
                    </div>
                  {/if}
                </div>
              {:else}
                <!-- Image Display -->
                <img 
                  src={currentMedia.url} 
                  alt={currentMedia.alt_text || product.title}
                  class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                />
              {/if}
            {:else if selectedVariant?.thumbnail}
              <img 
                src={selectedVariant.thumbnail} 
                alt={product.title}
                class="w-full h-full object-cover"
              />
            {:else}
              <div class="w-full h-full flex items-center justify-center text-[var(--color-text-muted)]">
                <svg class="w-20 h-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            {/if}

            <!-- Navigation Arrows -->
            {#if validMedia.length > 1}
              <button
                onclick={() => { selectedMediaIndex = selectedMediaIndex === 0 ? validMedia.length - 1 : selectedMediaIndex - 1; isVideoPlaying = false; }}
                class="absolute left-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/90 shadow-lg flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-white hover:scale-110"
                aria-label="Previous image"
              >
                <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <button
                onclick={() => { selectedMediaIndex = selectedMediaIndex === validMedia.length - 1 ? 0 : selectedMediaIndex + 1; isVideoPlaying = false; }}
                class="absolute right-3 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/90 shadow-lg flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-white hover:scale-110"
                aria-label="Next image"
              >
                <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
              
              <!-- Image Counter -->
              <div class="absolute bottom-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-black/50 text-white text-xs font-medium">
                {selectedMediaIndex + 1} / {validMedia.length}
              </div>
            {/if}
          </div>

          <!-- Media Thumbnails -->
          {#if validMedia.length > 1}
            <div class="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
              {#each validMedia as media, i}
                <button
                  onclick={() => { selectedMediaIndex = i; isVideoPlaying = false; }}
                  class="relative w-16 h-16 md:w-20 md:h-20 flex-shrink-0 rounded-xl overflow-hidden border-2 transition-all duration-200 {selectedMediaIndex === i ? 'border-[var(--color-primary)] ring-2 ring-[var(--color-primary)]/20' : 'border-[var(--color-border)] hover:border-[var(--color-text-muted)]'}"
                >
                  {#if isVideo(media)}
                    <!-- Video thumbnail -->
                    <div class="w-full h-full bg-gray-900 flex items-center justify-center">
                      <svg class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M8 5v14l11-7z" />
                      </svg>
                    </div>
                  {:else}
                    <img 
                      src={media.url} 
                      alt={media.alt_text || ''} 
                      class="w-full h-full object-cover"
                    />
                  {/if}
                </button>
              {/each}
            </div>
          {/if}
        </div>

        <!-- Product Info -->
        <div class="space-y-6">
          <!-- Brand & Merchant Row - Two Column Layout -->
          {#if product.brand || product.merchant}
            <div class="grid grid-cols-2 gap-3 mb-2">
              <!-- Brand (Left Column) -->
              {#if product.brand}
                <button 
                  onclick={() => navigateToSearch(product.brand.name)}
                  class="flex items-center gap-2.5 px-3 py-2.5 bg-gradient-to-r from-[var(--color-bg)] to-white border border-[var(--color-border)] rounded-xl hover:border-[var(--color-primary)] hover:shadow-md transition-all duration-200 cursor-pointer"
                >
                  {#if product.brand.icon_url}
                    <img src={product.brand.icon_url} alt="" class="w-8 h-8 rounded-full object-cover ring-2 ring-white shadow-sm flex-shrink-0" />
                  {:else}
                    <div class="w-8 h-8 rounded-full bg-[var(--color-primary)]/10 flex items-center justify-center flex-shrink-0">
                      <svg class="w-4 h-4 text-[var(--color-primary)]" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                      </svg>
                    </div>
                  {/if}
                  <span class="text-sm font-bold text-[var(--color-text)] truncate">{product.brand.name}</span>
                </button>
              {:else}
                <div></div>
              {/if}

              <!-- Merchant (Right Column) -->
              {#if product.merchant}
                <a 
                  href="/merchant/{product.merchant.code}"
                  class="flex items-center gap-2.5 px-3 py-2.5 bg-gradient-to-br from-white to-[var(--color-bg)] border border-[var(--color-border)] rounded-xl hover:border-[var(--color-primary)] hover:shadow-md transition-all duration-200 group"
                >
                  <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--color-primary)]/15 to-[var(--color-primary)]/5 flex items-center justify-center overflow-hidden ring-1 ring-[var(--color-border)] flex-shrink-0">
                    {#if product.merchant.icon_url}
                      <img src={product.merchant.icon_url} alt="" class="w-full h-full object-cover" />
                    {:else}
                      <svg class="w-4 h-4 text-[var(--color-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                    {/if}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-1">
                      <span class="text-sm font-bold text-[var(--color-text)] group-hover:text-[var(--color-primary)] transition-colors truncate">{product.merchant.name}</span>
                      <svg class="w-4 h-4 text-[var(--color-text-muted)] group-hover:text-[var(--color-primary)] group-hover:translate-x-0.5 transition-all flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                    <div class="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
                      {#if product.merchant.rating}
                        <span class="flex items-center gap-0.5">
                          <svg class="w-3 h-3 text-amber-500" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                          <span class="font-medium text-amber-700">{product.merchant.rating.toFixed(1)}</span>
                        </span>
                      {/if}
                      <span class="text-[var(--color-text-muted)]">Verified Seller</span>
                      {#if product.merchant.location}
                        <span class="flex items-center gap-0.5">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                          </svg>
                          {product.merchant.location}
                        </span>
                      {/if}
                    </div>
                  </div>
                </a>
              {:else}
                <div></div>
              {/if}
            </div>
          {/if}

          <!-- Title -->
          <h1 class="text-2xl md:text-3xl font-bold text-[var(--color-text)] leading-tight tracking-tight">
            {product.title}
          </h1>

          <!-- Price Section -->
          <div class="flex items-baseline gap-3 flex-wrap">
            <span class="text-3xl font-bold text-[var(--color-primary)]">
              {formatPrice(discountedPrice)}
            </span>
            {#if hasDiscount}
              <span class="text-lg text-[var(--color-text-muted)] line-through">
                {formatPrice(product.price.price)}
              </span>
              <span class="badge badge-success">
                Save {formatPrice(product.price.discount)}
              </span>
            {/if}
          </div>

          <!-- Stock Status -->
          <div class="flex items-center gap-3">
            {#if product.stock?.has_stock}
              <span class="badge badge-success">
                <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
                In Stock ({product.stock.total_stock} available)
              </span>
            {:else}
              <span class="badge badge-error">
                <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
                Out of Stock
              </span>
            {/if}
            <span class="text-xs text-[var(--color-text-muted)]">
              SKU: {selectedVariant?.sub_sku || product.sku}
            </span>
          </div>

          <!-- Variant Selector -->
          {#if product.variants && product.variants.length > 1}
            <div class="space-y-3">
              <h3 class="text-sm font-semibold text-[var(--color-text)]">Select Variant</h3>
              <div class="flex flex-wrap gap-2">
                {#each product.variants as variant}
                  <button
                    onclick={() => selectVariant(variant)}
                    disabled={!variant.has_stock}
                    class="group relative flex items-center gap-2 px-4 py-2.5 border-2 rounded-xl transition-all duration-200 {selectedVariant?.sub_sku === variant.sub_sku ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5 ring-2 ring-[var(--color-primary)]/20' : 'border-[var(--color-border)] hover:border-[var(--color-text-muted)]'} {!variant.has_stock ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}"
                  >
                    {#if variant.thumbnail}
                      <img 
                        src={variant.thumbnail} 
                        alt=""
                        class="w-8 h-8 rounded-lg object-cover"
                      />
                    {/if}
                    <span class="text-sm font-medium text-[var(--color-text)]">
                      {variant.sub_sku}
                    </span>
                    {#if selectedVariant?.sub_sku === variant.sub_sku}
                      <svg class="w-4 h-4 text-[var(--color-primary)]" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    {/if}
                    {#if !variant.has_stock}
                      <span class="absolute -top-1 -right-1 px-1.5 py-0.5 bg-red-500 text-white text-[10px] font-bold rounded">
                        OUT
                      </span>
                    {/if}
                  </button>
                {/each}
              </div>
            </div>
          {/if}

          <!-- Description with See More/Less -->
          {#if product.short_description}
            <div class="space-y-2">
              <h3 class="text-sm font-semibold text-[var(--color-text)]">Description</h3>
              <div class="text-sm text-[var(--color-text-light)] leading-relaxed">
                {#if product.short_description.length > 200}
                  {#if descriptionExpanded}
                    <p>{product.short_description}</p>
                    <button 
                      onclick={() => descriptionExpanded = false}
                      class="text-[var(--color-primary)] font-medium hover:underline mt-2 inline-flex items-center gap-1"
                    >
                      See less
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                      </svg>
                    </button>
                  {:else}
                    <p>{product.short_description.slice(0, 200)}...</p>
                    <button 
                      onclick={() => descriptionExpanded = true}
                      class="text-[var(--color-primary)] font-medium hover:underline mt-2 inline-flex items-center gap-1"
                    >
                      See more
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                  {/if}
                {:else}
                  <p>{product.short_description}</p>
                {/if}
              </div>
            </div>
          {/if}

          <!-- Product Attributes -->
          {#if product.attributes && Object.keys(product.attributes).length > 0}
            <div class="space-y-3">
              <h3 class="text-sm font-semibold text-[var(--color-text)]">Specifications</h3>
              <div class="grid grid-cols-2 gap-2">
                {#each Object.entries(product.attributes) as [key, value]}
                  <button 
                    onclick={() => navigateToSearch(`${key}:${value}`)}
                    class="flex items-center gap-2 p-3 bg-[var(--color-bg)] rounded-xl hover:bg-[var(--color-border)] transition-colors text-left cursor-pointer group"
                  >
                    <div class="flex-1 min-w-0">
                      <div class="text-[10px] uppercase tracking-wider text-[var(--color-text-muted)] mb-0.5">{key}</div>
                      <div class="text-sm font-medium text-[var(--color-text)] truncate">{value}</div>
                    </div>
                    <svg class="w-4 h-4 text-[var(--color-text-muted)] opacity-0 group-hover:opacity-100 transition-opacity" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </button>
                {/each}
              </div>
            </div>
          {/if}

          <!-- Tags -->
          {#if product.tags && product.tags.length > 0}
            <div class="space-y-3">
              <h3 class="text-sm font-semibold text-[var(--color-text)]">Tags</h3>
              <div class="flex flex-wrap gap-2">
                {#each product.tags as tag}
                  <button 
                    onclick={() => navigateToSearch(tag)}
                    class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-[var(--color-border)] text-[var(--color-text-light)] rounded-lg text-xs font-medium hover:border-[var(--color-primary)] hover:bg-[var(--color-primary)]/5 hover:text-[var(--color-primary)] transition-all duration-200 cursor-pointer group"
                  >
                    <span class="text-[var(--color-primary)] group-hover:text-[var(--color-primary)]">#</span>
                    {tag}
                  </button>
                {/each}
              </div>
            </div>
          {/if}

        </div>
      </div>
    </div>

    <!-- Related Products -->
    {#if relatedProducts.length > 0}
      <div class="bg-white py-10 mt-6 border-t border-[var(--color-border)] pb-28 md:pb-10">
        <div class="container">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl font-bold text-[var(--color-text)]">
              You might also like
            </h2>
            {#if product.category}
              <button 
                onclick={() => navigateToSearch(product.category.name)}
                class="btn btn-outline btn-sm"
              >
                View all
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            {/if}
          </div>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 md:gap-6">
            {#each relatedProducts as relatedProduct, i}
              <ProductCard product={relatedProduct} index={i} />
            {/each}
          </div>
        </div>
      </div>
    {:else}
      <!-- Spacer for sticky bottom bar when no related products -->
      <div class="h-24 md:h-0"></div>
    {/if}

    <!-- Sticky Add to Cart Bar -->
    <div class="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-md border-t border-[var(--color-border)] shadow-[0_-4px_20px_rgba(0,0,0,0.1)] z-40 animate-fade-in safe-area-bottom">
      <div class="container pt-5 pb-6 md:pt-6 md:pb-8 px-6 md:px-10">
        <div class="flex items-center justify-between gap-8 md:gap-10">
          <!-- Left: Total Price (Price × Qty) -->
          <div class="flex-1 min-w-0">
            <p class="text-xs text-[var(--color-text-muted)] mb-0.5">Total</p>
            <p class="text-xl md:text-2xl font-bold text-[var(--color-primary)] truncate">
              {formatPrice(discountedPrice * quantity)}
            </p>
          </div>

          <!-- Center: Quantity Selector -->
          <div class="flex items-center border border-[var(--color-border)] rounded-full bg-[var(--color-bg)] flex-shrink-0">
            <button 
              onclick={() => quantity > 1 && (quantity -= 1)}
              class="w-11 h-11 md:w-12 md:h-12 flex items-center justify-center hover:bg-white transition-colors rounded-full"
              aria-label="Decrease quantity"
            >
              <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
              </svg>
            </button>
            <span class="w-10 md:w-12 text-center font-bold text-base">{quantity}</span>
            <button 
              onclick={() => quantity += 1}
              class="w-11 h-11 md:w-12 md:h-12 flex items-center justify-center hover:bg-white transition-colors rounded-full"
              aria-label="Increase quantity"
            >
              <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </button>
          </div>

          <!-- Right: Add to Cart Button (Compact) -->
          <button 
            onclick={addToCart}
            disabled={!product.stock?.has_stock || addingToCart}
            class="flex items-center gap-2.5 px-6 md:px-8 py-3.5 md:py-4 bg-[var(--color-primary)] hover:bg-[var(--color-primary-dark)] text-white font-semibold text-sm md:text-base rounded-full transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-[var(--color-primary)]/25 hover:shadow-xl hover:shadow-[var(--color-primary)]/30 flex-shrink-0"
          >
            {#if addingToCart}
              <svg class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            {:else}
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <span class="hidden sm:inline">
                {#if product.stock?.has_stock}
                  {$isAuthenticated ? 'Add' : 'Login'}
                {:else}
                  Sold Out
                {/if}
              </span>
            {/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .scrollbar-hide::-webkit-scrollbar {
    display: none;
  }
  .scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
  .safe-area-bottom {
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
</style>
