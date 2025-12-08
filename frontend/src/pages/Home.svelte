<script>
  import { onMount, onDestroy } from 'svelte';
  import { searchApi, categoriesApi, brandsApi } from '../lib/api/index.js';
  import { ProductCard, MerchantCard, CategoryCard, Loading, EmptyState } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';
  import { isAuthenticated } from '../lib/stores/auth.js';

  // Search state
  let searchQuery = $state('');
  let searchFocused = $state(false);

  let categories = $state([]);
  let brands = $state([]);
  let featuredProducts = $state([]);
  let featuredMerchants = $state([]);
  let totalProducts = $state(0);
  let totalMerchants = $state(0);
  let totalBrands = $state(0);
  let loading = $state(true);
  let error = $state(null);
  
  // Hero carousel state - switches between stats and featured products
  let heroCarouselIndex = $state(0);
  let heroCarouselInterval = null;
  let heroRandomProducts = $state([]); // 3 random products for carousel
  const HERO_CAROUSEL_ITEMS = 4; // 0 = stats, 1-3 = products
  
  // Aurora positions - autonomous movement only
  let aurora1X = $state(30);
  let aurora1Y = $state(40);
  let aurora2X = $state(70);
  let aurora2Y = $state(60);
  let aurora3X = $state(50);
  let aurora3Y = $state(30);
  
  // Stars array for twinkling effect
  let stars = $state([]);
  
  // Parallax scroll position
  let scrollY = $state(0);
  let parallaxAurora = $derived(scrollY * 0.3);
  let parallaxContent = $derived(scrollY * 0.15);
  let parallaxStars = $derived(scrollY * 0.5);
  
  // Window width for responsive layout
  let windowWidth = $state(typeof window !== 'undefined' ? window.innerWidth : 1024);
  let isDesktop = $derived(windowWidth >= 1024);
  
  // Animation frame for smooth movement
  let animationFrame = null;
  let time = 0;
  
  // Generate random stars
  function generateStars(count = 80) {
    const newStars = [];
    for (let i = 0; i < count; i++) {
      newStars.push({
        id: i,
        x: Math.random() * 100,
        y: Math.random() * 100,
        size: Math.random() * 2.5 + 0.5, // 0.5-3px
        opacity: Math.random() * 0.7 + 0.3, // 0.3-1
        animationDelay: Math.random() * 5, // 0-5s delay
        animationDuration: Math.random() * 3 + 2, // 2-5s duration
        twinkleType: Math.floor(Math.random() * 3) // 0, 1, or 2 for different animations
      });
    }
    return newStars;
  }
  
  // Smooth autonomous aurora movement using sine waves
  function animateAurora() {
    time += 0.006;
    
    // Create organic, flowing movement patterns
    aurora1X = 30 + Math.sin(time * 0.5) * 25 + Math.sin(time * 1.1) * 10;
    aurora1Y = 35 + Math.cos(time * 0.4) * 20 + Math.cos(time * 0.9) * 8;
    
    aurora2X = 65 + Math.sin(time * 0.45 + 2) * 22 + Math.cos(time * 1.0) * 12;
    aurora2Y = 55 + Math.cos(time * 0.6 + 1) * 18 + Math.sin(time * 0.7) * 10;
    
    aurora3X = 50 + Math.sin(time * 0.4 + 4) * 30 + Math.cos(time * 1.2) * 8;
    aurora3Y = 25 + Math.cos(time * 0.55 + 3) * 15 + Math.sin(time * 0.8) * 12;
    
    animationFrame = requestAnimationFrame(animateAurora);
  }
  
  // Handle scroll for parallax
  function handleScroll() {
    scrollY = window.scrollY;
  }
  
  // Handle resize for responsive layout
  function handleResize() {
    windowWidth = window.innerWidth;
  }

  onMount(async () => {
    // Generate stars
    stars = generateStars(80);
    
    // Start aurora animation
    animateAurora();
    
    // Add scroll listener for parallax
    window.addEventListener('scroll', handleScroll, { passive: true });
    
    // Add resize listener for responsive layout
    window.addEventListener('resize', handleResize, { passive: true });
    windowWidth = window.innerWidth;
    
    try {
      const [categoriesRes, brandsRes, searchRes] = await Promise.all([
        categoriesApi.filter({ size: 8 }).catch(() => ({ data: [] })),
        brandsApi.filter({ size: 8 }).catch(() => ({ data: [] })),
        searchApi.combined({ query: '', size: 8 }).catch(() => ({ products: { contents: [], total_document: 0 }, merchants: { contents: [], total_document: 0 } }))
      ]);

      categories = categoriesRes.data || [];
      brands = brandsRes.data || [];
      featuredProducts = searchRes.products?.contents || [];
      featuredMerchants = searchRes.merchants?.contents || [];
      totalProducts = searchRes.products?.total_document || searchRes.products?.total_match || featuredProducts.length;
      totalMerchants = searchRes.merchants?.total_document || searchRes.merchants?.total_match || featuredMerchants.length;
      totalBrands = brandsRes.total || brands.length;
      
      // Pick random products for hero carousel
      heroRandomProducts = pickRandomProducts(featuredProducts);
      
      // Start hero carousel after data loads
      startHeroCarousel();
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  });
  
  onDestroy(() => {
    if (animationFrame) {
      cancelAnimationFrame(animationFrame);
    }
    if (typeof window !== 'undefined') {
      window.removeEventListener('scroll', handleScroll);
      window.removeEventListener('resize', handleResize);
    }
    // Stop hero carousel
    stopHeroCarousel();
  });

  function handleCategoryClick(category) {
    navigate(`/search?q=${encodeURIComponent(category.name)}`);
  }

  function handleBrandClick(brand) {
    navigate(`/search?q=${encodeURIComponent(brand.name)}`);
  }

  function handleSearch(e) {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  }

  function formatCount(num) {
    if (num >= 1000) {
      return (num / 1000).toFixed(num >= 10000 ? 0 : 1) + 'K+';
    }
    return num.toString() + '+';
  }

  // Pick 3 random products for hero carousel
  function pickRandomProducts(products) {
    if (!products || products.length === 0) return [];
    const shuffled = [...products].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(3, shuffled.length));
  }

  // Start hero carousel animation
  function startHeroCarousel() {
    if (heroCarouselInterval) return;
    heroCarouselInterval = setInterval(() => {
      // Total slides = 1 (stats) + heroRandomProducts.length (up to 3)
      const totalSlides = 1 + heroRandomProducts.length;
      heroCarouselIndex = (heroCarouselIndex + 1) % totalSlides;
    }, 4000); // Switch every 4 seconds
  }

  // Stop hero carousel
  function stopHeroCarousel() {
    if (heroCarouselInterval) {
      clearInterval(heroCarouselInterval);
      heroCarouselInterval = null;
    }
  }

  // Format price for display
  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }
</script>

<svelte:head>
  <title>Waroenk - Your Trusted Marketplace</title>
</svelte:head>

<!-- Hero Section - Aurora animation with twinkling stars and parallax -->
<section class="relative overflow-hidden aurora-hero">
  <!-- Aurora gradient layers - flowing northern lights effect with parallax -->
  <div 
    class="aurora-container"
    style="transform: translateY({parallaxAurora}px);"
  >
    <!-- Primary aurora layer (teal/cyan) -->
    <div 
      class="aurora-layer aurora-1"
      style="--aurora-x: {aurora1X}%; --aurora-y: {aurora1Y}%;"
    ></div>
    
    <!-- Secondary aurora layer (emerald/green) -->
    <div 
      class="aurora-layer aurora-2"
      style="--aurora-x: {aurora2X}%; --aurora-y: {aurora2Y}%;"
    ></div>
    
    <!-- Tertiary aurora layer (soft pink/violet) -->
    <div 
      class="aurora-layer aurora-3"
      style="--aurora-x: {aurora3X}%; --aurora-y: {aurora3Y}%;"
    ></div>
    
    <!-- Subtle ambient glow -->
    <div class="aurora-ambient"></div>
  </div>
  
  <!-- Twinkling Stars with parallax -->
  <div 
    class="stars-container"
    style="transform: translateY({parallaxStars}px);"
  >
    {#each stars as star (star.id)}
      <div 
        class="star twinkle-{star.twinkleType}"
        style="
          left: {star.x}%;
          top: {star.y}%;
          width: {star.size}px;
          height: {star.size}px;
          --star-opacity: {star.opacity};
          animation-delay: {star.animationDelay}s;
          animation-duration: {star.animationDuration}s;
        "
      ></div>
    {/each}
  </div>
  
  <!-- Hero Content with parallax -->
  <div 
    class="container py-20 md:py-28 lg:py-36 relative z-10"
    style="transform: translateY({parallaxContent}px);"
  >
    <div class="hero-layout-grid">
      <!-- Left side: Main content -->
      <div class="hero-left">
        <div class="animate-fade-in" style="opacity: 0;">
          <span class="badge aurora-badge mb-8">
            ✨ Your marketplace
          </span>
        </div>
        
        <h1 class="text-5xl md:text-6xl lg:text-8xl font-bold text-[var(--color-text)] mb-10 tracking-tight animate-fade-in stagger-1 hero-title" style="opacity: 0;">
          Find products<br/>
          you'll <span class="aurora-text-gradient">love</span>
        </h1>
        
        <p class="text-xl md:text-2xl text-[var(--color-text-light)] mb-14 max-w-2xl leading-relaxed animate-fade-in stagger-2" style="opacity: 0;">
          Discover thousands of quality products from trusted local merchants. Shop smarter, support local.
        </p>

        <!-- AI Search Bar -->
        <form onsubmit={handleSearch} class="max-w-2xl animate-fade-in stagger-3" style="opacity: 0;">
          <div class="aurora-search-container {searchFocused ? 'focused' : ''}">
            <div class="aurora-search-inner">
              <!-- Sparkle/Star Icon -->
              <div class="aurora-search-icon">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L13.09 8.26L18 6L15.74 10.91L22 12L15.74 13.09L18 18L13.09 15.74L12 22L10.91 15.74L6 18L8.26 13.09L2 12L8.26 10.91L6 6L10.91 8.26L12 2Z"/>
                </svg>
              </div>
              
              <input
                type="text"
                bind:value={searchQuery}
                onfocus={() => searchFocused = true}
                onblur={() => searchFocused = false}
                placeholder="Search products, merchants, brands..."
                class="aurora-search-input"
              />
              
              <button type="submit" class="aurora-search-btn">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>
            </div>
          </div>
        </form>

        <!-- Action Buttons -->
        <div class="flex flex-wrap gap-5 mt-8 animate-fade-in stagger-4" style="opacity: 0;">
          <a href="/products" class="btn aurora-btn-primary btn-lg">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
            Browse Products
          </a>
          <a href="/merchants" class="btn btn-outline btn-lg">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            View Merchants
          </a>
        </div>
      </div>
      
      <!-- Right side: Hero Carousel (Stats + Featured Products) -->
      <div class="hero-right animate-stats-slide">
        <div class="hero-carousel-container">
          <div class="hero-carousel-wrapper">
            <!-- Slide 0: Marketplace Stats -->
            <div class="hero-carousel-slide {heroCarouselIndex === 0 ? 'active' : ''}">
              <div class="carousel-slide-label">
                <span class="slide-badge">📊 Marketplace Stats</span>
              </div>
              <div class="stats-grid-three">
                <!-- Products Stat Card -->
                <div class="stat-card stat-card-primary">
                  <div class="stat-card-icon">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                    </svg>
                  </div>
                  <p class="stat-card-value">{loading ? '...' : formatCount(totalProducts)}</p>
                  <p class="stat-card-label">Products</p>
                  <div class="stat-card-glow"></div>
                </div>
                
                <!-- Merchants Stat Card -->
                <div class="stat-card stat-card-secondary">
                  <div class="stat-card-icon">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                    </svg>
                  </div>
                  <p class="stat-card-value">{loading ? '...' : formatCount(totalMerchants)}</p>
                  <p class="stat-card-label">Merchants</p>
                  <div class="stat-card-glow"></div>
                </div>
                
                <!-- Brands Stat Card -->
                <div class="stat-card stat-card-accent">
                  <div class="stat-card-icon">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
                    </svg>
                  </div>
                  <p class="stat-card-value">{loading ? '...' : formatCount(totalBrands)}</p>
                  <p class="stat-card-label">Brands</p>
                  <div class="stat-card-glow"></div>
                </div>
              </div>
            </div>
            
            <!-- Slides 1-3: Featured Products -->
            {#each heroRandomProducts as product, i}
              <div class="hero-carousel-slide {heroCarouselIndex === i + 1 ? 'active' : ''}">
                <div class="carousel-slide-label">
                  <span class="slide-badge slide-badge-featured">⭐ Featured Product</span>
                </div>
                <a href="/product/{product.sub_sku || product.sku || product.id}" class="featured-product-card">
                  <div class="featured-product-image">
                    {#if product.thumbnail}
                      <img 
                        src={product.thumbnail} 
                        alt={product.title}
                        class="w-full h-full object-cover"
                        loading="lazy"
                      />
                    {:else}
                      <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-teal-500/10 to-emerald-500/10">
                        <svg class="w-12 h-12 text-white/20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                    {/if}
                    {#if product.category_name}
                      <span class="featured-product-category">{product.category_name}</span>
                    {/if}
                  </div>
                  <div class="featured-product-info">
                    <h3 class="featured-product-title">{product.title}</h3>
                    <div class="featured-product-price">
                      <span class="price-discounted">{formatPrice(product.price)}</span>
                    </div>
                    {#if product.merchant_name}
                      <div class="featured-product-merchant">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                        </svg>
                        <span>{product.merchant_name}</span>
                      </div>
                    {/if}
                  </div>
                  <div class="featured-product-cta">
                    <span>View Product</span>
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </a>
              </div>
            {/each}
          </div>
          
          <!-- Carousel Indicators -->
          <div class="hero-carousel-indicators">
            <!-- Stats indicator -->
            <button 
              class="carousel-indicator {heroCarouselIndex === 0 ? 'active' : ''}"
              onclick={() => heroCarouselIndex = 0}
              aria-label="Go to stats"
            >
              <div class="indicator-progress"></div>
            </button>
            <!-- Product indicators -->
            {#each heroRandomProducts as _, i}
              <button 
                class="carousel-indicator {heroCarouselIndex === i + 1 ? 'active' : ''}"
                onclick={() => heroCarouselIndex = i + 1}
                aria-label="Go to product {i + 1}"
              >
                <div class="indicator-progress"></div>
              </button>
            {/each}
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- Dynamic Wave Shape at Bottom -->
  <div class="hero-wave-container">
    <svg 
      class="hero-wave" 
      viewBox="0 0 1440 180" 
      preserveAspectRatio="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id="waveGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style="stop-color:rgba(45, 212, 191, 0.3)" />
          <stop offset="50%" style="stop-color:rgba(167, 139, 250, 0.2)" />
          <stop offset="100%" style="stop-color:rgba(52, 211, 153, 0.3)" />
        </linearGradient>
      </defs>
      <!-- Background wave layer -->
      <path 
        class="wave-path wave-back"
        fill="url(#waveGradient)"
        d="M0,100 C150,160 350,40 600,80 C850,120 1050,40 1200,90 C1350,140 1400,100 1440,100 L1440,180 L0,180 Z"
      />
      <!-- Middle wave layer -->
      <path 
        class="wave-path wave-mid"
        fill="rgba(8, 8, 16, 0.5)"
        d="M0,120 C200,80 400,150 720,110 C1040,70 1200,140 1440,100 L1440,180 L0,180 Z"
      />
      <!-- Front wave layer (main surface color) -->
      <path 
        class="wave-path wave-front"
        fill="var(--color-surface, #f8fafc)"
        d="M0,140 C180,100 360,160 540,130 C720,100 900,150 1080,120 C1260,90 1360,130 1440,120 L1440,180 L0,180 Z"
      />
    </svg>
  </div>
</section>

<style>
  /* Aurora Hero Styles - Dark background with vivid aurora */
  .aurora-hero {
    background: #080810 !important;
    min-height: 100vh;
    position: relative;
    isolation: isolate;
    margin-top: -64px;
    padding-top: 64px;
    padding-bottom: 120px;
  }
  
  @media (max-width: 768px) {
    .aurora-hero {
      min-height: auto;
      padding-bottom: 100px;
    }
  }
  
  .aurora-hero::before {
    content: '';
    position: absolute;
    inset: 0;
    background: 
      radial-gradient(ellipse 120% 80% at 50% 120%, rgba(6, 78, 59, 0.15) 0%, transparent 50%),
      radial-gradient(ellipse 80% 60% at 80% 0%, rgba(30, 64, 175, 0.1) 0%, transparent 40%);
    z-index: 0;
  }
  
  .aurora-container {
    position: absolute;
    inset: 0;
    overflow: hidden;
    z-index: 1;
    will-change: transform;
  }
  
  /* Hero Two Column Layout */
  :global(.hero-layout-grid) {
    display: grid !important;
    grid-template-columns: 1fr 400px !important;
    gap: 2rem !important;
    align-items: start !important;
    width: 100% !important;
  }
  
  :global(.hero-left) {
    width: 100%;
  }
  
  :global(.hero-right) {
    width: 400px;
  }
  
  .hero-title {
    line-height: 1.05;
    letter-spacing: -0.03em;
  }
  
  /* Mobile: Stack in single column */
  @media (max-width: 1023px) {
    :global(.hero-layout-grid) {
      display: flex !important;
      flex-direction: column !important;
      gap: 2.5rem !important;
    }
    
    :global(.hero-right) {
      width: 100% !important;
      max-width: 480px !important;
      margin: 0 auto !important;
    }
  }
  
  /* Stats section slide-up animation */
  .animate-stats-slide {
    animation: statsSlideUp 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.5s both;
  }
  
  @keyframes statsSlideUp {
    0% {
      opacity: 0;
      transform: translateY(60px);
    }
    100% {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  /* Stats Container */
  .hero-stats-container {
    width: 100%;
    max-width: 450px;
  }
  
  .stats-header {
    display: flex;
    justify-content: center;
    margin-bottom: 1.25rem;
  }
  
  /* Hero Carousel Container */
  .hero-carousel-container {
    position: relative;
    width: 100%;
    max-width: 480px;
    margin: 0 auto;
  }
  
  @media (min-width: 1024px) {
    .hero-carousel-container {
      max-width: 440px;
    }
  }
  
  .hero-carousel-wrapper {
    position: relative;
    height: 340px;
    overflow: hidden;
  }
  
  @media (min-width: 640px) {
    .hero-carousel-wrapper {
      height: 320px;
    }
  }
  
  @media (min-width: 1024px) {
    .hero-carousel-wrapper {
      height: 360px;
    }
  }
  
  /* Carousel Slide - Bottom to Top animation */
  .hero-carousel-slide {
    position: absolute;
    inset: 0;
    opacity: 0;
    transform: translateY(40px);
    transition: all 0.5s cubic-bezier(0.22, 1, 0.36, 1);
    pointer-events: none;
    display: flex;
    flex-direction: column;
  }
  
  .hero-carousel-slide.active {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }
  
  .hero-carousel-slide.exit-up {
    opacity: 0;
    transform: translateY(-40px);
  }
  
  .hero-carousel-slide.enter-down {
    opacity: 0;
    transform: translateY(40px);
  }
  
  /* Carousel Slide Label */
  .carousel-slide-label {
    margin-bottom: 0.75rem;
    display: flex;
    justify-content: center;
    flex-shrink: 0;
  }
  
  @media (min-width: 1024px) {
    .carousel-slide-label {
      margin-bottom: 1rem;
    }
  }
  
  .slide-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
    padding: 0.375rem 0.75rem;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 500px;
    font-size: 0.7rem;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
  }
  
  @media (min-width: 1024px) {
    .slide-badge {
      padding: 0.5rem 1rem;
      font-size: 0.75rem;
      gap: 0.5rem;
    }
  }
  
  .slide-badge-featured {
    background: linear-gradient(135deg, rgba(251, 191, 36, 0.15) 0%, rgba(245, 158, 11, 0.1) 100%);
    border-color: rgba(251, 191, 36, 0.25);
    color: #fcd34d;
  }
  
  /* Carousel Indicators */
  .hero-carousel-indicators {
    display: flex;
    justify-content: center;
    gap: 0.5rem;
    margin-top: 1rem;
    padding-top: 0.25rem;
  }
  
  @media (min-width: 1024px) {
    .hero-carousel-indicators {
      gap: 0.625rem;
      margin-top: 1.25rem;
    }
  }
  
  .carousel-indicator {
    position: relative;
    width: 2rem;
    height: 4px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 3px;
    cursor: pointer;
    overflow: hidden;
    transition: all 0.3s ease;
    border: none;
    padding: 0;
  }
  
  @media (min-width: 1024px) {
    .carousel-indicator {
      width: 2.5rem;
      height: 5px;
    }
  }
  
  .carousel-indicator:hover {
    background: rgba(255, 255, 255, 0.25);
  }
  
  .carousel-indicator.active {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .indicator-progress {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 0;
    background: linear-gradient(90deg, #2dd4bf, #34d399);
    border-radius: 3px;
  }
  
  .carousel-indicator:not(.active) .indicator-progress {
    width: 0 !important;
    transition: none;
  }
  
  .carousel-indicator.active .indicator-progress {
    animation: progressFill 4s linear forwards;
  }
  
  @keyframes progressFill {
    from { width: 0; }
    to { width: 100%; }
  }
  
  /* Featured Product Card */
  .featured-product-card {
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 1.25rem;
    overflow: hidden;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    height: 100%;
    max-height: 280px;
  }
  
  @media (min-width: 1024px) {
    .featured-product-card {
      max-height: 300px;
    }
  }
  
  .featured-product-card:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.2);
    transform: translateY(-4px);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  }
  
  .featured-product-image {
    position: relative;
    width: 100%;
    height: 120px;
    flex-shrink: 0;
    overflow: hidden;
  }
  
  @media (min-width: 640px) {
    .featured-product-image {
      height: 130px;
    }
  }
  
  @media (min-width: 1024px) {
    .featured-product-image {
      height: 140px;
    }
  }
  
  .featured-product-image img {
    transition: transform 0.4s ease;
  }
  
  .featured-product-card:hover .featured-product-image img {
    transform: scale(1.05);
  }
  
  .featured-product-category {
    position: absolute;
    top: 0.625rem;
    left: 0.625rem;
    padding: 0.25rem 0.5rem;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    border-radius: 6px;
    font-size: 0.6rem;
    font-weight: 600;
    color: white;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  
  .featured-product-info {
    padding: 0.875rem 1rem;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }
  
  .featured-product-title {
    font-size: 0.85rem;
    font-weight: 700;
    color: white;
    line-height: 1.25;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  
  .featured-product-price {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
  }
  
  .price-discounted {
    font-size: 1rem;
    font-weight: 800;
    background: linear-gradient(135deg, #2dd4bf, #34d399);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .price-original {
    font-size: 0.7rem;
    color: rgba(255, 255, 255, 0.4);
    text-decoration: line-through;
  }
  
  .featured-product-merchant {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    font-size: 0.65rem;
    color: rgba(255, 255, 255, 0.5);
    margin-top: auto;
  }
  
  .featured-product-cta {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.375rem;
    padding: 0.625rem;
    background: linear-gradient(135deg, rgba(45, 212, 191, 0.15) 0%, rgba(52, 211, 153, 0.15) 100%);
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    font-size: 0.7rem;
    font-weight: 600;
    color: #2dd4bf;
    transition: all 0.3s ease;
    flex-shrink: 0;
  }
  
  .featured-product-card:hover .featured-product-cta {
    background: linear-gradient(135deg, rgba(45, 212, 191, 0.25) 0%, rgba(52, 211, 153, 0.25) 100%);
    color: #5eead4;
  }
  
  .featured-product-cta svg {
    transition: transform 0.3s ease;
  }
  
  .featured-product-card:hover .featured-product-cta svg {
    transform: translateX(4px);
  }
  
  /* 3-column stats grid for Products, Merchants, Brands */
  .stats-grid-three {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 0.75rem;
    width: 100%;
    max-width: 100%;
  }
  
  @media (min-width: 640px) {
    .stats-grid-three {
      gap: 0.875rem;
    }
  }
  
  @media (min-width: 1024px) {
    .stats-grid-three {
      gap: 1rem;
    }
  }
  
  /* Stat Cards */
  .stat-card {
    position: relative;
    padding: 1rem 0.75rem;
    border-radius: 1rem;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    text-align: center;
    overflow: hidden;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  @media (min-width: 640px) {
    .stat-card {
      padding: 1.25rem 0.875rem;
      border-radius: 1.125rem;
    }
  }
  
  @media (min-width: 1024px) {
    .stat-card {
      padding: 1.5rem 1rem;
      border-radius: 1.25rem;
    }
  }
  
  .stat-card:hover {
    transform: translateY(-4px) scale(1.02);
    border-color: rgba(255, 255, 255, 0.25);
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 16px 32px -8px rgba(0, 0, 0, 0.4);
  }
  
  .stat-card-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    margin: 0 auto 0.625rem;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.08);
    opacity: 1;
  }
  
  @media (min-width: 640px) {
    .stat-card-icon {
      width: 44px;
      height: 44px;
      margin-bottom: 0.75rem;
    }
  }
  
  @media (min-width: 1024px) {
    .stat-card-icon {
      width: 48px;
      height: 48px;
      margin-bottom: 0.875rem;
      border-radius: 14px;
    }
  }
  
  .stat-card-primary .stat-card-icon {
    color: #2dd4bf;
    background: rgba(45, 212, 191, 0.12);
  }
  
  .stat-card-secondary .stat-card-icon {
    color: #34d399;
    background: rgba(52, 211, 153, 0.12);
  }
  
  .stat-card-accent .stat-card-icon {
    color: #a78bfa;
    background: rgba(167, 139, 250, 0.12);
  }
  
  .stat-card-value {
    font-size: 1.5rem;
    font-weight: 800;
    color: #ffffff;
    line-height: 1;
    margin-bottom: 0.375rem;
    background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.9) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  @media (min-width: 640px) {
    .stat-card-value {
      font-size: 1.75rem;
      margin-bottom: 0.5rem;
    }
  }
  
  @media (min-width: 1024px) {
    .stat-card-value {
      font-size: 2rem;
    }
  }
  
  .stat-card-label {
    font-size: 0.625rem;
    color: rgba(255, 255, 255, 0.6);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }
  
  @media (min-width: 640px) {
    .stat-card-label {
      font-size: 0.7rem;
    }
  }
  
  @media (min-width: 1024px) {
    .stat-card-label {
      font-size: 0.75rem;
      letter-spacing: 0.08em;
    }
  }
  
  .stat-card-glow {
    position: absolute;
    inset: 0;
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
  }
  
  .stat-card:hover .stat-card-glow {
    opacity: 1;
  }
  
  .stat-card-primary .stat-card-glow {
    background: radial-gradient(circle at center, rgba(45, 212, 191, 0.2) 0%, transparent 70%);
  }
  
  .stat-card-secondary .stat-card-glow {
    background: radial-gradient(circle at center, rgba(52, 211, 153, 0.2) 0%, transparent 70%);
  }
  
  .stat-card-accent .stat-card-glow {
    background: radial-gradient(circle at center, rgba(167, 139, 250, 0.2) 0%, transparent 70%);
  }
  
  /* Wave shape at bottom */
  .hero-wave-container {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    height: 180px;
    z-index: 5;
    overflow: hidden;
  }
  
  .hero-wave {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }
  
  .wave-path {
    transition: d 0.3s ease;
  }
  
  /* Wave animations */
  .wave-back {
    animation: waveBackAnim 8s ease-in-out infinite;
  }
  
  .wave-mid {
    animation: waveMidAnim 6s ease-in-out infinite;
  }
  
  .wave-front {
    animation: waveFrontAnim 4s ease-in-out infinite;
  }
  
  @keyframes waveBackAnim {
    0%, 100% {
      d: path("M0,100 C150,160 350,40 600,80 C850,120 1050,40 1200,90 C1350,140 1400,100 1440,100 L1440,180 L0,180 Z");
    }
    50% {
      d: path("M0,90 C200,50 400,130 650,100 C900,70 1100,130 1250,80 C1380,40 1420,90 1440,90 L1440,180 L0,180 Z");
    }
  }
  
  @keyframes waveMidAnim {
    0%, 100% {
      d: path("M0,120 C200,80 400,150 720,110 C1040,70 1200,140 1440,100 L1440,180 L0,180 Z");
    }
    50% {
      d: path("M0,100 C250,140 500,70 780,120 C1060,170 1250,90 1440,120 L1440,180 L0,180 Z");
    }
  }
  
  @keyframes waveFrontAnim {
    0%, 100% {
      d: path("M0,140 C180,100 360,160 540,130 C720,100 900,150 1080,120 C1260,90 1360,130 1440,120 L1440,180 L0,180 Z");
    }
    50% {
      d: path("M0,130 C200,160 380,100 560,140 C740,180 920,110 1100,140 C1280,170 1380,120 1440,130 L1440,180 L0,180 Z");
    }
  }
  
  /* Aurora gradient layers - vibrant northern lights */
  .aurora-layer {
    position: absolute;
    inset: -50%;
    filter: blur(60px);
    will-change: transform, opacity;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  .aurora-focused {
    transition: all 0.1s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  /* Teal/Cyan aurora - primary wave (brightest) */
  .aurora-1 {
    background: radial-gradient(
      ellipse 70% 45% at var(--aurora-x, 30%) var(--aurora-y, 40%),
      rgba(45, 212, 191, 0.7) 0%,
      rgba(20, 184, 166, 0.5) 25%,
      rgba(6, 182, 212, 0.3) 45%,
      transparent 65%
    );
    opacity: 0.8;
    animation: auroraWave1 10s ease-in-out infinite;
  }
  
  /* Emerald/Green aurora - secondary wave */
  .aurora-2 {
    background: radial-gradient(
      ellipse 55% 60% at var(--aurora-x, 70%) var(--aurora-y, 60%),
      rgba(52, 211, 153, 0.65) 0%,
      rgba(16, 185, 129, 0.45) 30%,
      rgba(110, 231, 183, 0.2) 50%,
      transparent 65%
    );
    opacity: 0.75;
    animation: auroraWave2 14s ease-in-out infinite;
  }
  
  /* Violet/Pink aurora - accent wave */
  .aurora-3 {
    background: radial-gradient(
      ellipse 60% 50% at var(--aurora-x, 50%) var(--aurora-y, 30%),
      rgba(167, 139, 250, 0.55) 0%,
      rgba(139, 92, 246, 0.35) 30%,
      rgba(192, 132, 252, 0.2) 50%,
      transparent 60%
    );
    opacity: 0.7;
    animation: auroraWave3 16s ease-in-out infinite;
  }
  
  /* Ambient glow layer - subtle base lighting */
  .aurora-ambient {
    position: absolute;
    inset: 0;
    background: 
      radial-gradient(ellipse at 10% 90%, rgba(45, 212, 191, 0.12) 0%, transparent 45%),
      radial-gradient(ellipse at 90% 10%, rgba(52, 211, 153, 0.1) 0%, transparent 45%),
      radial-gradient(ellipse at 50% 50%, rgba(139, 92, 246, 0.08) 0%, transparent 55%),
      radial-gradient(ellipse at 30% 20%, rgba(236, 72, 153, 0.06) 0%, transparent 40%);
    animation: ambientPulse 6s ease-in-out infinite;
  }
  
  @keyframes auroraWave1 {
    0%, 100% { 
      opacity: 0.75;
      transform: scale(1) rotate(0deg) translateY(0);
    }
    33% {
      opacity: 0.9;
      transform: scale(1.08) rotate(1deg) translateY(-2%);
    }
    66% { 
      opacity: 0.8;
      transform: scale(1.02) rotate(-0.5deg) translateY(1%);
    }
  }
  
  @keyframes auroraWave2 {
    0%, 100% { 
      opacity: 0.7;
      transform: scale(1) rotate(0deg);
    }
    25% { 
      opacity: 0.85;
      transform: scale(1.1) rotate(-1.5deg);
    }
    50% {
      opacity: 0.75;
      transform: scale(0.98) rotate(0.5deg);
    }
    75% {
      opacity: 0.8;
      transform: scale(1.05) rotate(-0.5deg);
    }
  }
  
  @keyframes auroraWave3 {
    0%, 100% { 
      opacity: 0.65;
      transform: scale(1) rotate(0deg) translateX(0);
    }
    50% { 
      opacity: 0.8;
      transform: scale(1.06) rotate(2deg) translateX(2%);
    }
  }
  
  @keyframes ambientPulse {
    0%, 100% { opacity: 0.9; }
    50% { opacity: 1; }
  }
  
  /* Aurora badge - glass effect with aurora tint */
  .aurora-badge {
    background: rgba(45, 212, 191, 0.15);
    color: #5eead4;
    border: 1px solid rgba(94, 234, 212, 0.25);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }
  
  /* Aurora text gradient - animated shimmer */
  .aurora-text-gradient {
    background: linear-gradient(
      90deg, 
      #2dd4bf 0%, 
      #34d399 25%, 
      #a78bfa 50%, 
      #34d399 75%, 
      #2dd4bf 100%
    );
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    background-size: 200% 100%;
    animation: gradientShift 3s linear infinite;
  }
  
  @keyframes gradientShift {
    0% { background-position: 100% 50%; }
    100% { background-position: -100% 50%; }
  }
  
  /* Aurora primary button - glowing effect */
  .aurora-btn-primary {
    background: linear-gradient(135deg, #14b8a6 0%, #10b981 100%);
    color: white;
    box-shadow: 
      0 4px 20px rgba(20, 184, 166, 0.4),
      0 0 40px rgba(20, 184, 166, 0.2);
    border: none;
    position: relative;
    overflow: hidden;
  }
  
  .aurora-btn-primary::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%, rgba(255,255,255,0.1) 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }
  
  .aurora-btn-primary:hover {
    background: linear-gradient(135deg, #2dd4bf 0%, #34d399 100%);
    box-shadow: 
      0 6px 30px rgba(45, 212, 191, 0.5),
      0 0 60px rgba(45, 212, 191, 0.3);
    transform: scale(1.02) translateY(-1px);
  }
  
  .aurora-btn-primary:hover::before {
    opacity: 1;
  }
  
  /* Aurora hero text - crisp white on dark */
  .aurora-hero h1 {
    color: #ffffff;
    text-shadow: 0 2px 20px rgba(0, 0, 0, 0.3);
  }
  
  .aurora-hero p {
    color: rgba(255, 255, 255, 0.75);
  }
  
  /* Stats styling - legacy support */
  .stat-item {
    position: relative;
  }
  
  .stat-number-aurora {
    background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.85) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-shadow: none;
  }
  
  /* Container with parallax will-change */
  .aurora-hero .container {
    will-change: transform;
  }
  
  /* Override text colors in aurora hero for Tailwind classes */
  .aurora-hero .text-\[var\(--color-text\)\] {
    color: #ffffff !important;
  }
  
  .aurora-hero .text-\[var\(--color-text-light\)\] {
    color: rgba(255, 255, 255, 0.75) !important;
  }
  
  .aurora-hero .text-\[var\(--color-text-muted\)\] {
    color: rgba(255, 255, 255, 0.55) !important;
  }
  
  /* Outline button in aurora section - glass morphism */
  .aurora-hero .btn-outline {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: white;
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
  }
  
  .aurora-hero .btn-outline:hover {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.35);
    box-shadow: 0 4px 20px rgba(255, 255, 255, 0.1);
  }
  
  /* Container z-index fix */
  .aurora-hero .container {
    position: relative;
    z-index: 3;
  }
  
  /* Ensure all hero text is visible on dark bg */
  .aurora-hero h1,
  .aurora-hero .text-4xl,
  .aurora-hero .text-3xl {
    color: #ffffff !important;
  }
  
  /* ===== Twinkling Stars ===== */
  .stars-container {
    position: absolute;
    inset: 0;
    z-index: 2;
    overflow: hidden;
    pointer-events: none;
    will-change: transform;
  }
  
  .star {
    position: absolute;
    background: white;
    border-radius: 50%;
    opacity: var(--star-opacity, 0.6);
    box-shadow: 
      0 0 3px rgba(255, 255, 255, 0.8),
      0 0 6px rgba(255, 255, 255, 0.4);
  }
  
  /* Different twinkle animations for variety */
  .twinkle-0 {
    animation: twinkle1 ease-in-out infinite;
  }
  
  .twinkle-1 {
    animation: twinkle2 ease-in-out infinite;
  }
  
  .twinkle-2 {
    animation: twinkle3 ease-in-out infinite;
  }
  
  @keyframes twinkle1 {
    0%, 100% { 
      opacity: var(--star-opacity, 0.6);
      transform: scale(1);
    }
    50% { 
      opacity: 0.1;
      transform: scale(0.8);
    }
  }
  
  @keyframes twinkle2 {
    0%, 100% { 
      opacity: var(--star-opacity, 0.6);
      transform: scale(1);
      box-shadow: 0 0 3px rgba(255, 255, 255, 0.8), 0 0 6px rgba(255, 255, 255, 0.4);
    }
    25% {
      opacity: 1;
      transform: scale(1.2);
      box-shadow: 0 0 6px rgba(255, 255, 255, 1), 0 0 12px rgba(255, 255, 255, 0.6), 0 0 20px rgba(255, 255, 255, 0.3);
    }
    50% { 
      opacity: 0.15;
      transform: scale(0.6);
      box-shadow: 0 0 2px rgba(255, 255, 255, 0.5);
    }
    75% {
      opacity: 0.8;
      transform: scale(1.1);
    }
  }
  
  @keyframes twinkle3 {
    0%, 100% { 
      opacity: var(--star-opacity, 0.6);
      transform: scale(1) rotate(0deg);
    }
    33% { 
      opacity: 0.2;
      transform: scale(0.7) rotate(90deg);
    }
    66% { 
      opacity: 0.9;
      transform: scale(1.3) rotate(180deg);
    }
  }
  
  /* Some larger stars have a subtle color tint */
  .star:nth-child(5n) {
    box-shadow: 
      0 0 4px rgba(94, 234, 212, 0.9),
      0 0 8px rgba(94, 234, 212, 0.5),
      0 0 12px rgba(94, 234, 212, 0.3);
  }
  
  .star:nth-child(7n) {
    box-shadow: 
      0 0 4px rgba(167, 139, 250, 0.9),
      0 0 8px rgba(167, 139, 250, 0.5),
      0 0 12px rgba(167, 139, 250, 0.3);
  }
  
  .star:nth-child(11n) {
    box-shadow: 
      0 0 4px rgba(52, 211, 153, 0.9),
      0 0 8px rgba(52, 211, 153, 0.5),
      0 0 12px rgba(52, 211, 153, 0.3);
  }
  
  /* ===== Aurora AI Search Bar ===== */
  .aurora-search-container {
    position: relative;
    border-radius: 500px;
    padding: 2px;
    background: linear-gradient(
      90deg,
      rgba(45, 212, 191, 0.4) 0%,
      rgba(52, 211, 153, 0.4) 25%,
      rgba(167, 139, 250, 0.4) 50%,
      rgba(52, 211, 153, 0.4) 75%,
      rgba(45, 212, 191, 0.4) 100%
    );
    background-size: 300% 100%;
    animation: auroraGradientFlow 4s ease infinite;
    transition: all 0.3s ease;
  }
  
  .aurora-search-container.focused {
    padding: 2.5px;
    background: linear-gradient(
      90deg,
      rgba(45, 212, 191, 0.8) 0%,
      rgba(52, 211, 153, 0.8) 25%,
      rgba(167, 139, 250, 0.8) 50%,
      rgba(52, 211, 153, 0.8) 75%,
      rgba(45, 212, 191, 0.8) 100%
    );
    background-size: 300% 100%;
    box-shadow: 
      0 0 20px rgba(45, 212, 191, 0.3),
      0 0 40px rgba(167, 139, 250, 0.2);
  }
  
  @keyframes auroraGradientFlow {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
  }
  
  .aurora-search-inner {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    background: rgba(8, 8, 16, 0.9);
    border-radius: 500px;
    padding: 0.875rem 1rem;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
  }
  
  .aurora-search-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #a78bfa;
    animation: starPulse 2s ease-in-out infinite;
  }
  
  @keyframes starPulse {
    0%, 100% { 
      color: #a78bfa;
      filter: drop-shadow(0 0 4px rgba(167, 139, 250, 0.5));
    }
    33% { 
      color: #2dd4bf;
      filter: drop-shadow(0 0 4px rgba(45, 212, 191, 0.5));
    }
    66% { 
      color: #34d399;
      filter: drop-shadow(0 0 4px rgba(52, 211, 153, 0.5));
    }
  }
  
  .aurora-search-input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    font-family: var(--font-sans);
    font-size: 1rem;
    color: white;
    padding: 0;
  }
  
  .aurora-search-input::placeholder {
    color: rgba(255, 255, 255, 0.45);
  }
  
  .aurora-search-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.5rem;
    background: linear-gradient(135deg, rgba(45, 212, 191, 0.3) 0%, rgba(167, 139, 250, 0.3) 100%);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 50%;
    color: white;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  
  .aurora-search-btn:hover {
    background: linear-gradient(135deg, rgba(45, 212, 191, 0.5) 0%, rgba(167, 139, 250, 0.5) 100%);
    border-color: rgba(255, 255, 255, 0.3);
    transform: scale(1.05);
  }
</style>

{#if loading}
  <section class="section">
    <div class="container">
      <!-- Skeleton for Categories -->
      <div class="mb-12">
        <div class="skeleton skeleton-text w-32 h-6 mb-6"></div>
        <div class="grid grid-cols-4 sm:grid-cols-8 gap-3">
          {#each Array(8) as _}
            <div class="skeleton h-20 rounded-xl"></div>
          {/each}
        </div>
      </div>
      
      <!-- Skeleton for Products -->
      <div>
        <div class="skeleton skeleton-text w-40 h-6 mb-6"></div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          {#each Array(8) as _}
            <div class="skeleton h-64 rounded-xl"></div>
          {/each}
        </div>
      </div>
    </div>
  </section>
{:else if error}
  <section class="section">
    <div class="container">
      <EmptyState title="Something went wrong" message={error} icon="error" />
    </div>
  </section>
{:else}
  <!-- Categories Section -->
  {#if categories.length > 0}
    <section class="section bg-[var(--color-surface)]">
      <div class="container">
        <div class="flex items-center justify-between mb-8">
          <h2 class="section-title">
            Browse by Category
          </h2>
          <a href="/categories" class="btn btn-outline btn-sm hidden sm:flex">
            View all
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>
        <div class="grid grid-cols-4 sm:grid-cols-8 gap-3">
          {#each categories as category, i}
            <button 
              onclick={() => handleCategoryClick(category)}
              class="group flex flex-col items-center p-4 rounded-2xl bg-[var(--color-surface)] hover:bg-[var(--color-primary)]/5 border border-transparent hover:border-[var(--color-primary)]/20 transition-all duration-300 animate-fade-in stagger-{(i % 8) + 1}"
              style="opacity: 0;"
            >
              <!-- Icon -->
              <div class="w-12 h-12 rounded-2xl overflow-hidden bg-[var(--color-border-light)] mb-3 transition-transform duration-300 group-hover:scale-110">
                {#if category.icon_url}
                  <img 
                    src={category.icon_url} 
                    alt={category.name}
                    class="w-full h-full object-cover"
                    loading="lazy"
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
            </button>
          {/each}
        </div>
        <div class="mt-8 text-center sm:hidden">
          <a href="/categories" class="btn btn-outline">
            View all categories
          </a>
        </div>
      </div>
    </section>
  {/if}

  <!-- Brands Section -->
  {#if brands.length > 0}
    <section class="section">
      <div class="container">
        <div class="flex items-center justify-between mb-8">
          <div>
            <span class="section-subtitle">Shop by</span>
            <h2 class="section-title mt-1">
              Popular Brands
            </h2>
          </div>
          <a href="/brands" class="btn btn-outline btn-sm hidden sm:flex">
            View all
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>
        <div class="grid grid-cols-4 sm:grid-cols-8 gap-3">
          {#each brands as brand, i}
            <button 
              onclick={() => handleBrandClick(brand)}
              class="group flex flex-col items-center p-4 rounded-2xl bg-[var(--color-surface)] hover:bg-[var(--color-primary)]/5 border border-transparent hover:border-[var(--color-primary)]/20 transition-all duration-300 animate-fade-in stagger-{(i % 8) + 1}"
              style="opacity: 0;"
            >
              <!-- Logo -->
              <div class="w-12 h-12 rounded-2xl overflow-hidden bg-[var(--color-border-light)] mb-3 transition-transform duration-300 group-hover:scale-110">
                {#if brand.logo_url}
                  <img 
                    src={brand.logo_url} 
                    alt={brand.name}
                    class="w-full h-full object-contain p-1"
                    loading="lazy"
                  />
                {:else}
                  <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[var(--color-primary)]/10 to-[var(--color-primary)]/5">
                    <span class="text-lg font-bold text-[var(--color-primary)]">
                      {brand.name?.charAt(0)?.toUpperCase() || 'B'}
                    </span>
                  </div>
                {/if}
              </div>
              <!-- Name -->
              <h3 class="text-xs font-semibold text-[var(--color-text)] text-center group-hover:text-[var(--color-primary)] transition-colors truncate w-full">
                {brand.name}
              </h3>
            </button>
          {/each}
        </div>
        <div class="mt-8 text-center sm:hidden">
          <a href="/brands" class="btn btn-outline">
            View all brands
          </a>
        </div>
      </div>
    </section>
  {/if}

  <!-- Featured Products Section -->
  {#if featuredProducts.length > 0}
    <section class="section bg-[var(--color-surface)]">
      <div class="container">
        <div class="flex items-center justify-between mb-8">
          <div>
            <span class="section-subtitle">Popular right now</span>
            <h2 class="section-title mt-1">
              Featured Products
            </h2>
          </div>
          <a href="/products" class="btn btn-outline btn-sm hidden sm:flex">
            View all
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
          {#each featuredProducts as product, i}
            <ProductCard {product} index={i} />
          {/each}
        </div>
        <div class="mt-8 text-center sm:hidden">
          <a href="/products" class="btn btn-outline">
            View all products
          </a>
        </div>
      </div>
    </section>
  {/if}

  <!-- Featured Merchants Section -->
  {#if featuredMerchants.length > 0}
    <section class="section">
      <div class="container">
        <div class="flex items-center justify-between mb-8">
          <div>
            <span class="section-subtitle">Trusted sellers</span>
            <h2 class="section-title mt-1">
              Top Merchants
            </h2>
          </div>
          <a href="/merchants" class="btn btn-outline btn-sm hidden sm:flex">
            View all
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
          {#each featuredMerchants as merchant, i}
            <MerchantCard {merchant} index={i} />
          {/each}
        </div>
        <div class="mt-8 text-center sm:hidden">
          <a href="/merchants" class="btn btn-outline">
            View all merchants
          </a>
        </div>
      </div>
    </section>
  {/if}

  <!-- CTA Section - Clean & Minimal (only show when not authenticated) -->
  {#if !$isAuthenticated}
    <section class="section-lg bg-gradient-to-br from-[var(--color-text)] to-[#282828]">
      <div class="container text-center">
        <div class="max-w-2xl mx-auto">
          <h2 class="text-2xl md:text-4xl font-bold text-white mb-4 tracking-tight">
            Ready to start shopping?
          </h2>
          <p class="text-[#a7a7a7] mb-8 text-lg">
            Create an account to track your orders, save favorites, and get personalized recommendations.
          </p>
          <div class="flex flex-wrap justify-center gap-4">
            <a href="/register" class="btn btn-primary btn-lg">
              Create free account
            </a>
            <a href="/login" class="btn btn-lg" style="background: rgba(255,255,255,0.1); color: white; border: 1px solid rgba(255,255,255,0.2);">
              Log in
            </a>
          </div>
        </div>
      </div>
    </section>
  {/if}
{/if}

