<script>
</script>

<svelte:head>
  <title>404 - Page Not Found</title>
</svelte:head>

<div class="min-h-screen flex items-center justify-center py-12 px-4 bg-[var(--color-bg)]">
  <div class="text-center">
    <div class="w-20 h-20 mx-auto mb-4 rounded-xl bg-white border border-[var(--color-border)] flex items-center justify-center">
      <span class="text-3xl font-bold text-[var(--color-text-muted)]">404</span>
    </div>
    <h1 class="text-lg font-semibold text-[var(--color-text)] mb-2">
      Page Not Found
    </h1>
    <p class="text-sm text-[var(--color-text-muted)] mb-6 max-w-xs mx-auto">
      The page you're looking for doesn't exist
    </p>
    <div class="flex flex-wrap justify-center gap-3">
      <a href="/" class="btn btn-primary">Go Home</a>
      <a href="/products" class="btn btn-outline">Browse Products</a>
    </div>
  </div>
</div>
