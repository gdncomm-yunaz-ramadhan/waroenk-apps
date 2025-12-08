# 🧾 Checkout System Plan

**Status:** ✅ Implemented

**Tech Stack:**  
- Spring Boot 3  
- Java 17+  
- Maven  
- MongoDB 6  
- Cursor-based pagination using existing customized `MongoPageable`
- **gRPC** for inter-microservice communication (Cart ↔ Catalog)  

---

## 1️⃣ System Requirements

- Each user has **one active cart**
- Items added to cart must have stock validated against inventory service
- Cart stores **snapshotted product data** (name, price, stock, attributes)
- At checkout:
  - System attempts **bulk stock lock**
  - Items with unavailable stock should be flagged and updated in cart
  - Items successfully locked are moved into a **checkout record**
- User finalizes checkout by selecting/creating a shipping address (stored as a snapshot)
- Checkout lifecycle statuses:  
  `WAITING → PAID | CANCELLED | EXPIRED`
- Expired checkouts release inventory lock
- API must allow:
  - Listing checkouts with cursor-based pagination
  - Getting checkout details
  - Simulating payment and cancellation for testing

---

## 2️⃣ Data Model Plan

### 📌 Cart Collection

| Field | Type |
|-------|------|
| `id` | UUID |
| `userId` | UUID (unique index) |
| `items` | List<CartItem> |
| `createdAt` | DateTime |
| `updatedAt` | DateTime |

---

### 📌 CartItem (Embedded)

| Field | Type |
|-------|------|
| `sku` | String |
| `name` | String |
| `price` | Decimal |
| `qtyRequested` | Integer |
| `availableStockSnapshot` | Integer |
| `attributes` | Map<String, String> |

---

### 📌 Checkout Collection

| Field | Type |
|-------|------|
| `checkoutId` | UUID |
| `userId` | UUID |
| `orderId` | String |
| `paymentCode` | String |
| `items` | List<CheckoutItem> |
| `status` | Enum(WAITING, CANCELLED, PAID) |
| `shippingAddress` | AddressSnapshot |
| `totalPrice` | Decimal |
| `expiresAt` | DateTime |
| `createdAt` | DateTime |

---

### 📌 CheckoutItem (Embedded)

Same structure as `CartItem` but represents final locked state.

---

### 📌 AddressSnapshot

| Field | Type |
|-------|------|
| `recipientName` | String |
| `phone` | String |
| `street` | String |
| `city` | String |
| `country` | String |
| `postalCode` | String |
| `notes` | Optional String |

---

## 3️⃣ Index Strategy

| Collection | Index |
|------------|-------|
| `cart` | `userId` (unique) |
| `checkout` | `userId`, `status`, `expiresAt`, `orderId` |
| `checkout` | Compound index → `(userId, createdAt DESC)` for cursor pagination |

---

## 4️⃣ API Workflow

### 4.1 Add Item to Cart  
`POST /cart/items`

Process:

1. Call inventory to check stock
2. If no stock → return error
3. Store updated snapshot data in cart
4. Return item status + remaining stock

---

### 4.2 Prepare Checkout (Bulk Lock)  
`POST /checkout/prepare`

Process:

1. Lookup cart items
2. Call catalog system bulk lock
3. For each SKU:
   - If lock failed → update cart qty to `0`
   - If lock success → include in checkout
4. If at least one valid item → create checkout entry with:
   - `WAITING` status
   - expiration timestamp

Response includes SKU lock summary.

---

### 4.3 Finalize Checkout (Select Address)  
`POST /checkout/{checkoutId}/finalize`

Process:

1. User selects existing address or creates new one
2. Store address as snapshot in checkout
3. Calculate total
4. Generate:
   - unique payment code
   - readable orderId

---

### 4.4 Payment Simulation  
`POST /checkout/{checkoutId}/pay`

- Update status → `PAID`
- Commit inventory lock permanently

---

### 4.5 Cancel Checkout  
`POST /checkout/{checkoutId}/cancel`

- Update status → `CANCELLED`
- Release locked inventory

---

## 5️⃣ Expiry Handling

A scheduled job runs periodically:

```
Find checkouts where status = WAITING and expiresAt < now()
→ Mark CANCELLED
→ Release inventory lock
```

In API responses:  
If checkout is WAITING and past expiresAt → **return status as EXPIRED (derived), do not update DB.**

---

## 6️⃣ Retrieval APIs

### 6.1 List Checkouts with Cursor Pagination  
`GET /checkouts?cursor=&orderId=`

Includes:
- Filters by orderId (optional)
- Translates expired WAITING checkouts to EXPIRED

---

### 6.2 Get Checkout Details  
`GET /checkouts/{checkoutId}`

Returns:
- Snapshot items
- Address
- Status (translated if expired)

---

## 7️⃣ Integration Points

| Operation | Inventory Action |
|----------|------------------|
| Add to Cart | Stock check |
| Prepare Checkout | Bulk temporary lock |
| Cancel Checkout | Release lock |
| Expired Checkout | Release lock |
| Payment Success | Convert temporary lock → permanent |

---

## 8️⃣ Future Enhancements (Optional)

| Feature | Purpose |
|--------|---------|
| Idempotency keys | Prevent duplicate payment |
| Redis caching stock | Reduce load on catalog system |
| Activity logs | Compliance tracking |

---

## ✔️ Summary

This plan ensures:

- Reliable stock handling via lock system  
- Immutable checkout data integrity  
- Clean pagination support  
- Flexible lifecycle with expiry and recovery

---

## 📝 Implementation Notes

### Files Modified/Created:

**Entities:**
- `Cart.java` - unchanged
- `CartItem.java` - added `subSku`, `availableStockSnapshot`
- `Checkout.java` - added `orderId`, `paymentCode`, `shippingAddress`, `currency`, `paidAt`, `cancelledAt`
- `CheckoutItem.java` - added `subSku`, `availableStockSnapshot`, `imageUrl`, `attributes`, `reservationError`
- `AddressSnapshot.java` - NEW embedded document

**gRPC Clients:**
- `CatalogGrpcClient.java` - calls InventoryService and VariantService
- `MemberGrpcClient.java` - calls AddressService

**Proto Files:**
- `cart_messages.proto` - updated CartItemData with new fields
- `checkout_messages.proto` - added AddressSnapshotData, new messages for checkout flow
- `checkout_service.proto` - added PrepareCheckout, FinalizeCheckout, PayCheckout, CancelCheckout RPCs

**Migration:**
- `V002_CheckoutSystemEnhancements.java` - new indexes for checkout flow

**Services:**
- `CartService.java` - added methods with stock validation via gRPC
- `CartServiceImpl.java` - integrated CatalogGrpcClient
- `CheckoutService.java` - new checkout flow methods
- `CheckoutServiceImpl.java` - full checkout flow with gRPC integration

**DTOs:**
- Updated cart and checkout DTOs to match new entity structure
- Added `AddCartItemResult`, `BulkAddCartItemsResult`
- Added `PrepareCheckoutResult`, `FinalizeCheckoutResult`, `PayCheckoutResult`  
