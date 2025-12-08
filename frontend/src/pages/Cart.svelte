<script>
  import { onMount } from 'svelte';
  import { cartStore, cartTotal } from '../lib/stores/cart.js';
  import { isAuthenticated } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { EmptyState, Loading } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let cart = $derived($cartStore);
  let loading = $state(true);
  let actionLoading = $state(false);

  onMount(async () => {
    if ($isAuthenticated) {
      try {
        await cartStore.init();
      } catch (e) {
        console.error('Failed to load cart:', e);
        toastStore.error('Failed to load cart');
      }
    }
    loading = false;
  });

  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }

  async function updateQuantity(sku, newQuantity) {
    if (actionLoading) return;
    actionLoading = true;
    
    try {
      if (newQuantity < 1) {
        await removeItem(sku);
      } else {
        await cartStore.updateQuantity(sku, newQuantity);
      }
    } catch (e) {
      toastStore.error('Failed to update quantity');
    } finally {
      actionLoading = false;
    }
  }

  async function removeItem(sku) {
    if (actionLoading) return;
    actionLoading = true;
    
    try {
      await cartStore.removeItem(sku);
      toastStore.success('Item removed');
    } catch (e) {
      toastStore.error('Failed to remove item');
    } finally {
      actionLoading = false;
    }
  }

  async function clearCart() {
    if (actionLoading) return;
    if (!confirm('Clear your cart?')) return;
    
    actionLoading = true;
    try {
      await cartStore.clear();
      toastStore.success('Cart cleared');
    } catch (e) {
      toastStore.error('Failed to clear cart');
    } finally {
      actionLoading = false;
    }
  }

  function proceedToCheckout() {
    if (!$isAuthenticated) {
      navigate('/login?redirect=/checkout');
    } else {
      navigate('/checkout');
    }
  }
</script>

<svelte:head>
  <title>Cart - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <div class="container py-6">
    <h1 class="text-lg font-semibold text-[var(--color-text)] mb-6">
      Shopping Cart
    </h1>

    {#if !$isAuthenticated}
      <!-- Not authenticated -->
      <div class="text-center py-12">
        <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-[var(--color-border-light)] flex items-center justify-center">
          <svg class="w-10 h-10 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-[var(--color-text)] mb-2">Login Required</h2>
        <p class="text-[var(--color-text-muted)] mb-6 max-w-md mx-auto">
          Please login to view your cart and add items to it.
        </p>
        <div class="flex flex-wrap justify-center gap-4">
          <a href="/login?redirect=/cart" class="btn btn-primary">Login</a>
          <a href="/register" class="btn btn-outline">Create Account</a>
        </div>
      </div>
    {:else if loading}
      <Loading text="Loading your cart..." />
    {:else if cart.length === 0}
      <EmptyState 
        title="Your cart is empty" 
        message="Start shopping to add items"
        icon="cart"
      />
      <div class="text-center mt-4">
        <a href="/products" class="btn btn-primary">Browse Products</a>
      </div>
    {:else}
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Cart Items -->
        <div class="lg:col-span-2 space-y-3">
          {#each cart as item (item.sku)}
            <div class="bg-white rounded-xl border border-[var(--color-border)] p-4 flex gap-4 animate-fade-in {actionLoading ? 'opacity-60 pointer-events-none' : ''}">
              <!-- Image - Clickable -->
              <a 
                href="/product/{item.subSku || item.sku}"
                class="w-20 h-20 bg-[var(--color-bg)] rounded-lg overflow-hidden flex-shrink-0 hover:opacity-80 transition-opacity"
              >
                {#if item.image}
                  <img src={item.image} alt={item.title} class="w-full h-full object-cover" />
                {:else}
                  <div class="w-full h-full flex items-center justify-center text-[var(--color-text-muted)]">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                  </div>
                {/if}
              </a>

              <!-- Details -->
              <div class="flex-1 min-w-0">
                <a 
                  href="/product/{item.subSku || item.sku}"
                  class="text-sm font-medium text-[var(--color-text)] truncate block hover:text-[var(--color-primary)] transition-colors"
                >
                  {item.title}
                </a>
                <p class="text-xs text-[var(--color-text-muted)] mb-2">SKU: {item.sku}</p>
                <p class="text-sm font-semibold text-[var(--color-text)]">
                  {formatPrice(item.price)}
                </p>

                <!-- Quantity Controls -->
                <div class="flex items-center gap-3 mt-2">
                  <div class="flex items-center border border-[var(--color-border)] rounded-lg">
                    <button 
                      onclick={() => updateQuantity(item.sku, item.quantity - 1)}
                      disabled={actionLoading}
                      class="w-7 h-7 flex items-center justify-center hover:bg-[var(--color-bg)] transition-colors disabled:opacity-50"
                      aria-label="Decrease quantity"
                    >
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
                      </svg>
                    </button>
                    <span class="w-8 text-center text-xs font-medium">{item.quantity}</span>
                    <button 
                      onclick={() => updateQuantity(item.sku, item.quantity + 1)}
                      disabled={actionLoading}
                      class="w-7 h-7 flex items-center justify-center hover:bg-[var(--color-bg)] transition-colors disabled:opacity-50"
                      aria-label="Increase quantity"
                    >
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                    </button>
                  </div>

                  <button 
                    onclick={() => removeItem(item.sku)}
                    disabled={actionLoading}
                    class="text-xs text-[var(--color-error)] hover:underline disabled:opacity-50"
                  >
                    Remove
                  </button>
                </div>
              </div>

              <!-- Subtotal -->
              <div class="text-right hidden sm:block">
                <p class="text-xs text-[var(--color-text-muted)]">Subtotal</p>
                <p class="text-sm font-medium text-[var(--color-text)]">
                  {formatPrice(item.price * item.quantity)}
                </p>
              </div>
            </div>
          {/each}

          <!-- Clear Cart -->
          <div class="text-right">
            <button 
              onclick={clearCart}
              disabled={actionLoading}
              class="text-xs text-[var(--color-error)] hover:underline disabled:opacity-50"
            >
              Clear Cart
            </button>
          </div>
        </div>

        <!-- Order Summary -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-xl border border-[var(--color-border)] p-5 sticky top-20">
            <h2 class="font-semibold text-sm text-[var(--color-text)] mb-4">Order Summary</h2>
            
            <div class="space-y-2 mb-4 text-sm">
              <div class="flex justify-between">
                <span class="text-[var(--color-text-muted)]">Subtotal ({cart.length} items)</span>
                <span>{formatPrice($cartTotal)}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-[var(--color-text-muted)]">Shipping</span>
                <span class="text-[var(--color-success)] text-xs">Calculated at checkout</span>
              </div>
            </div>

            <div class="border-t border-[var(--color-border)] pt-3 mb-4">
              <div class="flex justify-between font-semibold">
                <span>Total</span>
                <span>{formatPrice($cartTotal)}</span>
              </div>
            </div>

            <button 
              onclick={proceedToCheckout}
              disabled={actionLoading}
              class="btn btn-primary w-full py-2.5 disabled:opacity-50"
            >
              Checkout
            </button>

            <a href="/products" class="block text-center text-xs text-[var(--color-primary)] hover:underline mt-3">
              Continue Shopping
            </a>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div>
