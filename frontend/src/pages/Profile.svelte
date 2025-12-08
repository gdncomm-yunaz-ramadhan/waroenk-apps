<script>
  import { onMount } from 'svelte';
  import { authApi } from '../lib/api/index.js';
  import { authStore, isAuthenticated, currentUser } from '../lib/stores/auth.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { Loading } from '../lib/components/index.js';
  import { navigate } from '../lib/router/index.js';

  let loading = $state(true);
  let saving = $state(false);
  let editMode = $state(false);

  // User data from store
  let user = $derived($currentUser);

  // Form state for editing
  let form = $state({
    fullName: '',
    email: '',
    phone: '',
    gender: ''
  });

  onMount(async () => {
    if (!$isAuthenticated) {
      navigate('/login?redirect=/profile');
      return;
    }
    await loadProfile();
  });

  async function loadProfile() {
    loading = true;
    try {
      const userData = await authApi.getProfile();
      authStore.updateUser(userData);
      resetForm(userData);
    } catch (e) {
      console.error('Failed to load profile:', e);
      toastStore.error('Failed to load profile');
    } finally {
      loading = false;
    }
  }

  function resetForm(userData = user) {
    form = {
      fullName: userData?.full_name || userData?.fullName || '',
      email: userData?.email || '',
      phone: userData?.phone || '',
      gender: userData?.gender || ''
    };
  }

  function startEdit() {
    resetForm();
    editMode = true;
  }

  function cancelEdit() {
    resetForm();
    editMode = false;
  }

  async function saveProfile(e) {
    e.preventDefault();
    if (saving) return;
    saving = true;

    try {
      const data = {
        full_name: form.fullName,
        email: form.email,
        phone: form.phone,
        gender: form.gender || undefined
      };

      const updated = await authApi.updateProfile(data);
      authStore.updateUser(updated);
      toastStore.success('Profile updated successfully');
      editMode = false;
    } catch (e) {
      console.error('Failed to update profile:', e);
      toastStore.error(e.message || 'Failed to update profile');
    } finally {
      saving = false;
    }
  }

  function logout() {
    authStore.logout();
    navigate('/');
    toastStore.success('Logged out successfully');
  }

  // Get initials for avatar
  function getInitials(name) {
    if (!name) return 'U';
    return name.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
  }

  // Format date
  function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return dateStr;
    }
  }
</script>

<svelte:head>
  <title>My Profile - Waroenk</title>
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
        <p class="text-[var(--color-text-muted)] mb-6">Please login to view your profile.</p>
        <a href="/login?redirect=/profile" class="btn btn-primary">Login</a>
      </div>
    </div>
  {:else if loading}
    <div class="container py-12">
      <Loading text="Loading profile..." />
    </div>
  {:else}
    <!-- Header with Avatar -->
    <div class="profile-header text-white">
      <div class="container py-8">
        <div class="flex flex-col sm:flex-row items-center gap-6">
          <!-- Avatar -->
          <div class="w-24 h-24 rounded-full bg-white/20 backdrop-blur flex items-center justify-center text-3xl font-bold shadow-lg">
            {getInitials(user?.full_name || user?.fullName)}
          </div>
          
          <!-- Info -->
          <div class="text-center sm:text-left flex-1">
            <h1 class="text-2xl font-bold mb-1">
              {user?.full_name || user?.fullName || 'User'}
            </h1>
            <p class="text-white/80 text-sm">
              {user?.email}
            </p>
            {#if user?.created_at || user?.createdAt}
              <p class="text-white/60 text-xs mt-1">
                Member since {formatDate(user?.created_at || user?.createdAt)}
              </p>
            {/if}
          </div>

          <!-- Quick Actions -->
          <div class="flex gap-2">
            <a href="/addresses" class="btn btn-sm profile-action-btn">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              </svg>
              Addresses
            </a>
          </div>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="container py-8">
      <div class="max-w-2xl mx-auto">
        <!-- Profile Card -->
        <div class="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden">
          <div class="px-6 py-4 border-b border-[var(--color-border-light)] flex items-center justify-between">
            <h2 class="font-semibold text-[var(--color-text)]">Profile Information</h2>
            {#if !editMode}
              <button onclick={startEdit} class="text-sm text-[var(--color-primary)] hover:underline font-medium">
                Edit
              </button>
            {/if}
          </div>

          {#if editMode}
            <!-- Edit Form -->
            <form onsubmit={saveProfile} class="p-6 space-y-5">
              <!-- Full Name -->
              <div>
                <label for="fullName" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
                  Full Name <span class="text-[var(--color-error)]">*</span>
                </label>
                <input
                  type="text"
                  id="fullName"
                  bind:value={form.fullName}
                  placeholder="Your full name"
                  required
                  class="input"
                />
              </div>

              <!-- Email -->
              <div>
                <label for="email" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
                  Email Address <span class="text-[var(--color-error)]">*</span>
                </label>
                <input
                  type="email"
                  id="email"
                  bind:value={form.email}
                  placeholder="your@email.com"
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

              <!-- Gender -->
              <div>
                <label for="gender" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
                  Gender <span class="text-[var(--color-text-muted)]">(optional)</span>
                </label>
                <select id="gender" bind:value={form.gender} class="input">
                  <option value="">Select gender</option>
                  <option value="male">Male</option>
                  <option value="female">Female</option>
                  <option value="other">Other</option>
                </select>
              </div>

              <!-- Actions -->
              <div class="flex gap-3 pt-2">
                <button type="button" onclick={cancelEdit} class="btn btn-outline flex-1">
                  Cancel
                </button>
                <button type="submit" disabled={saving} class="btn btn-primary flex-1">
                  {#if saving}
                    <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Saving...
                  {:else}
                    Save Changes
                  {/if}
                </button>
              </div>
            </form>
          {:else}
            <!-- View Mode -->
            <div class="p-6 space-y-5">
              <!-- Full Name -->
              <div>
                <span class="block text-xs font-medium text-[var(--color-text-muted)] mb-1">Full Name</span>
                <p class="text-[var(--color-text)] font-medium">{user?.full_name || user?.fullName || '-'}</p>
              </div>

              <!-- Email -->
              <div>
                <span class="block text-xs font-medium text-[var(--color-text-muted)] mb-1">Email Address</span>
                <p class="text-[var(--color-text)]">{user?.email || '-'}</p>
              </div>

              <!-- Phone -->
              <div>
                <span class="block text-xs font-medium text-[var(--color-text-muted)] mb-1">Phone Number</span>
                <p class="text-[var(--color-text)]">{user?.phone || '-'}</p>
              </div>

              <!-- Gender -->
              <div>
                <span class="block text-xs font-medium text-[var(--color-text-muted)] mb-1">Gender</span>
                <p class="text-[var(--color-text)] capitalize">{user?.gender || '-'}</p>
              </div>

              <!-- User ID -->
              <div>
                <span class="block text-xs font-medium text-[var(--color-text-muted)] mb-1">User ID</span>
                <p class="text-[var(--color-text-muted)] text-sm font-mono">{user?.id || user?.userId || '-'}</p>
              </div>
            </div>
          {/if}
        </div>

        <!-- Quick Links -->
        <div class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
          <!-- Addresses -->
          <a href="/addresses" class="bg-white rounded-xl border border-[var(--color-border)] p-5 flex items-center gap-4 hover:shadow-md transition-shadow group">
            <div class="w-12 h-12 rounded-xl bg-[var(--color-primary)]/10 text-[var(--color-primary)] flex items-center justify-center group-hover:bg-[var(--color-primary)]/15 transition-colors">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-[var(--color-text)]">My Addresses</h3>
              <p class="text-xs text-[var(--color-text-muted)]">Manage delivery addresses</p>
            </div>
            <svg class="w-5 h-5 text-[var(--color-text-muted)] ml-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>

          <!-- Orders -->
          <a href="/orders" class="bg-white rounded-xl border border-[var(--color-border)] p-5 flex items-center gap-4 hover:shadow-md transition-shadow group">
            <div class="w-12 h-12 rounded-xl bg-[var(--color-secondary)]/10 text-[var(--color-secondary)] flex items-center justify-center group-hover:bg-[var(--color-secondary)]/15 transition-colors">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-[var(--color-text)]">My Orders</h3>
              <p class="text-xs text-[var(--color-text-muted)]">View order history</p>
            </div>
            <svg class="w-5 h-5 text-[var(--color-text-muted)] ml-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </a>
        </div>

        <!-- Danger Zone -->
        <div class="mt-8 p-5 border border-red-200 rounded-xl bg-red-50">
          <h3 class="font-semibold text-[var(--color-error)] mb-2">Sign Out</h3>
          <p class="text-sm text-[var(--color-text-muted)] mb-4">
            Sign out of your account on this device.
          </p>
          <button onclick={logout} class="btn btn-sm" style="background: var(--color-error); color: white;">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Sign Out
          </button>
        </div>
      </div>
    </div>
  {/if}
</div>

