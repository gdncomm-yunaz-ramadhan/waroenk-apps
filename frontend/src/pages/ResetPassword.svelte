<script>
  import { authApi } from '../lib/api/index.js';
  import { isAuthenticated } from '../lib/stores/index.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { navigate } from '../lib/router/index.js';

  // Get token from URL query params
  const urlParams = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null;
  const tokenFromUrl = urlParams?.get('token') || '';

  let resetToken = $state(tokenFromUrl);
  let newPassword = $state('');
  let confirmPassword = $state('');
  let loading = $state(false);
  let error = $state(null);
  let success = $state(false);
  let showNewPassword = $state(false);
  let showConfirmPassword = $state(false);

  // Redirect to homepage if already logged in
  $effect(() => {
    if ($isAuthenticated) {
      navigate('/', true);
    }
  });

  // Password requirements
  const requirements = [
    { label: 'At least 8 characters', check: (pwd) => pwd.length >= 8 },
    { label: 'One uppercase letter', check: (pwd) => /[A-Z]/.test(pwd) },
    { label: 'One lowercase letter', check: (pwd) => /[a-z]/.test(pwd) },
    { label: 'One number', check: (pwd) => /\d/.test(pwd) },
    { label: 'One special character', check: (pwd) => /[!@#$%^&*(),.?":{}|<>]/.test(pwd) }
  ];

  let passwordsMatch = $derived(newPassword && confirmPassword && newPassword === confirmPassword);
  let allRequirementsMet = $derived(requirements.every(r => r.check(newPassword)));

  async function handleSubmit(e) {
    e.preventDefault();
    
    if (!passwordsMatch) {
      error = 'Passwords do not match';
      return;
    }

    if (!allRequirementsMet) {
      error = 'Password does not meet requirements';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await authApi.changePassword({
        resetToken,
        newPassword,
        confirmPassword
      });
      
      if (response.success) {
        success = true;
        toastStore.success('Password changed successfully!');
        // Redirect to login after 2 seconds
        setTimeout(() => navigate('/login'), 2000);
      } else {
        error = response.message || 'Failed to change password';
      }
    } catch (e) {
      error = e.message || 'Failed to change password';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Reset Password - Waroenk</title>
</svelte:head>

<div class="min-h-screen flex items-center justify-center py-12 px-4 bg-[var(--color-bg)]">
  <div class="w-full max-w-sm">
    <!-- Logo -->
    <div class="text-center mb-6">
      <a href="/" class="inline-flex items-center gap-2">
        <div class="w-9 h-9 rounded-lg bg-[var(--color-primary)] flex items-center justify-center">
          <span class="text-white font-semibold">W</span>
        </div>
        <span class="text-xl font-semibold text-[var(--color-text)]">
          Waroenk
        </span>
      </a>
    </div>

    <!-- Form Card -->
    <div class="bg-white rounded-xl border border-[var(--color-border)] p-6">
      {#if success}
        <!-- Success State -->
        <div class="text-center">
          <div class="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h1 class="text-lg font-semibold text-[var(--color-text)] mb-2">
            Password Changed
          </h1>
          <p class="text-xs text-[var(--color-text-muted)] mb-4">
            Your password has been successfully changed. Redirecting to login...
          </p>
          <a href="/login" class="text-xs text-[var(--color-primary)] hover:underline">
            Go to Login
          </a>
        </div>
      {:else}
        <!-- Form State -->
        <h1 class="text-lg font-semibold text-center text-[var(--color-text)] mb-1">
          Reset password
        </h1>
        <p class="text-xs text-[var(--color-text-muted)] text-center mb-6">
          Enter your new password below.
        </p>

        {#if error}
          <div class="bg-red-50 text-[var(--color-error)] px-3 py-2 rounded-lg mb-4 text-xs">
            {error}
          </div>
        {/if}

        <form onsubmit={handleSubmit} class="space-y-4">
          <!-- Reset Token -->
          <div>
            <label for="resetToken" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              Reset Token
            </label>
            <input
              type="text"
              id="resetToken"
              bind:value={resetToken}
              placeholder="Enter your reset token"
              required
              class="input"
            />
          </div>

          <!-- New Password -->
          <div>
            <label for="newPassword" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              New Password
            </label>
            <div class="relative">
              <input
                type={showNewPassword ? 'text' : 'password'}
                id="newPassword"
                bind:value={newPassword}
                placeholder="Enter new password"
                required
                class="input pr-10"
              />
              <button 
                type="button"
                onclick={() => showNewPassword = !showNewPassword}
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
                aria-label="Toggle password visibility"
              >
                {#if showNewPassword}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                {:else}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                {/if}
              </button>
            </div>

            <!-- Password Requirements -->
            {#if newPassword}
              <div class="mt-2 space-y-1">
                {#each requirements as req}
                  <div class="flex items-center gap-2 text-xs {req.check(newPassword) ? 'text-green-600' : 'text-[var(--color-text-muted)]'}">
                    {#if req.check(newPassword)}
                      <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    {:else}
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <circle cx="12" cy="12" r="10" stroke-width="2" />
                      </svg>
                    {/if}
                    {req.label}
                  </div>
                {/each}
              </div>
            {/if}
          </div>

          <!-- Confirm Password -->
          <div>
            <label for="confirmPassword" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
              Confirm Password
            </label>
            <div class="relative">
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                id="confirmPassword"
                bind:value={confirmPassword}
                placeholder="Confirm new password"
                required
                class="input pr-10"
              />
              <button 
                type="button"
                onclick={() => showConfirmPassword = !showConfirmPassword}
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
                aria-label="Toggle password visibility"
              >
                {#if showConfirmPassword}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                {:else}
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                {/if}
              </button>
            </div>
            
            {#if confirmPassword && !passwordsMatch}
              <p class="mt-1 text-xs text-[var(--color-error)]">Passwords do not match</p>
            {:else if confirmPassword && passwordsMatch}
              <p class="mt-1 text-xs text-green-600">Passwords match</p>
            {/if}
          </div>

          <!-- Submit -->
          <button 
            type="submit"
            disabled={loading || !passwordsMatch || !allRequirementsMet}
            class="btn btn-primary w-full py-2.5 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {#if loading}
              <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Resetting...
            {:else}
              Reset Password
            {/if}
          </button>
        </form>

        <!-- Back to Login -->
        <p class="text-center text-xs text-[var(--color-text-muted)] mt-4">
          Remember your password?
          <a href="/login" class="text-[var(--color-primary)] font-medium hover:underline">
            Sign in
          </a>
        </p>
      {/if}
    </div>
  </div>
</div>





