<script>
  import { onMount } from 'svelte';
  import { checkoutApi } from '../lib/api/index.js';
  import { isAuthenticated } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { Loading, EmptyState } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let orders = $state([]);
  let loading = $state(true);
  let error = $state(null);
  let activeTab = $state('all');
  let nextToken = $state(null);
  let loadingMore = $state(false);

  const tabs = [
    { id: 'all', label: 'All Orders' },
    { id: 'WAITING', label: 'Waiting Payment' },
    { id: 'PAID', label: 'Paid' },
    { id: 'EXPIRED', label: 'Expired' },
    { id: 'CANCELLED', label: 'Cancelled' }
  ];

  onMount(async () => {
    if (!$isAuthenticated) {
      navigate('/login?redirect=/orders');
      return;
    }
    await loadOrders();
  });

  async function loadOrders(cursor = null) {
    if (cursor) {
      loadingMore = true;
    } else {
      loading = true;
    }
    error = null;

    try {
      const params = { size: 10 };
      if (activeTab !== 'all') {
        params.status = activeTab;
      }
      if (cursor) {
        params.cursor = cursor;
      }

      const res = await checkoutApi.filter(params);
      
      if (cursor) {
        orders = [...orders, ...(res.data || [])];
      } else {
        orders = res.data || [];
      }
      nextToken = res.next_token || null;
    } catch (e) {
      console.error('Failed to load orders:', e);
      error = e.message || 'Failed to load orders';
    } finally {
      loading = false;
      loadingMore = false;
    }
  }

  function handleTabChange(tabId) {
    if (activeTab === tabId) return;
    activeTab = tabId;
    loadOrders();
  }

  async function handleCancel(order) {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    
    try {
      const orderId = typeof order === 'string' ? order : getOrderId(order);
      await checkoutApi.cancel(orderId);
      toastStore.success('Order cancelled successfully');
      loadOrders();
    } catch (e) {
      toastStore.error(e.message || 'Failed to cancel order');
    }
  }

  async function handlePay(order) {
    const orderId = getOrderId(order);
    if (!orderId) {
      toastStore.error('Invalid order');
      return;
    }

    try {
      const res = await checkoutApi.pay(orderId);
      if (res && res.success) {
        toastStore.success('Payment successful!');
        loadOrders();
      } else {
        toastStore.error(res?.message || 'Payment failed');
      }
    } catch (e) {
      toastStore.error(e.message || 'Payment failed');
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
        month: 'short',
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
      case 'WAITING':
        return 'badge-warning';
      case 'PAID':
        return 'badge-success';
      case 'EXPIRED':
        return 'badge-neutral';
      case 'CANCELLED':
        return 'badge-error';
      default:
        return 'badge-neutral';
    }
  }

  function getStatusLabel(status) {
    switch (status) {
      case 'WAITING':
        return 'Waiting Payment';
      case 'PAID':
        return 'Paid';
      case 'EXPIRED':
        return 'Expired';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  /**
   * Get checkout ID for API calls
   */
  function getOrderId(order) {
    return order.checkout_id || order.id || null;
  }

  /**
   * Get human-readable order ID for display
   * Uses order_id (e.g. ORD-20241206-ABCD) if available, otherwise checkout_id
   */
  function getOrderIdDisplay(order) {
    // Prefer the human-readable order_id
    if (order.order_id) {
      return order.order_id;
    }
    // Fallback to checkout_id or id
    const id = getOrderId(order);
    return id ? `#${id.slice(0, 8)}` : '#N/A';
  }

  /**
   * Calculate total from items if total_amount not available
   */
  function getOrderTotal(order) {
    // Try total_price first (from proto CheckoutData)
    if (order.total_price != null && order.total_price > 0) {
      return order.total_price;
    }
    // Try total_amount
    if (order.total_amount != null && order.total_amount > 0) {
      return order.total_amount;
    }
    // Try total field
    if (order.total != null && order.total > 0) {
      return order.total;
    }
    // Calculate from items
    if (order.items && order.items.length > 0) {
      return order.items.reduce((sum, item) => {
        const price = item.price_snapshot || item.priceSnapshot || item.price || 0;
        const qty = item.quantity || 1;
        return sum + (price * qty);
      }, 0);
    }
    return 0;
  }

  /**
   * Get total items count
   */
  function getOrderItemCount(order) {
    if (order.total_items != null && order.total_items > 0) {
      return order.total_items;
    }
    if (order.items && order.items.length > 0) {
      return order.items.reduce((sum, item) => sum + (item.quantity || 1), 0);
    }
    return 0;
  }
</script>

<svelte:head>
  <title>My Orders - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  <!-- Header -->
  <div class="bg-white border-b border-[var(--color-border)]">
    <div class="container py-6">
      <h1 class="text-2xl font-bold text-[var(--color-text)] mb-6">My Orders</h1>

      <!-- Tabs -->
      <div class="flex gap-2 overflow-x-auto pb-2 -mb-2">
        {#each tabs as tab}
          <button
            onclick={() => handleTabChange(tab.id)}
            class="chip whitespace-nowrap {activeTab === tab.id ? 'chip-active' : ''}"
          >
            {tab.label}
          </button>
        {/each}
      </div>
    </div>
  </div>

  <!-- Orders List -->
  <div class="container py-6">
    {#if loading}
      <Loading text="Loading orders..." />
    {:else if error}
      <EmptyState title="Failed to load orders" message={error} icon="error" />
    {:else if orders.length === 0}
      <EmptyState 
        title="No orders found" 
        message={activeTab === 'all' ? "You haven't placed any orders yet." : `No ${getStatusLabel(activeTab).toLowerCase()} orders.`}
        icon="cart"
      />
    {:else}
      <div class="space-y-4">
        {#each orders as order}
          <div class="bg-white rounded-xl border border-[var(--color-border)] overflow-hidden">
            <!-- Order Header -->
            <div class="p-4 border-b border-[var(--color-border-light)] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <span class="font-mono text-sm text-[var(--color-text-muted)]">
                    {getOrderIdDisplay(order)}
                  </span>
                  <span class="badge {getStatusBadge(order.status)}">
                    {getStatusLabel(order.status)}
                  </span>
                </div>
                <p class="text-xs text-[var(--color-text-muted)]">
                  {formatDate(order.created_at || order.createdAt)}
                </p>
              </div>
              <div class="flex items-center gap-2">
                {#if order.status === 'WAITING'}
                  <button onclick={() => handlePay(order)} class="btn btn-primary btn-sm">
                    Pay Now
                  </button>
                  <button onclick={() => handleCancel(order)} class="btn btn-outline btn-sm text-[var(--color-error)]">
                    Cancel
                  </button>
                {/if}
                <a href="/orders/{getOrderId(order)}" class="btn btn-outline btn-sm">
                  View Details
                </a>
              </div>
            </div>

            <!-- Order Items -->
            <div class="p-4">
              <div class="space-y-3">
                {#each (order.items || []).slice(0, 3) as item}
                  <div class="flex gap-3">
                    <div class="w-16 h-16 rounded-lg overflow-hidden bg-[var(--color-border-light)] flex-shrink-0">
                      {#if item.image_url || item.imageUrl}
                        <img src={item.image_url || item.imageUrl} alt={item.title || item.name} class="w-full h-full object-cover" />
                      {:else}
                        <div class="w-full h-full flex items-center justify-center">
                          <svg class="w-6 h-6 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                          </svg>
                        </div>
                      {/if}
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="font-medium text-[var(--color-text)] truncate">{item.title || item.name}</p>
                      <p class="text-sm text-[var(--color-text-muted)]">
                        Qty: {item.quantity || 1} × {formatPrice(item.price_snapshot || item.priceSnapshot || item.price)}
                      </p>
                    </div>
                  </div>
                {/each}
                {#if (order.items || []).length > 3}
                  <p class="text-sm text-[var(--color-text-muted)]">
                    +{order.items.length - 3} more item(s)
                  </p>
                {/if}
              </div>
            </div>

            <!-- Order Footer -->
            <div class="px-4 py-3 bg-[var(--color-bg)] border-t border-[var(--color-border-light)] flex items-center justify-between">
              <span class="text-sm text-[var(--color-text-muted)]">
                {getOrderItemCount(order)} item(s)
              </span>
              <div class="text-right">
                <span class="text-sm text-[var(--color-text-muted)]">Total:</span>
                <span class="font-bold text-[var(--color-text)] ml-2">
                  {formatPrice(getOrderTotal(order))}
                </span>
              </div>
            </div>
          </div>
        {/each}
      </div>

      <!-- Load More -->
      {#if nextToken}
        <div class="text-center mt-8">
          <button
            onclick={() => loadOrders(nextToken)}
            disabled={loadingMore}
            class="btn btn-outline"
          >
            {#if loadingMore}
              <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Loading...
            {:else}
              Load More
            {/if}
          </button>
        </div>
      {/if}
    {/if}
  </div>
</div>


