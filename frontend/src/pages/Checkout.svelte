<script>
  import { onMount } from 'svelte';
  import { cartStore, cartTotal } from '../lib/stores/cart.js';
  import { isAuthenticated, currentUser } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { addressApi, checkoutApi } from '../lib/api/index.js';
  import { Loading, EmptyState } from '../lib/components/index.js';

  let cart = $derived($cartStore);
  let step = $state(1); // 1: Address, 2: Payment, 3: Review
  let loading = $state(true);
  let submitting = $state(false);
  let addresses = $state([]);
  let selectedAddressId = $state(null);
  let paymentMethod = $state('bank_transfer');

  // New address form - matching member API proto contract (snake_case)
  let showAddressForm = $state(false);
  let newAddress = $state({
    label: '',
    country: 'Indonesia',
    province: '',
    city: '',
    district: '',
    sub_district: '',
    postal_code: '',
    street: '',
    details: ''
  });

  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }

  onMount(async () => {
    // Redirect if not authenticated
    if (!$isAuthenticated) {
      window.location.href = '/login?redirect=/checkout';
      return;
    }

    // Redirect if cart is empty
    if (cart.length === 0) {
      window.location.href = '/cart';
      return;
    }

    // Load addresses - pass user_id from current user (snake_case for proto)
    try {
      const userId = $currentUser?.id;
      if (userId) {
        const res = await addressApi.filter({ user: userId, size: 10 });
        addresses = res.data || [];
        
        // Find default address or use first one
        const defaultAddr = addresses.find(a => a.label === 'Default' || a.label === 'default');
        if (defaultAddr) {
          selectedAddressId = defaultAddr.id;
        } else if (addresses.length > 0) {
          selectedAddressId = addresses[0].id;
        }
      }
    } catch (e) {
      console.error('Failed to load addresses:', e);
    } finally {
      loading = false;
    }
  });

  async function handleAddressSubmit(e) {
    e.preventDefault();
    await saveNewAddress();
  }

  async function saveNewAddress() {
    try {
      // Build request matching member API proto contract (snake_case)
      const userId = $currentUser?.id;
      if (!userId) {
        toastStore.error('User not authenticated');
        return;
      }
      
      const addressData = {
        user_id: userId,
        label: newAddress.label || 'Home',
        country: newAddress.country || 'Indonesia',
        province: newAddress.province,
        city: newAddress.city,
        district: newAddress.district,
        sub_district: newAddress.sub_district,
        postal_code: newAddress.postal_code,
        street: newAddress.street,
        details: newAddress.details || null
      };
      
      const res = await addressApi.upsert(addressData);
      addresses = [...addresses, res];
      selectedAddressId = res.id;
      showAddressForm = false;
      newAddress = {
        label: '',
        country: 'Indonesia',
        province: '',
        city: '',
        district: '',
        sub_district: '',
        postal_code: '',
        street: '',
        details: ''
      };
      toastStore.success('Address saved');
    } catch (e) {
      toastStore.error(e.message || 'Failed to save address');
    }
  }

  function nextStep() {
    if (step === 1 && !selectedAddressId) {
      toastStore.error('Please select a delivery address');
      return;
    }
    if (step < 3) step += 1;
  }

  function prevStep() {
    if (step > 1) step -= 1;
  }

  async function placeOrder() {
    submitting = true;

    try {
      const userId = $currentUser?.id;
      if (!userId) {
        toastStore.error('User not authenticated');
        return;
      }

      // Step 1: Prepare checkout - validates stock, locks inventory, creates checkout session
      // This also removes valid items from cart, keeping invalid items
      const prepareRes = await checkoutApi.prepare(userId);
      
      if (!prepareRes) {
        toastStore.error('Failed to prepare checkout');
        return;
      }

      // Check for partial success (some items couldn't be reserved)
      const checkoutId = prepareRes.checkout_id || prepareRes.checkout?.checkout_id;
      if (!checkoutId) {
        toastStore.error(prepareRes.message || 'No items could be reserved for checkout');
        // Refresh cart to show remaining items
        await cartStore.refresh();
        return;
      }

      // Show warning if partial success
      if (prepareRes.sku_lock_summary) {
        const failedItems = prepareRes.sku_lock_summary.filter(s => !s.locked);
        if (failedItems.length > 0) {
          toastStore.warning(`${failedItems.length} item(s) couldn't be reserved due to insufficient stock`);
        }
      }

      // Step 2: Finalize checkout with selected address - creates WAITING order
      const finalizeRes = await checkoutApi.finalize(checkoutId, selectedAddressId);
      
      if (!finalizeRes || !finalizeRes.success) {
        toastStore.error(finalizeRes?.message || 'Failed to finalize checkout');
        return;
      }

      // Order created successfully with WAITING status
      // User can pay from the orders page
      const orderId = finalizeRes.order_id || finalizeRes.checkout?.order_id;
      toastStore.success(`Order ${orderId} created! Please complete payment.`);
      
      // Refresh cart from server to show any remaining items
      await cartStore.refresh();
      
      // Redirect to orders page
      window.location.href = '/orders';
    } catch (e) {
      toastStore.error(e.message || 'Failed to place order');
      // Refresh cart in case of error
      await cartStore.refresh();
    } finally {
      submitting = false;
    }
  }

  const selectedAddress = $derived(addresses.find(a => a.id === selectedAddressId));
</script>

<svelte:head>
  <title>Checkout - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <div class="container py-8">
    {#if loading}
      <Loading text="Loading checkout..." />
    {:else if cart.length === 0}
      <EmptyState 
        title="Your cart is empty" 
        message="Add items to your cart before checking out."
        icon="cart"
      />
    {:else}
      <h1 class="text-2xl font-bold text-[var(--color-text)] mb-8">
        Checkout
      </h1>

      <!-- Progress Steps -->
      <div class="flex items-center justify-center mb-8">
        {#each [{ num: 1, label: 'Address' }, { num: 2, label: 'Payment' }, { num: 3, label: 'Review' }] as stepItem}
          <div class="flex items-center">
            <div class="flex flex-col items-center">
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-semibold {step >= stepItem.num ? 'bg-[var(--color-primary)] text-white' : 'bg-[var(--color-border-light)] text-[var(--color-text-muted)]'}">
                {#if step > stepItem.num}
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                {:else}
                  {stepItem.num}
                {/if}
              </div>
              <span class="text-xs mt-1 font-medium {step >= stepItem.num ? 'text-[var(--color-primary)]' : 'text-[var(--color-text-muted)]'}">
                {stepItem.label}
              </span>
            </div>
            {#if stepItem.num < 3}
              <div class="w-16 md:w-24 h-1 mx-2 rounded-full {step > stepItem.num ? 'bg-[var(--color-primary)]' : 'bg-[var(--color-border-light)]'}"></div>
            {/if}
          </div>
        {/each}
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Main Content -->
        <div class="lg:col-span-2">
          <!-- Step 1: Address -->
          {#if step === 1}
            <div class="bg-[var(--color-surface)] rounded-xl p-6 border border-[var(--color-border)] animate-fade-in">
              <h2 class="text-lg font-semibold text-[var(--color-text)] mb-6">Delivery Address</h2>

              {#if addresses.length > 0 && !showAddressForm}
                <div class="space-y-3 mb-6">
                  {#each addresses as address}
                    <label class="block p-4 border-2 rounded-xl cursor-pointer transition-colors {selectedAddressId === address.id ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)] hover:border-[var(--color-border-light)]'}">
                      <div class="flex items-start gap-3">
                        <input 
                          type="radio" 
                          name="address" 
                          value={address.id}
                          bind:group={selectedAddressId}
                          class="mt-1 accent-[var(--color-primary)]"
                        />
                        <div class="flex-1">
                          <div class="flex items-center gap-2 mb-1">
                            <span class="font-medium text-[var(--color-text)]">{address.label || 'Address'}</span>
                            {#if address.label === 'Default' || address.label === 'default'}
                              <span class="badge badge-success text-xs">Default</span>
                            {/if}
                          </div>
                          <p class="text-sm text-[var(--color-text-light)]">{address.street}</p>
                          <p class="text-sm text-[var(--color-text-muted)]">
                            {address.sub_district || address.subDistrict}, {address.district}
                          </p>
                          <p class="text-sm text-[var(--color-text-muted)]">
                            {address.city}, {address.province} {address.postal_code || address.postalCode}
                          </p>
                          <p class="text-sm text-[var(--color-text-muted)]">{address.country}</p>
                          {#if address.details}
                            <p class="text-sm text-[var(--color-text-muted)] italic">{address.details}</p>
                          {/if}
                        </div>
                      </div>
                    </label>
                  {/each}
                </div>
                <button 
                  onclick={() => showAddressForm = true}
                  class="text-[var(--color-primary)] font-medium hover:underline"
                >
                  + Add New Address
                </button>
              {:else}
                <!-- Address Form - Matching member API contract -->
                <form onsubmit={handleAddressSubmit} class="space-y-4">
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label for="checkout-label" class="block text-sm font-medium mb-1">Label*</label>
                      <input id="checkout-label" bind:value={newAddress.label} placeholder="Home, Office, etc." required class="input" />
                    </div>
                    <div>
                      <label for="checkout-country" class="block text-sm font-medium mb-1">Country*</label>
                      <input id="checkout-country" bind:value={newAddress.country} required class="input" />
                    </div>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label for="checkout-province" class="block text-sm font-medium mb-1">Province*</label>
                      <input id="checkout-province" bind:value={newAddress.province} required class="input" placeholder="e.g., DKI Jakarta" />
                    </div>
                    <div>
                      <label for="checkout-city" class="block text-sm font-medium mb-1">City*</label>
                      <input id="checkout-city" bind:value={newAddress.city} required class="input" placeholder="e.g., Jakarta Selatan" />
                    </div>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label for="checkout-district" class="block text-sm font-medium mb-1">District (Kecamatan)*</label>
                      <input id="checkout-district" bind:value={newAddress.district} required class="input" placeholder="e.g., Kebayoran Baru" />
                    </div>
                    <div>
                      <label for="checkout-subdistrict" class="block text-sm font-medium mb-1">Subdistrict (Kelurahan)*</label>
                      <input id="checkout-subdistrict" bind:value={newAddress.sub_district} required class="input" placeholder="e.g., Senayan" />
                    </div>
                  </div>
                  <div>
                    <label for="checkout-street" class="block text-sm font-medium mb-1">Street Address*</label>
                    <input id="checkout-street" bind:value={newAddress.street} required class="input" placeholder="e.g., Jl. Sudirman No. 123" />
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <label for="checkout-postal" class="block text-sm font-medium mb-1">Postal Code*</label>
                      <input id="checkout-postal" bind:value={newAddress.postal_code} required class="input" placeholder="e.g., 12190" />
                    </div>
                    <div>
                      <label for="checkout-details" class="block text-sm font-medium mb-1">Details (Optional)</label>
                      <input id="checkout-details" bind:value={newAddress.details} class="input" placeholder="e.g., Apartment 5B, near mall" />
                    </div>
                  </div>
                  <div class="flex gap-3">
                    <button type="submit" class="btn btn-primary">Save Address</button>
                    {#if addresses.length > 0}
                      <button type="button" onclick={() => showAddressForm = false} class="btn btn-outline">Cancel</button>
                    {/if}
                  </div>
                </form>
              {/if}
            </div>
          {/if}

          <!-- Step 2: Payment -->
          {#if step === 2}
            <div class="bg-[var(--color-surface)] rounded-xl p-6 border border-[var(--color-border)] animate-fade-in">
              <h2 class="text-lg font-semibold text-[var(--color-text)] mb-6">Payment Method</h2>

              <div class="space-y-3">
                <label class="block p-4 border-2 rounded-xl cursor-pointer transition-colors {paymentMethod === 'bank_transfer' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}">
                  <div class="flex items-center gap-3">
                    <input type="radio" value="bank_transfer" bind:group={paymentMethod} class="accent-[var(--color-primary)]" />
                    <div>
                      <p class="font-medium text-[var(--color-text)]">Bank Transfer</p>
                      <p class="text-sm text-[var(--color-text-muted)]">Pay via bank transfer</p>
                    </div>
                  </div>
                </label>

                <label class="block p-4 border-2 rounded-xl cursor-pointer transition-colors {paymentMethod === 'credit_card' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}">
                  <div class="flex items-center gap-3">
                    <input type="radio" value="credit_card" bind:group={paymentMethod} class="accent-[var(--color-primary)]" />
                    <div>
                      <p class="font-medium text-[var(--color-text)]">Credit/Debit Card</p>
                      <p class="text-sm text-[var(--color-text-muted)]">Visa, Mastercard, JCB</p>
                    </div>
                  </div>
                </label>

                <label class="block p-4 border-2 rounded-xl cursor-pointer transition-colors {paymentMethod === 'ewallet' ? 'border-[var(--color-primary)] bg-[var(--color-primary)]/5' : 'border-[var(--color-border)]'}">
                  <div class="flex items-center gap-3">
                    <input type="radio" value="ewallet" bind:group={paymentMethod} class="accent-[var(--color-primary)]" />
                    <div>
                      <p class="font-medium text-[var(--color-text)]">E-Wallet</p>
                      <p class="text-sm text-[var(--color-text-muted)]">GoPay, OVO, DANA</p>
                    </div>
                  </div>
                </label>
              </div>
            </div>
          {/if}

          <!-- Step 3: Review -->
          {#if step === 3}
            <div class="bg-[var(--color-surface)] rounded-xl p-6 border border-[var(--color-border)] animate-fade-in">
              <h2 class="text-lg font-semibold text-[var(--color-text)] mb-6">Review Order</h2>

              <!-- Delivery Address -->
              <div class="mb-6">
                <h3 class="font-medium text-[var(--color-text-muted)] text-xs uppercase tracking-wide mb-2">Delivery Address</h3>
                {#if selectedAddress}
                  <p class="font-medium text-[var(--color-text)]">{selectedAddress.label}</p>
                  <p class="text-sm text-[var(--color-text-light)]">{selectedAddress.street}</p>
                  <p class="text-sm text-[var(--color-text-muted)]">{selectedAddress.sub_district || selectedAddress.subDistrict}, {selectedAddress.district}</p>
                  <p class="text-sm text-[var(--color-text-muted)]">{selectedAddress.city}, {selectedAddress.province} {selectedAddress.postal_code || selectedAddress.postalCode}</p>
                  <p class="text-sm text-[var(--color-text-muted)]">{selectedAddress.country}</p>
                  {#if selectedAddress.details}
                    <p class="text-sm text-[var(--color-text-muted)] italic">{selectedAddress.details}</p>
                  {/if}
                {/if}
              </div>

              <!-- Payment Method -->
              <div class="mb-6">
                <h3 class="font-medium text-[var(--color-text-muted)] text-xs uppercase tracking-wide mb-2">Payment Method</h3>
                <p class="font-medium text-[var(--color-text)]">
                  {paymentMethod === 'bank_transfer' ? 'Bank Transfer' : paymentMethod === 'credit_card' ? 'Credit/Debit Card' : 'E-Wallet'}
                </p>
              </div>

              <!-- Items -->
              <div>
                <h3 class="font-medium text-[var(--color-text-muted)] text-xs uppercase tracking-wide mb-2">Items ({cart.length})</h3>
                <div class="space-y-3">
                  {#each cart as item}
                    <div class="flex gap-3 p-3 bg-[var(--color-bg)] rounded-xl">
                      <div class="w-16 h-16 bg-[var(--color-border-light)] rounded-lg overflow-hidden flex-shrink-0">
                        {#if item.image}
                          <img src={item.image} alt={item.title} class="w-full h-full object-cover" />
                        {/if}
                      </div>
                      <div class="flex-1 min-w-0">
                        <p class="font-medium text-[var(--color-text)] truncate">{item.title}</p>
                        <p class="text-sm text-[var(--color-text-muted)]">Qty: {item.quantity}</p>
                      </div>
                      <p class="font-medium text-[var(--color-text)]">{formatPrice(item.price * item.quantity)}</p>
                    </div>
                  {/each}
                </div>
              </div>
            </div>
          {/if}

          <!-- Back Button Only -->
          {#if step > 1}
            <div class="mt-6">
              <button onclick={prevStep} class="btn btn-outline">
                ← Back
              </button>
            </div>
          {/if}
        </div>

        <!-- Order Summary Sidebar -->
        <div class="lg:col-span-1">
          <div class="bg-[var(--color-surface)] rounded-xl p-6 border border-[var(--color-border)] sticky top-24">
            <h2 class="text-lg font-semibold text-[var(--color-text)] mb-6">Order Summary</h2>
            
            <div class="space-y-3 mb-6">
              {#each cart as item}
                <div class="flex justify-between text-sm">
                  <span class="text-[var(--color-text-light)] truncate pr-2">{item.title} x{item.quantity}</span>
                  <span class="text-[var(--color-text)]">{formatPrice(item.price * item.quantity)}</span>
                </div>
              {/each}
            </div>

            <div class="border-t border-[var(--color-border)] pt-4 space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-[var(--color-text-muted)]">Subtotal</span>
                <span class="text-[var(--color-text)]">{formatPrice($cartTotal)}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-[var(--color-text-muted)]">Shipping</span>
                <span class="text-[var(--color-success)]">Free</span>
              </div>
            </div>

            <div class="border-t border-[var(--color-border)] pt-4 mt-4">
              <div class="flex justify-between font-semibold text-lg">
                <span class="text-[var(--color-text)]">Total</span>
                <span class="text-[var(--color-primary)]">{formatPrice($cartTotal)}</span>
              </div>
            </div>

            <!-- Action Button -->
            <div class="mt-6">
              {#if step < 3}
                <button onclick={nextStep} class="btn btn-primary w-full">
                  Continue →
                </button>
              {:else}
                <button 
                  onclick={placeOrder} 
                  disabled={submitting}
                  class="btn btn-primary w-full"
                >
                  {#if submitting}
                    Processing...
                  {:else}
                    Place Order
                  {/if}
                </button>
              {/if}
            </div>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div>


