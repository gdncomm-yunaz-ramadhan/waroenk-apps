<script>
  import { cartCount } from '../stores/cart.js';
  import { isAuthenticated, authStore } from '../stores/auth.js';
  import { navigate } from '../router/index.js';

  let menuOpen = $state(false);

  function logout() {
    authStore.logout();
    navigate('/');
    menuOpen = false;
  }

  function toggleMenu() {
    menuOpen = !menuOpen;
  }

  function closeMenu() {
    menuOpen = false;
  }
</script>

<header class="sticky top-0 z-50 glass border-b border-[var(--color-border-light)]">
  <div class="container">
    <div class="flex items-center justify-between h-16">
      <!-- Logo -->
      <a href="/" class="flex items-center gap-2.5 group" onclick={closeMenu}>
        <svg class="waroenk-logo" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="aurora-header" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop class="grad-a" offset="0%"/>
              <stop class="grad-b" offset="100%"/>
            </linearGradient>
          </defs>
          <!-- Shopping bag body -->
          <path class="bag-body" fill="url(#aurora-header)"
                d="M12 24C12 20 14 18 18 18H46C50 18 52 20 52 24V52C52 56 50 58 46 58H18C14 58 12 56 12 52V24Z"/>
          <!-- Bag handle -->
          <path d="M22 18C22 10 26 6 32 6C38 6 42 10 42 18" stroke="url(#aurora-header)" stroke-width="5" stroke-linecap="round" fill="none"/>
          <!-- W letter -->
          <text x="21" y="46" font-size="22" font-family="system-ui, sans-serif" font-weight="800" fill="white">W</text>
          <!-- Sparkles -->
          <circle class="sparkle sparkle-1" cx="10" cy="12" r="2.5" fill="url(#aurora-header)"/>
          <circle class="sparkle sparkle-2" cx="54" cy="10" r="2" fill="url(#aurora-header)"/>
          <circle class="sparkle sparkle-3" cx="56" cy="44" r="2.2" fill="url(#aurora-header)"/>
        </svg>
        <span class="waroenk-text hidden sm:block">Waroenk</span>
      </a>

      <!-- Navigation (Desktop) -->
      <nav class="hidden md:flex items-center gap-1">
        <a href="/" class="chip">
          Home
        </a>
        <a href="/products" class="chip">
          Products
        </a>
        <a href="/merchants" class="chip">
          Merchants
        </a>
      </nav>

      <!-- Right Actions (Desktop) -->
      <div class="hidden md:flex items-center gap-3">
        <!-- Cart -->
        <a 
          href="/cart" 
          class="relative p-2.5 rounded-full bg-[var(--color-border-light)] hover:bg-[var(--color-border)] transition-colors" 
          aria-label="Shopping Cart"
        >
          <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          {#if $cartCount > 0}
            <span class="absolute -top-1 -right-1 bg-[var(--color-primary)] text-white text-[10px] font-bold rounded-full w-5 h-5 flex items-center justify-center shadow-md">
              {$cartCount > 9 ? '9+' : $cartCount}
            </span>
          {/if}
        </a>

        <!-- Auth -->
        {#if $isAuthenticated}
          <div class="relative group">
            <button class="flex items-center gap-2 p-1.5 rounded-full hover:bg-[var(--color-border-light)] transition-colors" aria-label="User menu">
              <div class="avatar avatar-sm user-avatar-gradient">
                <svg class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
              </div>
            </button>
            <div class="absolute right-0 mt-2 w-48 bg-[var(--color-surface)] rounded-xl shadow-xl border border-[var(--color-border-light)] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all p-2">
              <a href="/profile" class="flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-lg transition-colors font-medium">
                <svg class="w-4 h-4 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                My Profile
              </a>
              <a href="/addresses" class="flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-lg transition-colors font-medium">
                <svg class="w-4 h-4 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                My Addresses
              </a>
              <a href="/orders" class="flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-lg transition-colors font-medium">
                <svg class="w-4 h-4 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                </svg>
                My Orders
              </a>
              <div class="divider !my-1"></div>
              <button onclick={logout} class="w-full flex items-center gap-2 text-left px-3 py-2.5 text-sm text-[var(--color-error)] hover:bg-red-50 rounded-lg transition-colors font-medium">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Log out
              </button>
            </div>
          </div>
        {:else}
          <a href="/login" class="btn btn-ghost btn-sm">Log in</a>
          <a href="/register" class="btn btn-primary btn-sm">Sign up</a>
        {/if}
      </div>

      <!-- Mobile Actions -->
      <div class="flex items-center gap-2 md:hidden">
        <a href="/cart" class="relative p-2.5 rounded-full bg-[var(--color-border-light)]" aria-label="Shopping Cart">
          <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          {#if $cartCount > 0}
            <span class="absolute -top-1 -right-1 bg-[var(--color-primary)] text-white text-[10px] font-bold rounded-full w-5 h-5 flex items-center justify-center">
              {$cartCount > 9 ? '9+' : $cartCount}
            </span>
          {/if}
        </a>

        <button 
          onclick={toggleMenu} 
          class="p-2.5 rounded-full bg-[var(--color-border-light)] transition-colors"
          aria-label="Toggle Menu"
        >
          <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {#if menuOpen}
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            {:else}
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            {/if}
          </svg>
        </button>
      </div>
    </div>
  </div>

  <!-- Mobile Menu -->
  {#if menuOpen}
    <div class="md:hidden bg-[var(--color-surface)] border-t border-[var(--color-border-light)] animate-fade-in">
      <nav class="container py-4 space-y-2">
        <a href="/" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          Home
        </a>
        <a href="/products" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
          Products
        </a>
        <a href="/merchants" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          Merchants
        </a>

        <div class="divider !my-3"></div>
        
        {#if $isAuthenticated}
          <a href="/profile" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            My Profile
          </a>
          <a href="/addresses" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            My Addresses
          </a>
          <a href="/orders" onclick={closeMenu} class="flex items-center gap-3 py-3 px-4 text-[var(--color-text)] hover:bg-[var(--color-border-light)] rounded-xl transition-colors font-medium">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
            </svg>
            My Orders
          </a>
          <button 
            onclick={logout} 
            class="w-full flex items-center gap-3 py-3 px-4 text-[var(--color-error)] hover:bg-red-50 rounded-xl transition-colors font-medium"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Log out
          </button>
        {:else}
          <div class="flex gap-3 pt-2 px-2">
            <a href="/login" onclick={closeMenu} class="btn btn-outline flex-1">Log in</a>
            <a href="/register" onclick={closeMenu} class="btn btn-primary flex-1">Sign up</a>
          </div>
        {/if}
      </nav>
    </div>
  {/if}
</header>

<style>
  /* Waroenk Logo SVG */
  .waroenk-logo {
    width: 48px;
    height: 48px;
    cursor: pointer;
    transition: transform 0.4s ease, filter 0.4s ease;
  }
  
  .waroenk-logo:hover {
    transform: scale(1.08);
    filter: drop-shadow(0 0 12px rgba(45, 212, 191, 0.6));
  }

  /* Waroenk Text with Aurora Gradient */
  .waroenk-text {
    font-size: 1.35rem;
    font-weight: 700;
    letter-spacing: -0.02em;
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
    animation: textGradientShift 3s linear infinite;
  }

  @keyframes textGradientShift {
    0% { background-position: 100% 50%; }
    100% { background-position: -100% 50%; }
  }

  /* Aurora Color Drift for logo */
  @keyframes auroraShiftA {
    0% { stop-color: #2dd4bf; }
    25% { stop-color: #34d399; }
    50% { stop-color: #a78bfa; }
    75% { stop-color: #34d399; }
    100% { stop-color: #2dd4bf; }
  }
  
  @keyframes auroraShiftB {
    0% { stop-color: #a78bfa; }
    25% { stop-color: #2dd4bf; }
    50% { stop-color: #34d399; }
    75% { stop-color: #a78bfa; }
    100% { stop-color: #34d399; }
  }
  
  .grad-a { animation: auroraShiftA 4s ease-in-out infinite; }
  .grad-b { animation: auroraShiftB 4s ease-in-out infinite 0.5s; }

  /* Sparkle animations */
  @keyframes sparklePulse {
    0%, 100% { opacity: 0.4; transform: scale(1); }
    50% { opacity: 1; transform: scale(1.5); }
  }
  
  @keyframes sparkleFloat {
    0%, 100% { opacity: 0.3; transform: scale(0.9) translateY(0); }
    50% { opacity: 1; transform: scale(1.3) translateY(-2px); }
  }
  
  .sparkle { transform-origin: center; }
  .sparkle-1 { animation: sparklePulse 2.5s infinite ease-in-out 0.3s; }
  .sparkle-2 { animation: sparkleFloat 3s infinite ease-in-out 1s; }
  .sparkle-3 { animation: sparklePulse 2.8s infinite ease-in-out 1.8s; }

  /* User Avatar with aurora gradient animation */
  .user-avatar-gradient {
    background: linear-gradient(135deg, #2dd4bf, #a78bfa, #34d399);
    background-size: 300% 300%;
    animation: avatarGradient 4s ease infinite;
    box-shadow: 0 2px 10px rgba(45, 212, 191, 0.4);
  }
  
  @keyframes avatarGradient {
    0% { background-position: 0% 50%; }
    25% { background-position: 100% 0%; }
    50% { background-position: 100% 100%; }
    75% { background-position: 0% 100%; }
    100% { background-position: 0% 50%; }
  }
</style>
