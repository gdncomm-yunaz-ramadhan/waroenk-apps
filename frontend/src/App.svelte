<script>
  import { Header, Footer, Toast } from './lib/components/index.js';
  import { authStore, cartStore } from './lib/stores/index.js';
  import { router, matchPath } from './lib/router/index.js';
  import { onMount } from 'svelte';

  // Pages
  import Home from './pages/Home.svelte';
  import Products from './pages/Products.svelte';
  import ProductDetail from './pages/ProductDetail.svelte';
  import Merchants from './pages/Merchants.svelte';
  import MerchantDetail from './pages/MerchantDetail.svelte';
  import Categories from './pages/Categories.svelte';
  import Brands from './pages/Brands.svelte';
  import Search from './pages/Search.svelte';
  import Login from './pages/Login.svelte';
  import Register from './pages/Register.svelte';
  import ForgotPassword from './pages/ForgotPassword.svelte';
  import ResetPassword from './pages/ResetPassword.svelte';
  import Cart from './pages/Cart.svelte';
  import Checkout from './pages/Checkout.svelte';
  import Profile from './pages/Profile.svelte';
  import Addresses from './pages/Addresses.svelte';
  import Orders from './pages/Orders.svelte';
  import OrderDetail from './pages/OrderDetail.svelte';
  import NotFound from './pages/NotFound.svelte';

  // Route definitions
  const routes = [
    { path: '/', name: 'home' },
    { path: '/products', name: 'products' },
    { path: '/product/:id', name: 'productDetail' },
    { path: '/merchants', name: 'merchants' },
    { path: '/merchant/:id', name: 'merchantDetail' },
    { path: '/categories', name: 'categories' },
    { path: '/brands', name: 'brands' },
    { path: '/search', name: 'search' },
    { path: '/login', name: 'login' },
    { path: '/register', name: 'register' },
    { path: '/forgot-password', name: 'forgotPassword' },
    { path: '/reset-password', name: 'resetPassword' },
    { path: '/cart', name: 'cart' },
    { path: '/checkout', name: 'checkout' },
    { path: '/profile', name: 'profile' },
    { path: '/addresses', name: 'addresses' },
    { path: '/orders', name: 'orders' },
    { path: '/orders/:id', name: 'orderDetail' },
    { path: '*', name: 'notFound' }
  ];

  let currentRouteName = $state('home');
  let params = $state({});

  // Match route when path changes
  function updateRoute(path) {
    for (const route of routes) {
      const match = matchPath(route.path, path);
      if (match !== null) {
        currentRouteName = route.name;
        params = match;
        return;
      }
    }
    // Fallback to not found
    currentRouteName = 'notFound';
    params = {};
  }

  // Subscribe to router changes
  $effect(() => {
    const unsubscribe = router.subscribe(path => {
      updateRoute(path);
    });
    return unsubscribe;
  });

  onMount(() => {
    // Initialize stores
    authStore.init();
    cartStore.init();
    // Set initial route
    updateRoute(window.location.pathname);
  });
</script>

<div class="flex flex-col min-h-screen">
  <Header />
  
  <main class="flex-1">
    {#if currentRouteName === 'home'}
      <Home />
    {:else if currentRouteName === 'products'}
      <Products />
    {:else if currentRouteName === 'productDetail'}
      <ProductDetail id={params.id} />
    {:else if currentRouteName === 'merchants'}
      <Merchants />
    {:else if currentRouteName === 'merchantDetail'}
      <MerchantDetail id={params.id} />
    {:else if currentRouteName === 'categories'}
      <Categories />
    {:else if currentRouteName === 'brands'}
      <Brands />
    {:else if currentRouteName === 'search'}
      <Search />
    {:else if currentRouteName === 'login'}
      <Login />
    {:else if currentRouteName === 'register'}
      <Register />
    {:else if currentRouteName === 'forgotPassword'}
      <ForgotPassword />
    {:else if currentRouteName === 'resetPassword'}
      <ResetPassword />
    {:else if currentRouteName === 'cart'}
      <Cart />
    {:else if currentRouteName === 'checkout'}
      <Checkout />
    {:else if currentRouteName === 'profile'}
      <Profile />
    {:else if currentRouteName === 'addresses'}
      <Addresses />
    {:else if currentRouteName === 'orders'}
      <Orders />
    {:else if currentRouteName === 'orderDetail'}
      <OrderDetail id={params.id} />
    {:else}
      <NotFound />
    {/if}
  </main>
  
  <Footer />
</div>

<Toast />
