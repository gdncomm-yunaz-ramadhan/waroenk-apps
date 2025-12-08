# Lightweight E-Commerce Front-End Plan

## 1. Goal

Build a lightweight modern e-commerce front-end with core functions:

* Product listing, search, and details
* Merchant profiles and merchant-level search
* User authentication (login/register)
* Add to cart and checkout flow

Front-end should be lightweight, fast, maintainable, and mobile-first.

---

## 2. Tech Stack

| Layer             | Technology                                                                    |
| ----------------- | ----------------------------------------------------------------------------- |
| Framework         | Vanilla JS or lightweight framework (Svelte / Preact optional)                |
| Styling           | TailwindCSS or minimal custom CSS                                             |
| State             | LocalStorage + lightweight global store (Svelte store or Zustand alternative) |
| API Communication | REST (Axios or fetch wrapper)                                                 |
| Build Tool        | Vite                                                                          |

> No unnecessary libraries to keep bundle size small (<250kb).

---

## 3. Core Pages & Features

### 3.1 Home Page

* Category shortcuts (optional)
* Search bar (global product search)
* Product listing preview

### 3.2 Product Listing Page

* Search bar
* Pagination or infinite scroll
* Filters (optional/minimal)
* Product cards

### 3.3 Product Detail Page

* Product name, images, price, stock status
* Merchant info link
* Add to cart button

### 3.4 Merchant Detail Page

* Merchant profile information
* Search bar to filter products within this merchant's catalog
* All products under the merchant

### 3.5 Authentication Pages

#### Login

* Email / Username + Password
* Redirect back to previous page or dashboard

#### Register

* Simple form: name, email, password
* Optional: confirmation email (handled by backend)

### 3.6 Cart Page

* Items list with quantity modification
* Price summary
* Proceed to checkout button

### 3.7 Checkout Page

* Address input or selection
* Payment method selection
* Confirm order

---

## 4. UI/UX Principles

* Fast, clean layout
* Minimal use of modal
* Mobile-first design
* Keep interactions predictable (simple navigation)

---

## 5. State Management Strategy

* Use LocalStorage to store temporary cart data
* Sync cart with server on login or checkout
* Keep global user state in store (Svelte store or Zustand-style pattern)

### Example State Segments

| Key           | Type   | Description               |
| ------------- | ------ | ------------------------- |
| `user`        | object | user profile & auth token |
| `cart`        | array  | product sku + quantity    |
| `searchState` | string | search terms              |

---

## 6. API Contracts

| Feature               | Endpoint                                       |
| --------------------- | ---------------------------------------------- |
| Get products          | `GET /products?search=&page=`                  |
| Get product details   | `GET /product/{id}`                            |
| Get merchant products | `GET /merchant/{id}/products?search=&page=`    |
| Register              | `POST /auth/register`                          |
| Login                 | `POST /auth/login`                             |
| Add to cart           | `POST /cart` or client-only then `/cart/merge` |
| Checkout              | `POST /checkout`                               |

---

## 7. Performance Optimizations

* Lazy load images
* Cache common data (products, merchants, auth token)
* Avoid large JS frameworks
* Minify & compress assets

---

## 8. Security Considerations

* Store tokens in HttpOnly cookies or encrypted storage
* Validate all calls
* CSRF protection (if using cookies)

---

## 9. Milestone Breakdown

### Phase 1 — Foundation (1 week)

* Project structure + routing
* Home + Product listing

### Phase 2 — Product and Merchant Interaction (1 week)

* Product detail
* Merchant page
* Search functionality

### Phase 3 — Authentication (3–5 days)

* Login
* Register
* User state persistence

### Phase 4 — Cart and Checkout (1–2 weeks)

* Local cart storage
* Merge cart with backend
* Checkout validation & submission

### Phase 5 — Optimization & Testing (ongoing)

---

## 10. Extra Optional Enhancements

* Wishlist
* Product reviews
* Offline support (PWA)

---

## Final Output

A lightweight, modular, scalable front-end capable of:

* Browsing products & merchants
* Searching and filtering
* Authentication
* Cart management & checkout

Minimal footprint, fast load, designed for scale.
