<script>
  import { onMount } from 'svelte';
  import { checkoutApi } from '../lib/api/index.js';
  import { isAuthenticated } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { Loading, EmptyState } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let { id } = $props();
  
  let order = $state(null);
  let loading = $state(true);
  let error = $state(null);

  onMount(async () => {
    if (!$isAuthenticated) {
      navigate(`/login?redirect=/orders/${id}`);
      return;
    }
    await loadOrder();
  });

  async function loadOrder() {
    loading = true;
    error = null;

    try {
      const res = await checkoutApi.getById(id);
      order = res;
    } catch (e) {
      console.error('Failed to load order:', e);
      error = e.message || 'Failed to load order';
    } finally {
      loading = false;
    }
  }

  async function handlePay() {
    try {
      const res = await checkoutApi.pay(id);
      if (res && res.success) {
        toastStore.success('Payment successful!');
        await loadOrder();
      } else {
        toastStore.error(res?.message || 'Payment failed');
      }
    } catch (e) {
      toastStore.error(e.message || 'Payment failed');
    }
  }

  async function handleCancel() {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    
    try {
      await checkoutApi.cancel(id);
      toastStore.success('Order cancelled successfully');
      await loadOrder();
    } catch (e) {
      toastStore.error(e.message || 'Failed to cancel order');
    }
  }

  function formatPrice(price) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      minimumFractionDigits: 0
    }).format(price || 0);
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }

  function getStatusBadge(status) {
    switch (status) {
      case 'WAITING': return 'badge-warning';
      case 'PAID': return 'badge-success';
      case 'EXPIRED': return 'badge-neutral';
      case 'CANCELLED': return 'badge-error';
      default: return 'badge-neutral';
    }
  }

  function getStatusLabel(status) {
    switch (status) {
      case 'WAITING': return 'Waiting Payment';
      case 'PAID': return 'Paid';
      case 'EXPIRED': return 'Expired';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  }

  function getOrderTotal() {
    if (!order) return 0;
    if (order.total_price > 0) return order.total_price;
    if (order.total_amount > 0) return order.total_amount;
    if (order.items && order.items.length > 0) {
      return order.items.reduce((sum, item) => {
        const price = item.price_snapshot || item.priceSnapshot || item.price || 0;
        const qty = item.quantity || 1;
        return sum + (price * qty);
      }, 0);
    }
    return 0;
  }
</script>

<svelte:head>
  <title>Order Details - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <!-- Header -->
  <div class="bg-white border-b border-[var(--color-border)]">
    <div class="container py-6">
      <div class="flex items-center gap-4">
        <button onclick={() => navigate('/orders')} class="p-2 hover:bg-[var(--color-bg)] rounded-full transition-colors">
          <svg class="w-5 h-5 text-[var(--color-text)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 class="text-xl font-bold text-[var(--color-text)]">Order Details</h1>
          {#if order}
            <p class="text-sm text-[var(--color-text-muted)]">{order.order_id || id}</p>
          {/if}
        </div>
      </div>
    </div>
  </div>

  <div class="container py-6">
    {#if loading}
      <Loading text="Loading order..." />
    {:else if error || !order}
      <EmptyState title="Order not found" message={error || "This order doesn't exist"} icon="cart" />
      <div class="text-center mt-4">
        <button onclick={() => navigate('/orders')} class="btn btn-primary">Back to Orders</button>
      </div>
    {:else}
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Order Info -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Status Card -->
          <div class="bg-white rounded-xl border border-[var(--color-border)] p-6">
            <div class="flex items-center justify-between flex-wrap gap-4">
              <div>
                <span class="badge {getStatusBadge(order.status)} text-sm px-4 py-1.5">
                  {getStatusLabel(order.status)}
                </span>
                <p class="text-sm text-[var(--color-text-muted)] mt-2">
                  Order placed on {formatDate(order.created_at || order.createdAt)}
                </p>
              </div>
              {#if order.status === 'WAITING'}
                <div class="flex gap-2">
                  <button onclick={handlePay} class="btn btn-primary">Pay Now</button>
                  <button onclick={handleCancel} class="btn btn-outline text-[var(--color-error)]">Cancel</button>
                </div>
              {/if}
            </div>
          </div>

          <!-- Order Items -->
          <div class="bg-white rounded-xl border border-[var(--color-border)] p-6">
            <h2 class="font-semibold text-[var(--color-text)] mb-4">Order Items</h2>
            <div class="space-y-4">
              {#each (order.items || []) as item}
                <a 
                  href="/product/{item.sub_sku || item.subSku || item.sku}"
                  class="flex gap-4 p-4 bg-[var(--color-bg)] rounded-xl hover:bg-[var(--color-border-light)] transition-colors cursor-pointer"
                >
                  <div class="w-20 h-20 rounded-lg overflow-hidden bg-[var(--color-border-light)] flex-shrink-0">
                    {#if item.image_url || item.imageUrl}
                      <img src={item.image_url || item.imageUrl} alt={item.title || item.name} class="w-full h-full object-cover" />
                    {:else}
                      <div class="w-full h-full flex items-center justify-center">
                        <svg class="w-8 h-8 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                      </div>
                    {/if}
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="font-medium text-[var(--color-text)] truncate">{item.title || item.name || 'Product'}</h3>
                    <p class="text-sm text-[var(--color-text-muted)] mt-1">SKU: {item.sub_sku || item.subSku || item.sku}</p>
                    <div class="flex items-center justify-between mt-2">
                      <p class="text-sm text-[var(--color-text-muted)]">
                        Qty: {item.quantity || 1} × {formatPrice(item.price_snapshot || item.priceSnapshot || item.price)}
                      </p>
                      <p class="font-semibold text-[var(--color-text)]">
                        {formatPrice((item.price_snapshot || item.priceSnapshot || item.price || 0) * (item.quantity || 1))}
                      </p>
                    </div>
                  </div>
                  <svg class="w-5 h-5 text-[var(--color-text-muted)] self-center" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </a>
              {/each}
            </div>
          </div>

          <!-- Shipping Address -->
          {#if order.shipping_address || order.shippingAddress}
            {@const address = order.shipping_address || order.shippingAddress}
            <div class="bg-white rounded-xl border border-[var(--color-border)] p-6">
              <h2 class="font-semibold text-[var(--color-text)] mb-4">Shipping Address</h2>
              <div class="text-sm text-[var(--color-text-light)] space-y-1">
                {#if address.recipient_name || address.recipientName}
                  <p class="font-medium text-[var(--color-text)]">{address.recipient_name || address.recipientName}</p>
                {/if}
                {#if address.phone}
                  <p>{address.phone}</p>
                {/if}
                <p>{address.street}</p>
                <p>{address.sub_district || address.subDistrict}, {address.district}</p>
                <p>{address.city}, {address.province} {address.postal_code || address.postalCode}</p>
                <p>{address.country}</p>
                {#if address.notes}
                  <p class="italic">{address.notes}</p>
                {/if}
              </div>
            </div>
          {/if}
        </div>

        <!-- Order Summary Sidebar -->
        <div class="lg:col-span-1">
          <div class="bg-white rounded-xl border border-[var(--color-border)] p-6 sticky top-24">
            <h2 class="font-semibold text-[var(--color-text)] mb-4">Order Summary</h2>
            
            <div class="space-y-3 text-sm">
              {#if order.order_id}
                <div class="flex justify-between">
                  <span class="text-[var(--color-text-muted)]">Order ID</span>
                  <span class="font-mono text-[var(--color-text)]">{order.order_id}</span>
                </div>
              {/if}
              {#if order.payment_code || order.paymentCode}
                <div class="flex justify-between">
                  <span class="text-[var(--color-text-muted)]">Payment Code</span>
                  <span class="font-mono text-[var(--color-text)]">{order.payment_code || order.paymentCode}</span>
                </div>
              {/if}
              <div class="flex justify-between">
                <span class="text-[var(--color-text-muted)]">Items</span>
                <span class="text-[var(--color-text)]">{order.items?.length || 0}</span>
              </div>
            </div>

            <div class="border-t border-[var(--color-border)] pt-4 mt-4 space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-[var(--color-text-muted)]">Subtotal</span>
                <span class="text-[var(--color-text)]">{formatPrice(getOrderTotal())}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-[var(--color-text-muted)]">Shipping</span>
                <span class="text-[var(--color-success)]">Free</span>
              </div>
            </div>

            <div class="border-t border-[var(--color-border)] pt-4 mt-4">
              <div class="flex justify-between font-semibold text-lg">
                <span class="text-[var(--color-text)]">Total</span>
                <span class="text-[var(--color-primary)]">{formatPrice(getOrderTotal())}</span>
              </div>
            </div>

            {#if order.status === 'PAID' && order.paid_at}
              <div class="mt-4 p-3 bg-[var(--color-success)]/10 rounded-lg">
                <p class="text-sm text-[var(--color-success)] font-medium">
                  Paid on {formatDate(order.paid_at)}
                </p>
              </div>
            {/if}

            {#if order.status === 'CANCELLED' && order.cancelled_at}
              <div class="mt-4 p-3 bg-[var(--color-error)]/10 rounded-lg">
                <p class="text-sm text-[var(--color-error)] font-medium">
                  Cancelled on {formatDate(order.cancelled_at)}
                </p>
              </div>
            {/if}

            {#if order.expires_at && order.status === 'WAITING'}
              <div class="mt-4 p-3 bg-[var(--color-warning)]/10 rounded-lg">
                <p class="text-sm text-[var(--color-warning)] font-medium">
                  Expires on {formatDate(order.expires_at)}
                </p>
              </div>
            {/if}
          </div>
        </div>
      </div>
    {/if}
  </div>
</div>





