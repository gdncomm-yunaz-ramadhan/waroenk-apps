# Seed Data

This directory contains seed data that is packaged with the catalog service JAR.

## Current Contents

- `categories.json` - Product categories (~52 categories, ~9KB)

## Large Data Files

Large seed files (brands, merchants, products) have been moved to the `data/` directory
at the project root to avoid bloating the repository. These are seeded separately via script.

**Moved files:**
- `brands.json` → `data/brands.json` (114K brands, 18MB)
- `merchants.json` → `data/merchants.json` (100K merchants, 32MB)

## Seeding Process

### 1. Categories (Automatic)

Categories are automatically seeded via Mongock migration when the service starts.
The migration uses checksum validation to avoid re-seeding unchanged data.

### 2. Brands, Merchants, Products (Manual)

Use the Python script in the `data/` directory:

```bash
# Install dependency
pip install pymongo

# Seed everything
python data/seed_mongodb.py --all

# Or seed specific collections
python data/seed_mongodb.py --brands
python data/seed_mongodb.py --merchants
python data/seed_mongodb.py --products

# Custom MongoDB connection
python data/seed_mongodb.py --mongo-uri "mongodb://localhost:27017" --database catalog --all
```

### 3. TypeSense Indexing

After seeding, index data in TypeSense via gRPC:

```bash
# Using grpcurl
grpcurl -plaintext localhost:9090 catalog.SeedService/indexAllInTypeSense
```

## Data Structure

### categories.json

```json
{
  "id": "cat-electronics",
  "name": "Electronics",
  "slug": "electronics",
  "parentId": null,
  "iconUrl": "https://img.icons8.com/color/96/electronics.png"
}
```

### brands.json (in data/)

```json
{
  "id": "brand-xxx",
  "name": "Brand Name",
  "slug": "brand-name",
  "iconUrl": "https://..."
}
```

### merchants.json (in data/)

```json
{
  "id": "mch-xxx",
  "name": "Merchant Name",
  "code": "MCH00001",
  "iconUrl": "https://...",
  "location": "City",
  "contact": {
    "phone": "+62...",
    "email": "info@..."
  },
  "rating": 4.5
}
```

## Source Data

The seed data is generated from various e-commerce datasets:
- Flipkart Fashion Products
- Walmart Products
- Amazon Electronics
- Home Depot Products
- Croma Electronics
- Grocery Data
- AliExpress Products

Processing scripts are located in `data/` directory.
