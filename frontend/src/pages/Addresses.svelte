<script>
  import { onMount } from 'svelte';
  import { addressApi } from '../lib/api/index.js';
  import { isAuthenticated } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { Loading, EmptyState } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let addresses = $state([]);
  let loading = $state(true);
  let searchQuery = $state('');
  let showModal = $state(false);
  let editingAddress = $state(null);
  let actionLoading = $state(false);
  let deleteConfirmId = $state(null);

  // Form state
  let form = $state({
    label: '',
    recipientName: '',
    phone: '',
    street: '',
    city: '',
    province: '',
    postalCode: '',
    country: 'Indonesia',
    notes: '',
    isDefault: false
  });

  onMount(async () => {
    if (!$isAuthenticated) {
      navigate('/login?redirect=/addresses');
      return;
    }
    await loadAddresses();
  });

  async function loadAddresses() {
    loading = true;
    try {
      const response = await addressApi.filter({ size: 50 });
      addresses = response.data || [];
    } catch (e) {
      console.error('Failed to load addresses:', e);
      toastStore.error('Failed to load addresses');
    } finally {
      loading = false;
    }
  }

  function openAddModal() {
    editingAddress = null;
    form = {
      label: '',
      recipientName: '',
      phone: '',
      street: '',
      city: '',
      province: '',
      postalCode: '',
      country: 'Indonesia',
      notes: '',
      isDefault: false
    };
    showModal = true;
  }

  function openEditModal(address) {
    editingAddress = address;
    form = {
      label: address.label || '',
      recipientName: address.recipient_name || address.recipientName || '',
      phone: address.phone || '',
      street: address.street || '',
      city: address.city || '',
      province: address.province || '',
      postalCode: address.postal_code || address.postalCode || '',
      country: address.country || 'Indonesia',
      notes: address.notes || '',
      isDefault: address.is_default || address.isDefault || false
    };
    showModal = true;
  }

  function closeModal() {
    showModal = false;
    editingAddress = null;
  }

  async function saveAddress(e) {
    e.preventDefault();
    if (actionLoading) return;
    actionLoading = true;

    try {
      // Build request matching member API proto contract (snake_case)
      // Note: Address proto uses different fields than checkout AddressSnapshotData
      const data = {
        label: form.label,
        // AddressData in proto doesn't have recipient_name/phone (those are in checkout snapshot)
        street: form.street,
        city: form.city,
        province: form.province,
        postal_code: form.postalCode,
        country: form.country,
        details: form.notes, // proto uses 'details' not 'notes'
        is_default: form.isDefault
      };

      if (editingAddress) {
        data.id = editingAddress.id;
      }

      await addressApi.upsert(data);
      toastStore.success(editingAddress ? 'Address updated' : 'Address added');
      closeModal();
      await loadAddresses();
    } catch (e) {
      console.error('Failed to save address:', e);
      toastStore.error(e.message || 'Failed to save address');
    } finally {
      actionLoading = false;
    }
  }

  async function deleteAddress(id) {
    if (actionLoading) return;
    actionLoading = true;

    try {
      await addressApi.delete(id);
      toastStore.success('Address deleted');
      deleteConfirmId = null;
      await loadAddresses();
    } catch (e) {
      console.error('Failed to delete address:', e);
      toastStore.error(e.message || 'Failed to delete address');
    } finally {
      actionLoading = false;
    }
  }

  async function setDefaultAddress(id) {
    if (actionLoading) return;
    actionLoading = true;

    try {
      await addressApi.setDefault(id);
      toastStore.success('Default address updated');
      await loadAddresses();
    } catch (e) {
      console.error('Failed to set default address:', e);
      toastStore.error(e.message || 'Failed to set default address');
    } finally {
      actionLoading = false;
    }
  }

  // Filter addresses by search query
  let filteredAddresses = $derived(
    addresses.filter(addr => {
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      return (
        (addr.label || '').toLowerCase().includes(q) ||
        (addr.recipient_name || addr.recipientName || '').toLowerCase().includes(q) ||
        (addr.street || '').toLowerCase().includes(q) ||
        (addr.city || '').toLowerCase().includes(q) ||
        (addr.province || '').toLowerCase().includes(q)
      );
    })
  );
</script>

<svelte:head>
  <title>My Addresses - Waroenk</title>
</svelte:head>

<div class="min-h-screen bg-[var(--color-bg)]">
  {#if !$isAuthenticated}
    <div class="container py-12">
      <div class="text-center">
        <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-[var(--color-border-light)] flex items-center justify-center">
          <svg class="w-10 h-10 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-[var(--color-text)] mb-2">Login Required</h2>
        <p class="text-[var(--color-text-muted)] mb-6">Please login to manage your addresses.</p>
        <a href="/login?redirect=/addresses" class="btn btn-primary">Login</a>
      </div>
    </div>
  {:else}
    <!-- Header -->
    <div class="bg-white border-b border-[var(--color-border)]">
      <div class="container py-6">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 class="text-xl font-bold text-[var(--color-text)]">My Addresses</h1>
            <p class="text-sm text-[var(--color-text-muted)] mt-1">Manage your delivery addresses</p>
          </div>
          <button onclick={openAddModal} class="btn btn-primary">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Add Address
          </button>
        </div>
      </div>
    </div>

    <!-- Search Section -->
    {#if addresses.length > 0}
      <div class="container pt-6">
        <div class="relative max-w-md">
          <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            bind:value={searchQuery}
            placeholder="Search by name, label, or location..."
            class="w-full h-12 pl-12 pr-4 rounded-xl border border-[var(--color-border)] bg-white text-[var(--color-text)] placeholder:text-[var(--color-text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent transition-all"
          />
          {#if searchQuery}
            <button 
              onclick={() => searchQuery = ''}
              class="absolute right-4 top-1/2 -translate-y-1/2 p-1 hover:bg-[var(--color-bg)] rounded-full transition-colors"
              aria-label="Clear search"
            >
              <svg class="w-4 h-4 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          {/if}
        </div>
      </div>
    {/if}

    <!-- Content -->
    <div class="container py-6">
      {#if loading}
        <Loading text="Loading addresses..." />
      {:else if addresses.length === 0}
        <EmptyState
          title="No addresses yet"
          message="Add your first address to get started"
          icon="address"
        />
        <div class="text-center mt-4">
          <button onclick={openAddModal} class="btn btn-primary">Add Address</button>
        </div>
      {:else if filteredAddresses.length === 0}
        <EmptyState
          title="No matches found"
          message="Try a different search term"
          icon="search"
        />
      {:else}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {#each filteredAddresses as address (address.id)}
            {@const isDefault = address.is_default || address.isDefault}
            <div class="address-card bg-white rounded-2xl border-2 {isDefault ? 'border-[var(--color-primary)] shadow-lg shadow-[var(--color-primary)]/10' : 'border-[var(--color-border)]'} p-6 relative animate-fade-in hover:shadow-md transition-all duration-200">
              
              <!-- Default Badge -->
              {#if isDefault}
                <div class="absolute -top-3 left-6 flex items-center gap-1.5 bg-[var(--color-primary)] text-white text-xs font-semibold px-3 py-1.5 rounded-full shadow-sm">
                  <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                  Default Address
                </div>
              {/if}

              <!-- Label Tag -->
              <div class="flex items-center gap-2 mb-4 {isDefault ? 'mt-2' : ''}">
                <span class="inline-flex items-center gap-2 px-3 py-1.5 bg-[var(--color-bg)] text-[var(--color-text)] rounded-lg text-sm font-medium border border-[var(--color-border-light)]">
                  <svg class="w-4 h-4 text-[var(--color-primary)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  </svg>
                  {address.label || 'Address'}
                </span>
              </div>

              <!-- Address Content -->
              <div class="space-y-3">
                <!-- Street -->
                <p class="text-[var(--color-text)] font-medium leading-relaxed">{address.street}</p>
                
                <!-- City, Province, Postal -->
                <p class="text-sm text-[var(--color-text-light)]">
                  {address.city}{address.province ? `, ${address.province}` : ''} {address.postal_code}
                </p>
                
                <!-- Country -->
                <p class="text-sm text-[var(--color-text-muted)]">{address.country}</p>

                {#if address.notes}
                  <div class="bg-amber-50 text-amber-700 text-sm px-3 py-2 rounded-lg border border-amber-100">
                    <span class="font-medium">Note:</span> {address.notes}
                  </div>
                {/if}
              </div>

              <!-- Divider -->
              <div class="h-px bg-[var(--color-border-light)] my-5"></div>

              <!-- Actions -->
              <div class="flex items-center gap-2 flex-wrap">
                {#if !isDefault}
                  <button
                    onclick={() => setDefaultAddress(address.id)}
                    disabled={actionLoading}
                    class="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-[var(--color-primary)] bg-[var(--color-primary)]/5 hover:bg-[var(--color-primary)]/10 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    Set Default
                  </button>
                {/if}
                
                <button
                  onclick={() => openEditModal(address)}
                  disabled={actionLoading}
                  class="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-[var(--color-text-light)] bg-[var(--color-bg)] hover:bg-[var(--color-border-light)] rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  Edit
                </button>
                
                {#if deleteConfirmId === address.id}
                  <div class="flex items-center gap-2 ml-auto">
                    <span class="text-sm text-[var(--color-error)]">Delete?</span>
                    <button
                      onclick={() => deleteAddress(address.id)}
                      disabled={actionLoading}
                      class="px-3 py-2 text-sm font-medium text-white bg-[var(--color-error)] hover:bg-red-600 rounded-lg transition-colors disabled:opacity-50"
                    >
                      Yes
                    </button>
                    <button
                      onclick={() => deleteConfirmId = null}
                      class="px-3 py-2 text-sm font-medium text-[var(--color-text-muted)] bg-[var(--color-bg)] hover:bg-[var(--color-border-light)] rounded-lg transition-colors"
                    >
                      No
                    </button>
                  </div>
                {:else}
                  <button
                    onclick={() => deleteConfirmId = address.id}
                    disabled={actionLoading}
                    class="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-[var(--color-error)] bg-red-50 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed ml-auto"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    Delete
                  </button>
                {/if}
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  {/if}
</div>

<!-- Add/Edit Modal -->
{#if showModal}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <!-- Backdrop -->
    <div 
      class="absolute inset-0 bg-black/50" 
      onclick={closeModal}
      onkeydown={(e) => e.key === 'Escape' && closeModal()}
      role="button"
      tabindex="-1"
      aria-label="Close modal"
    ></div>
    
    <!-- Modal -->
    <div class="relative bg-white rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto animate-fade-in-scale">
      <div class="sticky top-0 bg-white border-b border-[var(--color-border-light)] px-6 py-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-[var(--color-text)]">
          {editingAddress ? 'Edit Address' : 'Add New Address'}
        </h2>
        <button onclick={closeModal} class="p-2 hover:bg-[var(--color-bg)] rounded-full transition-colors" aria-label="Close modal">
          <svg class="w-5 h-5 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <form onsubmit={saveAddress} class="p-6 space-y-4">
        <!-- Label -->
        <div>
          <label for="label" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Label <span class="text-[var(--color-text-muted)]">(e.g., Home, Office)</span>
          </label>
          <input
            type="text"
            id="label"
            bind:value={form.label}
            placeholder="Home"
            class="input"
          />
        </div>

        <!-- Recipient Name -->
        <div>
          <label for="recipientName" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Recipient Name <span class="text-[var(--color-error)]">*</span>
          </label>
          <input
            type="text"
            id="recipientName"
            bind:value={form.recipientName}
            placeholder="Full name"
            required
            class="input"
          />
        </div>

        <!-- Phone -->
        <div>
          <label for="phone" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Phone Number <span class="text-[var(--color-error)]">*</span>
          </label>
          <input
            type="tel"
            id="phone"
            bind:value={form.phone}
            placeholder="+62 812 3456 7890"
            required
            class="input"
          />
        </div>

        <!-- Street Address -->
        <div>
          <label for="street" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Street Address <span class="text-[var(--color-error)]">*</span>
          </label>
          <textarea
            id="street"
            bind:value={form.street}
            placeholder="Street name, building, apartment..."
            required
            rows="2"
            class="input"
          ></textarea>
        </div>

        <!-- City & Province -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label for="city" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              City <span class="text-[var(--color-error)]">*</span>
            </label>
            <input
              type="text"
              id="city"
              bind:value={form.city}
              placeholder="City"
              required
              class="input"
            />
          </div>
          <div>
            <label for="province" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              Province
            </label>
            <input
              type="text"
              id="province"
              bind:value={form.province}
              placeholder="Province"
              class="input"
            />
          </div>
        </div>

        <!-- Postal Code & Country -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label for="postalCode" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              Postal Code <span class="text-[var(--color-error)]">*</span>
            </label>
            <input
              type="text"
              id="postalCode"
              bind:value={form.postalCode}
              placeholder="12345"
              required
              class="input"
            />
          </div>
          <div>
            <label for="country" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              Country
            </label>
            <input
              type="text"
              id="country"
              bind:value={form.country}
              placeholder="Indonesia"
              class="input"
            />
          </div>
        </div>

        <!-- Notes -->
        <div>
          <label for="notes" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Delivery Notes <span class="text-[var(--color-text-muted)]">(optional)</span>
          </label>
          <textarea
            id="notes"
            bind:value={form.notes}
            placeholder="Any special instructions..."
            rows="2"
            class="input"
          ></textarea>
        </div>

        <!-- Default Checkbox -->
        <label class="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            bind:checked={form.isDefault}
            class="w-4 h-4 rounded border-[var(--color-border)] text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
          />
          <span class="text-sm text-[var(--color-text)]">Set as default address</span>
        </label>

        <!-- Actions -->
        <div class="flex gap-3 pt-4">
          <button type="button" onclick={closeModal} class="btn btn-outline flex-1">
            Cancel
          </button>
          <button type="submit" disabled={actionLoading} class="btn btn-primary flex-1">
            {#if actionLoading}
              <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Saving...
            {:else}
              {editingAddress ? 'Update Address' : 'Add Address'}
            {/if}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

