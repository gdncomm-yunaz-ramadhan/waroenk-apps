# Requirement :
- Use mongo db 6.0
- Use redis to store some value
- Add required dependency if not exists
- Create db migration script and utilize using mongock
- Create grcp contract that match with the database schema
- Create repository and entity
- Adhere the sctructure like we did on member application
- Controller expected :
  - System parameter controller
  - Product controller
  - Cache controller
  - Version controller

# 🏗️ Catalog Service Database Plan (MongoDB)

This document describes the **final optimized MongoDB model** for an eCommerce catalog service that supports:

* 100K+ products
* dynamic variants & attributes
* full-text and filtered search
* scalable read/write separation

It includes:

1. Core data model
2. Search-optimized model
3. Indexing strategy
4. Operational workflow
5. Setup steps

---

## 📌 1. Collections Overview

| Collection      | Role                               | Notes                                   |
| --------------- | ---------------------------------- | --------------------------------------- |
| `merchants`     | Stores merchant/seller information | Renamed from `stores`                   |
| `categories`    | Product categorization hierarchy   | Optional parent→child tree              |
| `brands`        | Product brands                     | One product → one brand                 |
| `products`      | Base product metadata              | Minimal data, no variants stored inside |
| `variants`      | Variant-level products             | Dynamic attribute storage               |
| `inventories`   | Stock per variant                  | Updated frequently                      |
| `product_index` | **Denormalized search model**      | Used only for search/read operations    |

---

## 📦 2. Source-of-Truth Collections (Normalized)

These collections support stable writes and transactional consistency.

### 🧾 `merchants`

* `_id` (UUID)
* `name`
* `code` (unique)
* `contact { phone, email }`
* `rating`

📌 Indexes: `code (unique)`

---

### 🧾 `categories`

* `_id` (UUID)
* `name`
* `slug` (unique)
* `parentId` (nullable)

📌 Indexes: `slug (unique)` + Full text on name

---

### 🧾 `brands`

* `_id` (UUID)
* `name`
* `slug` (unique)

📌 Indexes: `slug (unique)` + Full text on name

---

### 🧾 `products`

* `_id`
* `title`
* `sku` (unique)
* `merchantCode`
* `categoryId`
* `brandId`
* `summary { shortDescription, tags }` (no images - stored on variants)
* `detailRef` (optional external doc reference)

📌 Indexes: SKU unique, category, brand, full text on title/summary

---

### 🧾 `variants`

* `_id`
* `sku` (parent product SKU)
* `subSku` (unique auto-generated)
* `price`
* `attributes { color, size, material, CPU, RAM, ... }`
* `thumbnail` (default display image URL)
* `media: [ { url, type, sortOrder, altText } ]` (all media for this variant)

📌 Indexes: `subSku (unique)`, `sku`

---

### 🧾 `inventories`

* `_id`
* `subSku`
* `stock`

📌 Indexes: `subSku (unique)`

---

## 🔍 3. Search-Optimized Collection (Denormalized)

### 🧾 `product_index`

Used only for **read/search**. Generated automatically via event sync.

Fields:

* `_id` (product id)
* `title`
* `summary`
* `merchantCode`
* `categoryId`
* `brandId`
* `variants: [ { subSku, price, attributes, stock, thumbnail } ]`
* `searchKeywords`

📌 Indexes optimize common queries:

* Full text: `title`, `summary`, `attributes`, `searchKeywords`
* Facets: `brandId`, `categoryId`, `merchantCode`
* Price filters: `variants.price`

---

## 🛠️ 4. Operational Workflow

### Write Flow (Source Truth)

```
create/update product → update variants → update inventories
```

### Sync Flow → Build Search Index

```
Mongo Change Stream or Kafka events → process into product_index → upsert document
```

### Read/Search Flow

```
Client → Search API → Query product_index → return matching variants/products
```

---

## 🚀 5. Setup Steps

1. Deploy MongoDB instance (standalone or replica set)
2. Run migration scripts to create all collections and indexes
3. Implement write APIs using source-of-truth collections
4. Implement indexing service:

    * Listen to product/variant/inventory changes
    * Build denormalized `product_index`
5. Use search queries primarily on `product_index`
6. Optimize indexing based on analytics and traffic patterns

---

## 📚 Notes / Best Practices

✔ Keep `product_index` lightweight and flattened for fast read search.
✔ Avoid storing large descriptions or media in `product_index`.
✔ Use **incremental rebuilds**, not full rebuilds.
✔ Shard on SKU or merchant if scaling >10M products.
✔ Use Atlas Search for faster full-text search if available.

---

### Status: **Approved architecture for scalable eCommerce catalog**.

---

**Next Step Options:**

* Generate Spring Data MongoDB models
* Index syncing microservice design (Kafka or Mongo Change Stream)
* REST/GraphQL query examples for faceted filtering

---

**End of Document**
