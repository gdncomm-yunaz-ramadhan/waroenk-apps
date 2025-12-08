<script>
  import { authApi } from '../lib/api/index.js';
  import { isAuthenticated } from '../lib/stores/index.js';
  import { toastStore } from '../lib/stores/toast.js';
  import { navigate } from '../lib/router/index.js';

  let fullName = $state('');
  let email = $state('');
  let phone = $state('');
  let password = $state('');
  let confirmPassword = $state('');
  let loading = $state(false);
  let error = $state(null);
  let showPassword = $state(false);

  // Redirect to homepage if already logged in
  $effect(() => {
    if ($isAuthenticated) {
      navigate('/', true);
    }
  });

  async function handleSubmit(e) {
    e.preventDefault();
    
    if (password !== confirmPassword) {
      error = 'Passwords do not match';
      return;
    }
    
    if (password.length < 6) {
      error = 'Password must be at least 6 characters';
      return;
    }

    loading = true;
    error = null;

    try {
      await authApi.register({
        full_name: fullName,
        email: email,
        phone: phone,
        password: password
      });

      toastStore.success('Account created! Please login.');
      navigate('/login');
    } catch (e) {
      error = e.message || 'Registration failed';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Register - Waroenk</title>
</svelte:head>

<div class="auth-page min-h-screen flex items-center justify-center py-12 px-4">
  <!-- Aurora Background (washed out & blurred) -->
  <div class="aurora-bg">
    <div class="aurora-layer aurora-1"></div>
    <div class="aurora-layer aurora-2"></div>
    <div class="aurora-layer aurora-3"></div>
    <div class="aurora-ambient"></div>
  </div>

  <div class="w-full max-w-sm relative z-10">
    <!-- Animated Aurora Shapes -->
    <div class="aurora-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <!-- Form Card -->
    <div class="auth-card">
      <h1 class="text-lg font-semibold text-center text-[var(--color-text)] mb-1">
        Create Account
      </h1>
      <p class="text-xs text-[var(--color-text-muted)] text-center mb-6">
        Join Waroenk and start shopping
      </p>

      {#if error}
        <div class="bg-red-50 text-[var(--color-error)] px-3 py-2 rounded-lg mb-4 text-xs">
          {error}
        </div>
      {/if}

      <form onsubmit={handleSubmit} class="space-y-3">
        <div>
          <label for="fullName" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Full Name
          </label>
          <input
            type="text"
            id="fullName"
            bind:value={fullName}
            placeholder="Enter your full name"
            required
            class="input"
          />
        </div>

        <div>
          <label for="email" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Email
          </label>
          <input
            type="email"
            id="email"
            bind:value={email}
            placeholder="Enter your email"
            required
            class="input"
          />
        </div>

        <div>
          <label for="phone" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Phone
          </label>
          <input
            type="tel"
            id="phone"
            bind:value={phone}
            placeholder="Enter your phone"
            required
            class="input"
          />
        </div>

        <div>
          <label for="password" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Password
          </label>
          <div class="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              id="password"
              bind:value={password}
              placeholder="Create a password"
              required
              minlength="6"
              class="input pr-10"
            />
            <button 
              type="button"
              onclick={() => showPassword = !showPassword}
              class="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
              aria-label="Toggle password visibility"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {#if showPassword}
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                {:else}
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                {/if}
              </svg>
            </button>
          </div>
        </div>

        <div>
          <label for="confirmPassword" class="block text-xs font-medium text-[var(--color-text)] mb-1.5">
            Confirm Password
          </label>
          <input
            type="password"
            id="confirmPassword"
            bind:value={confirmPassword}
            placeholder="Confirm your password"
            required
            class="input"
          />
        </div>

        <button 
          type="submit"
          disabled={loading}
          class="auth-btn w-full py-2.5 mt-2"
        >
          {#if loading}
            <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Creating Account...
          {:else}
            Create Account
          {/if}
        </button>
      </form>

      <p class="text-center text-xs text-[var(--color-text-muted)] mt-4">
        Already have an account?
        <a href="/login" class="text-[var(--color-primary)] font-medium hover:underline">
          Sign in
        </a>
      </p>
    </div>
  </div>
</div>

<style>
  /* Auth page - washed out aurora background */
  .auth-page {
    position: relative;
    background: #f0f4f3;
    overflow: hidden;
    isolation: isolate;
  }

  /* Aurora background container */
  .aurora-bg {
    position: absolute;
    inset: 0;
    overflow: hidden;
    z-index: 0;
    filter: blur(80px) saturate(0.6);
    opacity: 0.5;
  }

  /* Aurora gradient layers */
  .aurora-layer {
    position: absolute;
    inset: -50%;
    will-change: transform, opacity;
  }

  /* Teal/Cyan aurora */
  .aurora-1 {
    background: radial-gradient(
      ellipse 70% 45% at 30% 40%,
      rgba(45, 212, 191, 0.6) 0%,
      rgba(20, 184, 166, 0.4) 25%,
      rgba(6, 182, 212, 0.2) 45%,
      transparent 65%
    );
    animation: auroraWave1 12s ease-in-out infinite;
  }

  /* Emerald/Green aurora */
  .aurora-2 {
    background: radial-gradient(
      ellipse 55% 60% at 70% 60%,
      rgba(52, 211, 153, 0.55) 0%,
      rgba(16, 185, 129, 0.35) 30%,
      rgba(110, 231, 183, 0.15) 50%,
      transparent 65%
    );
    animation: auroraWave2 16s ease-in-out infinite;
  }

  /* Violet/Pink aurora */
  .aurora-3 {
    background: radial-gradient(
      ellipse 60% 50% at 50% 30%,
      rgba(167, 139, 250, 0.45) 0%,
      rgba(139, 92, 246, 0.25) 30%,
      rgba(192, 132, 252, 0.15) 50%,
      transparent 60%
    );
    animation: auroraWave3 18s ease-in-out infinite;
  }

  /* Ambient glow */
  .aurora-ambient {
    position: absolute;
    inset: 0;
    background: 
      radial-gradient(ellipse at 10% 90%, rgba(45, 212, 191, 0.08) 0%, transparent 45%),
      radial-gradient(ellipse at 90% 10%, rgba(52, 211, 153, 0.06) 0%, transparent 45%),
      radial-gradient(ellipse at 50% 50%, rgba(139, 92, 246, 0.05) 0%, transparent 55%);
    animation: ambientPulse 8s ease-in-out infinite;
  }

  @keyframes auroraWave1 {
    0%, 100% { 
      opacity: 0.7;
      transform: scale(1) rotate(0deg) translateY(0);
    }
    33% {
      opacity: 0.85;
      transform: scale(1.06) rotate(1deg) translateY(-2%);
    }
    66% { 
      opacity: 0.75;
      transform: scale(1.02) rotate(-0.5deg) translateY(1%);
    }
  }

  @keyframes auroraWave2 {
    0%, 100% { 
      opacity: 0.65;
      transform: scale(1) rotate(0deg);
    }
    25% { 
      opacity: 0.8;
      transform: scale(1.08) rotate(-1.5deg);
    }
    50% {
      opacity: 0.7;
      transform: scale(0.98) rotate(0.5deg);
    }
    75% {
      opacity: 0.75;
      transform: scale(1.04) rotate(-0.5deg);
    }
  }

  @keyframes auroraWave3 {
    0%, 100% { 
      opacity: 0.6;
      transform: scale(1) rotate(0deg) translateX(0);
    }
    50% { 
      opacity: 0.75;
      transform: scale(1.05) rotate(2deg) translateX(2%);
    }
  }

  @keyframes ambientPulse {
    0%, 100% { opacity: 0.8; }
    50% { opacity: 1; }
  }

  /* Animated Aurora Shapes */
  .aurora-shapes {
    position: relative;
    height: 48px;
    margin-bottom: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }

  .shape {
    border-radius: 50%;
    filter: blur(0.5px);
    will-change: transform, opacity;
  }

  .shape-1 {
    width: 12px;
    height: 12px;
    background: linear-gradient(135deg, #2dd4bf 0%, #14b8a6 100%);
    box-shadow: 0 0 20px rgba(45, 212, 191, 0.5);
    animation: shapeFloat1 4s ease-in-out infinite, shapeMorph1 8s ease-in-out infinite;
  }

  .shape-2 {
    width: 20px;
    height: 20px;
    background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
    box-shadow: 0 0 24px rgba(52, 211, 153, 0.5);
    animation: shapeFloat2 5s ease-in-out infinite, shapeMorph2 10s ease-in-out infinite;
  }

  .shape-3 {
    width: 10px;
    height: 10px;
    background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
    box-shadow: 0 0 18px rgba(167, 139, 250, 0.5);
    animation: shapeFloat3 4.5s ease-in-out infinite, shapeMorph3 9s ease-in-out infinite;
  }

  @keyframes shapeFloat1 {
    0%, 100% { transform: translateY(0) translateX(0); }
    25% { transform: translateY(-8px) translateX(4px); }
    50% { transform: translateY(-4px) translateX(-2px); }
    75% { transform: translateY(-10px) translateX(2px); }
  }

  @keyframes shapeFloat2 {
    0%, 100% { transform: translateY(0) scale(1); }
    33% { transform: translateY(-12px) scale(1.1); }
    66% { transform: translateY(-6px) scale(0.95); }
  }

  @keyframes shapeFloat3 {
    0%, 100% { transform: translateY(0) translateX(0); }
    30% { transform: translateY(-6px) translateX(-4px); }
    60% { transform: translateY(-10px) translateX(3px); }
  }

  @keyframes shapeMorph1 {
    0%, 100% { border-radius: 50%; }
    25% { border-radius: 40% 60% 60% 40%; }
    50% { border-radius: 50% 50% 40% 60%; }
    75% { border-radius: 60% 40% 50% 50%; }
  }

  @keyframes shapeMorph2 {
    0%, 100% { border-radius: 50%; }
    20% { border-radius: 45% 55% 50% 50%; }
    40% { border-radius: 50% 50% 55% 45%; }
    60% { border-radius: 55% 45% 45% 55%; }
    80% { border-radius: 45% 55% 55% 45%; }
  }

  @keyframes shapeMorph3 {
    0%, 100% { border-radius: 50%; }
    33% { border-radius: 60% 40% 40% 60%; }
    66% { border-radius: 40% 60% 60% 40%; }
  }

  /* Auth card with glass effect */
  .auth-card {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.8);
    padding: 2rem;
    box-shadow: 
      0 4px 24px rgba(0, 0, 0, 0.06),
      0 1px 2px rgba(0, 0, 0, 0.04);
  }

  /* Auth button with light/glow effect */
  .auth-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 0.75rem 1.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    letter-spacing: -0.01em;
    border-radius: 500px;
    cursor: pointer;
    border: none;
    font-family: var(--font-sans);
    white-space: nowrap;
    background: linear-gradient(135deg, #14b8a6 0%, #10b981 100%);
    color: white;
    position: relative;
    overflow: hidden;
    box-shadow: 
      0 4px 15px rgba(20, 184, 166, 0.4),
      0 0 30px rgba(20, 184, 166, 0.2),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  /* Light shimmer effect */
  .auth-btn::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(255, 255, 255, 0.3) 50%,
      transparent 100%
    );
    animation: shimmerLight 3s ease-in-out infinite;
  }

  /* Top highlight for 3D light effect */
  .auth-btn::after {
    content: '';
    position: absolute;
    top: 0;
    left: 10%;
    right: 10%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  }

  @keyframes shimmerLight {
    0% {
      left: -100%;
    }
    50%, 100% {
      left: 100%;
    }
  }

  .auth-btn:hover {
    background: linear-gradient(135deg, #2dd4bf 0%, #34d399 100%);
    box-shadow: 
      0 6px 25px rgba(45, 212, 191, 0.5),
      0 0 50px rgba(45, 212, 191, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.25);
    transform: translateY(-1px) scale(1.01);
  }

  .auth-btn:active {
    transform: translateY(0) scale(0.99);
    box-shadow: 
      0 2px 10px rgba(20, 184, 166, 0.4),
      0 0 20px rgba(20, 184, 166, 0.2);
  }

  .auth-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
</style>
