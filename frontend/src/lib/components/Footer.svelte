<script>
  import { isAuthenticated } from '../stores/auth.js';
  
  const currentYear = new Date().getFullYear();
</script>

<footer class="footer-bg text-white mt-auto">
  <div class="container py-12">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8 lg:gap-12">
      <!-- Brand -->
      <div class="col-span-2 md:col-span-1">
        <div class="flex items-center gap-2.5 mb-4">
          <svg class="waroenk-logo-footer" viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="aurora-footer" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop class="grad-a-footer" offset="0%"/>
                <stop class="grad-b-footer" offset="100%"/>
              </linearGradient>
            </defs>
            <!-- Shopping bag body -->
            <path fill="url(#aurora-footer)"
                  d="M12 24C12 20 14 18 18 18H46C50 18 52 20 52 24V52C52 56 50 58 46 58H18C14 58 12 56 12 52V24Z"/>
            <!-- Bag handle -->
            <path d="M22 18C22 10 26 6 32 6C38 6 42 10 42 18" stroke="url(#aurora-footer)" stroke-width="5" stroke-linecap="round" fill="none"/>
            <!-- W letter -->
            <text x="21" y="46" font-size="22" font-family="system-ui, sans-serif" font-weight="800" fill="white">W</text>
            <!-- Sparkles -->
            <circle class="sparkle-footer sparkle-f1" cx="10" cy="12" r="2.5" fill="url(#aurora-footer)"/>
            <circle class="sparkle-footer sparkle-f2" cx="54" cy="10" r="2" fill="url(#aurora-footer)"/>
            <circle class="sparkle-footer sparkle-f3" cx="56" cy="44" r="2.2" fill="url(#aurora-footer)"/>
          </svg>
          <span class="waroenk-text-footer">Waroenk</span>
        </div>
        <p class="text-sm text-[var(--color-text-muted)] leading-relaxed max-w-xs">
          Your trusted marketplace for quality products from local merchants.
        </p>
        
        <!-- Social Links -->
        <div class="flex gap-3 mt-6">
          <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" class="footer-social w-10 h-10 rounded-full flex items-center justify-center transition-colors" aria-label="Follow us on Instagram">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
            </svg>
          </a>
          <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" class="footer-social w-10 h-10 rounded-full flex items-center justify-center transition-colors" aria-label="Follow us on Twitter">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M23.953 4.57a10 10 0 01-2.825.775 4.958 4.958 0 002.163-2.723c-.951.555-2.005.959-3.127 1.184a4.92 4.92 0 00-8.384 4.482C7.69 8.095 4.067 6.13 1.64 3.162a4.822 4.822 0 00-.666 2.475c0 1.71.87 3.213 2.188 4.096a4.904 4.904 0 01-2.228-.616v.06a4.923 4.923 0 003.946 4.827 4.996 4.996 0 01-2.212.085 4.936 4.936 0 004.604 3.417 9.867 9.867 0 01-6.102 2.105c-.39 0-.779-.023-1.17-.067a13.995 13.995 0 007.557 2.209c9.053 0 13.998-7.496 13.998-13.985 0-.21 0-.42-.015-.63A9.935 9.935 0 0024 4.59z"/>
            </svg>
          </a>
          <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" class="footer-social w-10 h-10 rounded-full flex items-center justify-center transition-colors" aria-label="Follow us on Facebook">
            <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
            </svg>
          </a>
        </div>
      </div>

      <!-- Shop -->
      <div>
        <h4 class="font-semibold text-sm mb-4">Shop</h4>
        <ul class="space-y-3">
          <li><a href="/products" class="footer-text text-sm transition-colors">All Products</a></li>
          <li><a href="/merchants" class="footer-text text-sm transition-colors">Merchants</a></li>
          <li><a href="/categories" class="footer-text text-sm transition-colors">Categories</a></li>
          <li><a href="/search" class="footer-text text-sm transition-colors">Search</a></li>
        </ul>
      </div>

      <!-- Account -->
      <div>
        <h4 class="font-semibold text-sm mb-4">Account</h4>
        <ul class="space-y-3">
          {#if !$isAuthenticated}
            <li><a href="/login" class="footer-text text-sm transition-colors">Log in</a></li>
            <li><a href="/register" class="footer-text text-sm transition-colors">Sign up</a></li>
          {:else}
            <li><a href="/profile" class="footer-text text-sm transition-colors">Profile</a></li>
          {/if}
          <li><a href="/cart" class="footer-text text-sm transition-colors">Cart</a></li>
          <li><a href="/orders" class="footer-text text-sm transition-colors">My Orders</a></li>
        </ul>
      </div>

      <!-- Support -->
      <div>
        <h4 class="font-semibold text-sm mb-4">Company</h4>
        <ul class="space-y-3">
          <li><a href="/about" class="footer-text text-sm transition-colors">About Us</a></li>
          <li><a href="/help" class="footer-text text-sm transition-colors">Help Center</a></li>
          <li><a href="/terms" class="footer-text text-sm transition-colors">Terms of Service</a></li>
          <li><a href="/privacy" class="footer-text text-sm transition-colors">Privacy Policy</a></li>
        </ul>
      </div>
    </div>

    <div class="border-t border-[var(--color-dark-elevated)] mt-10 pt-8 flex flex-col md:flex-row items-center justify-between gap-4">
      <p class="text-xs text-[var(--color-text-muted)]">&copy; {currentYear} Waroenk. All rights reserved.</p>
      <div class="flex items-center gap-4">
        <a href="/terms" class="footer-text text-xs transition-colors">Legal</a>
        <a href="/privacy" class="footer-text text-xs transition-colors">Privacy</a>
        <a href="/cookies" class="footer-text text-xs transition-colors">Cookies</a>
      </div>
    </div>
  </div>
</footer>

<style>
  /* Waroenk Logo SVG for Footer */
  .waroenk-logo-footer {
    width: 48px;
    height: 48px;
    cursor: pointer;
    transition: transform 0.4s ease, filter 0.4s ease;
  }
  
  .waroenk-logo-footer:hover {
    transform: scale(1.08);
    filter: drop-shadow(0 0 12px rgba(45, 212, 191, 0.6));
  }

  /* Waroenk Text with Aurora Gradient for Footer */
  .waroenk-text-footer {
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
    animation: textGradientShiftFooter 3s linear infinite;
  }

  @keyframes textGradientShiftFooter {
    0% { background-position: 100% 50%; }
    100% { background-position: -100% 50%; }
  }

  /* Aurora Color Drift for Footer */
  @keyframes auroraShiftAFooter {
    0% { stop-color: #2dd4bf; }
    25% { stop-color: #34d399; }
    50% { stop-color: #a78bfa; }
    75% { stop-color: #34d399; }
    100% { stop-color: #2dd4bf; }
  }
  
  @keyframes auroraShiftBFooter {
    0% { stop-color: #a78bfa; }
    25% { stop-color: #2dd4bf; }
    50% { stop-color: #34d399; }
    75% { stop-color: #a78bfa; }
    100% { stop-color: #34d399; }
  }
  
  .grad-a-footer { animation: auroraShiftAFooter 4s ease-in-out infinite; }
  .grad-b-footer { animation: auroraShiftBFooter 4s ease-in-out infinite 0.5s; }

  /* Sparkle animations for Footer */
  @keyframes sparklePulseFooter {
    0%, 100% { opacity: 0.4; transform: scale(1); }
    50% { opacity: 1; transform: scale(1.5); }
  }
  
  @keyframes sparkleFloatFooter {
    0%, 100% { opacity: 0.3; transform: scale(0.9) translateY(0); }
    50% { opacity: 1; transform: scale(1.3) translateY(-2px); }
  }
  
  .sparkle-footer { transform-origin: center; }
  .sparkle-f1 { animation: sparklePulseFooter 2.5s infinite ease-in-out 0.3s; }
  .sparkle-f2 { animation: sparkleFloatFooter 3s infinite ease-in-out 1s; }
  .sparkle-f3 { animation: sparklePulseFooter 2.8s infinite ease-in-out 1.8s; }
</style>
