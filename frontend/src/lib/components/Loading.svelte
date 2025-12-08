<script>
  let { text = 'Loading...', fullScreen = false } = $props();
</script>

{#if fullScreen}
  <div class="fixed inset-0 bg-white/90 backdrop-blur-md z-50 flex items-center justify-center">
    <div class="text-center animate-fade-in">
      <!-- Spotify-style loader -->
      <div class="flex items-center justify-center gap-1.5 mb-4">
        <div class="w-2 h-8 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 0ms;"></div>
        <div class="w-2 h-12 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 150ms;"></div>
        <div class="w-2 h-6 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 300ms;"></div>
        <div class="w-2 h-10 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 450ms;"></div>
        <div class="w-2 h-4 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 600ms;"></div>
      </div>
      <p class="text-sm font-medium text-[var(--color-text-light)]">{text}</p>
    </div>
  </div>
{:else}
  <div class="flex items-center justify-center py-16">
    <div class="text-center animate-fade-in">
      <!-- Spotify-style loader (smaller) -->
      <div class="flex items-center justify-center gap-1 mb-3">
        <div class="w-1.5 h-5 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 0ms;"></div>
        <div class="w-1.5 h-8 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 150ms;"></div>
        <div class="w-1.5 h-4 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 300ms;"></div>
        <div class="w-1.5 h-6 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 450ms;"></div>
        <div class="w-1.5 h-3 bg-[var(--color-primary)] rounded-full animate-pulse" style="animation-delay: 600ms;"></div>
      </div>
      <p class="text-xs font-medium text-[var(--color-text-muted)]">{text}</p>
    </div>
  </div>
{/if}

<style>
  @keyframes bars {
    0%, 100% { transform: scaleY(0.4); }
    50% { transform: scaleY(1); }
  }
</style>
