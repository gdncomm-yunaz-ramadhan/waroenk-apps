#!/usr/bin/env python3
"""
Product Description Generator using Mistral AI API
Generates descriptions for 10,000+ product variants across 100 categories
"""

import json
import requests
import time
import os
import random
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

# Configuration
API_URL = "https://api.mistral.ai/v1/conversations"
API_KEY = "M24widlbnEzcksIwoeOXF9II5ntz4xwV"
AGENT_ID = "ag_019af5e7cc3c77a6a4b5d213931ecb60"

# Output files
OUTPUT_FILE = "product_descriptions.json"
PROGRESS_FILE = "generation_progress.json"
ERROR_LOG_FILE = "generation_errors.log"

# Rate limiting
REQUESTS_PER_SECOND = 2  # Adjust based on API limits
RETRY_ATTEMPTS = 3
RETRY_DELAY = 5  # seconds

# Categories with subcategories for product generation
CATEGORIES = {
    "Electronics": {
        "subcategories": ["Smartphones", "Laptops & Computers", "Tablets", "Cameras & Photography", 
                         "Audio & Headphones", "Wearables & Smartwatches", "Gaming", 
                         "Electronics Accessories", "Smart Home"],
        "brands": ["Apple", "Samsung", "Sony", "LG Electronics", "Xiaomi", "Dell", "HP", "Lenovo", 
                  "ASUS", "Acer", "Microsoft", "Logitech", "Razer", "Canon", "Nikon", "GoPro",
                  "JBL", "Bose", "Sennheiser", "Beats", "Marshall", "Nintendo", "PlayStation", "Xbox",
                  "Anker", "Belkin", "Fitbit", "Garmin"],
        "products": {
            "Smartphones": ["Pro Max", "Ultra", "Plus", "Lite", "Mini", "5G", "SE", "Note", "Fold", "Flip"],
            "Laptops & Computers": ["Gaming Laptop", "Ultrabook", "Chromebook", "Workstation", "All-in-One PC", 
                                   "Desktop Computer", "Mini PC", "2-in-1 Laptop", "Business Laptop"],
            "Tablets": ["Pro", "Air", "Mini", "Tab", "Pad", "Slate", "Surface"],
            "Cameras & Photography": ["DSLR Camera", "Mirrorless Camera", "Action Camera", "Compact Camera",
                                      "Camera Lens", "Tripod", "Camera Bag", "Memory Card", "Camera Flash"],
            "Audio & Headphones": ["Wireless Headphones", "Earbuds", "Over-Ear Headphones", "Bluetooth Speaker",
                                  "Soundbar", "Home Theater System", "Microphone", "DAC Amplifier"],
            "Wearables & Smartwatches": ["Smartwatch", "Fitness Tracker", "Smart Ring", "Smart Glasses"],
            "Gaming": ["Gaming Console", "Gaming Controller", "Gaming Headset", "Gaming Mouse", 
                      "Gaming Keyboard", "Gaming Chair", "Gaming Monitor", "VR Headset"],
            "Electronics Accessories": ["Phone Case", "Screen Protector", "Charging Cable", "Power Bank",
                                       "Wireless Charger", "USB Hub", "Laptop Stand", "Webcam"],
            "Smart Home": ["Smart Speaker", "Smart Display", "Smart Light Bulb", "Smart Thermostat",
                          "Smart Lock", "Security Camera", "Smart Plug", "Robot Vacuum"]
        }
    },
    "Fashion": {
        "subcategories": ["Men's Clothing", "Women's Clothing", "Shoes & Footwear", "Bags & Luggage",
                         "Watches", "Jewelry & Accessories", "Sportswear", "Eyewear", "Fashion Accessories"],
        "brands": ["Nike", "Adidas", "Puma", "Uniqlo", "Zara", "H&M", "Under Armour", "New Balance",
                  "Reebok", "ASICS", "Converse", "Vans", "Crocs", "Birkenstock", "Dr. Martens",
                  "Timberland", "The North Face", "Patagonia", "Columbia", "Fossil", "Casio",
                  "Seiko", "Citizen", "Timex", "Samsonite", "American Tourister", "Herschel", "Fjällräven"],
        "products": {
            "Men's Clothing": ["T-Shirt", "Polo Shirt", "Dress Shirt", "Jeans", "Chinos", "Shorts",
                              "Jacket", "Hoodie", "Sweater", "Suit", "Blazer", "Underwear", "Socks"],
            "Women's Clothing": ["Blouse", "Dress", "Skirt", "Jeans", "Leggings", "Cardigan",
                                "Jacket", "Coat", "Sweater", "Jumpsuit", "Romper", "Tank Top"],
            "Shoes & Footwear": ["Running Shoes", "Sneakers", "Boots", "Sandals", "Loafers",
                                "High Heels", "Flats", "Slip-Ons", "Athletic Shoes", "Hiking Boots"],
            "Bags & Luggage": ["Backpack", "Handbag", "Crossbody Bag", "Tote Bag", "Luggage",
                              "Carry-On", "Duffel Bag", "Messenger Bag", "Clutch", "Wallet"],
            "Watches": ["Analog Watch", "Digital Watch", "Chronograph", "Dive Watch", "Dress Watch",
                       "Sport Watch", "Smartwatch Band"],
            "Jewelry & Accessories": ["Necklace", "Bracelet", "Ring", "Earrings", "Cufflinks",
                                     "Tie", "Belt", "Scarf", "Hat", "Beanie"],
            "Sportswear": ["Athletic Shorts", "Sports Bra", "Compression Shirt", "Track Pants",
                          "Yoga Pants", "Gym Tank Top", "Windbreaker", "Rain Jacket"],
            "Eyewear": ["Sunglasses", "Reading Glasses", "Blue Light Glasses", "Aviator", "Wayfarer"],
            "Fashion Accessories": ["Wallet", "Card Holder", "Phone Wallet", "Key Holder", "Passport Holder"]
        }
    },
    "Home & Living": {
        "subcategories": ["Furniture", "Kitchen & Dining", "Bedding & Bath", "Home Decor",
                         "Lighting", "Storage & Organization", "Garden & Outdoor", 
                         "Cleaning & Laundry", "Home Appliances"],
        "brands": ["IKEA", "Muji", "Philips", "Panasonic", "Dyson", "Electrolux", "Bosch",
                  "KitchenAid", "Cuisinart", "Breville", "Nespresso"],
        "products": {
            "Furniture": ["Sofa", "Armchair", "Coffee Table", "Dining Table", "Office Chair",
                         "Bookshelf", "TV Stand", "Bed Frame", "Wardrobe", "Desk", "Side Table"],
            "Kitchen & Dining": ["Cookware Set", "Knife Set", "Cutting Board", "Dinnerware Set",
                                "Glassware", "Utensil Set", "Mixing Bowls", "Bakeware", "Storage Containers"],
            "Bedding & Bath": ["Bed Sheet Set", "Duvet Cover", "Pillows", "Mattress Topper",
                              "Towel Set", "Bath Mat", "Shower Curtain", "Blanket", "Comforter"],
            "Home Decor": ["Wall Art", "Vase", "Candle", "Photo Frame", "Mirror", "Rug",
                          "Curtains", "Throw Pillow", "Clock", "Plant Pot"],
            "Lighting": ["Table Lamp", "Floor Lamp", "Pendant Light", "LED Strip", "Desk Lamp",
                        "Ceiling Light", "Wall Sconce", "Smart Light Bulb"],
            "Storage & Organization": ["Storage Box", "Drawer Organizer", "Shoe Rack", "Closet Organizer",
                                      "Laundry Basket", "Shelf Unit", "Hooks", "Bins"],
            "Garden & Outdoor": ["Patio Furniture", "Garden Tools", "Planter", "Outdoor Lighting",
                                "BBQ Grill", "Hammock", "Garden Hose", "Lawn Mower"],
            "Cleaning & Laundry": ["Vacuum Cleaner", "Mop", "Broom", "Cleaning Spray", "Laundry Detergent",
                                  "Iron", "Steamer", "Lint Roller"],
            "Home Appliances": ["Air Conditioner", "Fan", "Heater", "Air Purifier", "Humidifier",
                               "Dehumidifier", "Water Purifier", "Dishwasher"]
        }
    },
    "Beauty & Health": {
        "subcategories": ["Skincare", "Makeup & Cosmetics", "Hair Care", "Fragrances & Perfumes",
                         "Personal Care", "Vitamins & Supplements", "Medical Supplies",
                         "Fitness & Wellness", "Oral Care"],
        "brands": ["L'Oréal", "Maybelline", "MAC", "Clinique", "Estée Lauder", "SK-II",
                  "The Body Shop", "Innisfree", "Laneige", "Sulwhasoo", "Philips", "Dyson"],
        "products": {
            "Skincare": ["Moisturizer", "Serum", "Cleanser", "Toner", "Sunscreen", "Face Mask",
                        "Eye Cream", "Night Cream", "Exfoliator", "Essence"],
            "Makeup & Cosmetics": ["Foundation", "Concealer", "Lipstick", "Mascara", "Eyeshadow Palette",
                                  "Blush", "Bronzer", "Highlighter", "Setting Powder", "Makeup Brush Set"],
            "Hair Care": ["Shampoo", "Conditioner", "Hair Mask", "Hair Oil", "Hair Serum",
                         "Hair Spray", "Hair Dryer", "Straightener", "Curling Iron"],
            "Fragrances & Perfumes": ["Eau de Parfum", "Eau de Toilette", "Body Mist", "Cologne",
                                     "Perfume Set", "Fragrance Oil"],
            "Personal Care": ["Body Lotion", "Body Wash", "Deodorant", "Razor", "Electric Shaver",
                             "Trimmer", "Epilator", "Body Scrub"],
            "Vitamins & Supplements": ["Multivitamin", "Vitamin C", "Vitamin D", "Omega-3",
                                      "Protein Powder", "Collagen", "Probiotics"],
            "Medical Supplies": ["First Aid Kit", "Blood Pressure Monitor", "Thermometer",
                                "Pulse Oximeter", "Knee Brace", "Back Support"],
            "Fitness & Wellness": ["Yoga Mat", "Resistance Bands", "Dumbbells", "Foam Roller",
                                  "Massage Gun", "Fitness Tracker", "Jump Rope"],
            "Oral Care": ["Electric Toothbrush", "Toothpaste", "Mouthwash", "Dental Floss",
                         "Teeth Whitening Kit", "Water Flosser"]
        }
    },
    "Sports & Outdoor": {
        "subcategories": ["Exercise & Fitness Equipment", "Camping & Hiking", "Cycling",
                         "Swimming & Water Sports", "Team Sports", "Racket Sports",
                         "Fishing", "Golf", "Martial Arts & Boxing"],
        "brands": ["Nike", "Adidas", "Under Armour", "Puma", "The North Face", "Patagonia",
                  "Columbia", "Garmin", "Fitbit"],
        "products": {
            "Exercise & Fitness Equipment": ["Treadmill", "Exercise Bike", "Elliptical", "Weight Bench",
                                            "Kettlebell", "Pull-Up Bar", "Ab Roller", "Battle Ropes"],
            "Camping & Hiking": ["Tent", "Sleeping Bag", "Camping Stove", "Hiking Backpack",
                                "Headlamp", "Trekking Poles", "Water Bottle", "Compass"],
            "Cycling": ["Road Bike", "Mountain Bike", "Bike Helmet", "Cycling Jersey", "Bike Lock",
                       "Bike Pump", "Cycling Gloves", "Bike Light"],
            "Swimming & Water Sports": ["Swimsuit", "Goggles", "Swimming Cap", "Snorkel Set",
                                       "Life Jacket", "Paddle Board", "Kayak", "Wet Suit"],
            "Team Sports": ["Soccer Ball", "Basketball", "Football", "Volleyball", "Baseball Glove",
                           "Hockey Stick", "Team Jersey"],
            "Racket Sports": ["Tennis Racket", "Badminton Racket", "Table Tennis Paddle",
                             "Squash Racket", "Tennis Balls", "Shuttlecocks"],
            "Fishing": ["Fishing Rod", "Fishing Reel", "Tackle Box", "Fishing Line", "Fishing Lures",
                       "Fishing Net", "Waders"],
            "Golf": ["Golf Clubs", "Golf Bag", "Golf Balls", "Golf Gloves", "Golf Shoes",
                    "Golf Tees", "Rangefinder"],
            "Martial Arts & Boxing": ["Boxing Gloves", "Punching Bag", "MMA Gloves", "Shin Guards",
                                     "Headgear", "Hand Wraps", "Mouth Guard"]
        }
    },
    "Baby & Kids": {
        "subcategories": ["Baby Gear & Furniture", "Baby Clothing", "Toys & Games",
                         "Feeding & Nursing", "Diapering & Potty", "Kids Fashion",
                         "School Supplies", "Outdoor Play", "Educational Toys"],
        "brands": ["LEGO", "Hasbro", "Mattel", "Fisher-Price", "Hot Wheels", "Bandai Namco", "Funko"],
        "products": {
            "Baby Gear & Furniture": ["Stroller", "Car Seat", "Crib", "High Chair", "Baby Monitor",
                                     "Bouncer", "Swing", "Playpen", "Changing Table"],
            "Baby Clothing": ["Onesie", "Baby Romper", "Baby Dress", "Baby Jacket", "Baby Socks",
                             "Baby Hat", "Baby Mittens", "Baby Shoes"],
            "Toys & Games": ["Action Figure", "Doll", "Board Game", "Puzzle", "Building Blocks",
                            "Remote Control Car", "Stuffed Animal", "Play Set"],
            "Feeding & Nursing": ["Baby Bottle", "Breast Pump", "Bottle Warmer", "Sippy Cup",
                                 "Baby Food Maker", "Nursing Pillow", "Bibs"],
            "Diapering & Potty": ["Diapers", "Diaper Bag", "Wipes", "Changing Pad", "Potty Trainer",
                                 "Diaper Rash Cream"],
            "Kids Fashion": ["Kids T-Shirt", "Kids Jeans", "Kids Dress", "Kids Sneakers",
                            "Kids Backpack", "Kids Watch"],
            "School Supplies": ["Backpack", "Pencil Case", "Notebooks", "Crayons", "Markers",
                               "Ruler", "Calculator", "Lunch Box"],
            "Outdoor Play": ["Swing Set", "Trampoline", "Sandbox", "Play Tent", "Scooter",
                            "Balance Bike", "Bubble Machine"],
            "Educational Toys": ["Learning Tablet", "Flash Cards", "Science Kit", "Coding Robot",
                                "Musical Instruments", "Art Set", "Microscope"]
        }
    },
    "Automotive": {
        "subcategories": ["Car Parts & Accessories", "Motorcycle Parts", "Car Electronics",
                         "Car Care & Maintenance", "Interior Accessories", "Exterior Accessories",
                         "Tires & Wheels", "Tools & Equipment", "Oils & Fluids"],
        "brands": ["Bosch", "Stanley", "DeWalt", "Makita", "Milwaukee", "Black+Decker", "Philips", "Garmin"],
        "products": {
            "Car Parts & Accessories": ["Air Filter", "Oil Filter", "Brake Pads", "Spark Plugs",
                                       "Battery", "Alternator", "Radiator", "Wiper Blades"],
            "Motorcycle Parts": ["Motorcycle Helmet", "Motorcycle Gloves", "Motorcycle Jacket",
                                "Exhaust System", "Motorcycle Cover", "Saddlebags"],
            "Car Electronics": ["Dash Cam", "GPS Navigator", "Car Stereo", "Backup Camera",
                               "OBD2 Scanner", "Car Charger", "Bluetooth Adapter"],
            "Car Care & Maintenance": ["Car Wax", "Car Shampoo", "Tire Shine", "Glass Cleaner",
                                      "Microfiber Cloth", "Detailing Kit", "Car Polish"],
            "Interior Accessories": ["Car Seat Cover", "Floor Mats", "Steering Wheel Cover",
                                    "Sun Shade", "Air Freshener", "Phone Mount", "Trash Can"],
            "Exterior Accessories": ["Roof Rack", "Bike Rack", "Car Cover", "Mud Flaps",
                                    "Door Edge Guards", "License Plate Frame"],
            "Tires & Wheels": ["All-Season Tires", "Winter Tires", "Alloy Wheels", "Wheel Covers",
                              "Tire Pressure Gauge", "Jack Stand"],
            "Tools & Equipment": ["Socket Set", "Wrench Set", "Screwdriver Set", "Pliers",
                                 "Torque Wrench", "Jumper Cables", "Tire Inflator"],
            "Oils & Fluids": ["Motor Oil", "Transmission Fluid", "Brake Fluid", "Coolant",
                             "Power Steering Fluid", "Windshield Washer Fluid"]
        }
    },
    "Books & Media": {
        "subcategories": ["Fiction", "Non-Fiction", "Textbooks & Education", "Comics & Manga",
                         "Music", "Movies & TV", "Magazines", "E-Books & Audiobooks", "Stationery"],
        "brands": ["Penguin Books", "HarperCollins", "Random House", "Marvel", "DC Comics"],
        "products": {
            "Fiction": ["Novel", "Mystery Book", "Romance Novel", "Science Fiction Book",
                       "Fantasy Book", "Thriller", "Historical Fiction", "Short Stories"],
            "Non-Fiction": ["Biography", "Self-Help Book", "Business Book", "Cookbook",
                           "Travel Guide", "History Book", "Science Book", "Memoir"],
            "Textbooks & Education": ["Math Textbook", "Science Textbook", "Language Textbook",
                                     "Study Guide", "Workbook", "Reference Book", "Dictionary"],
            "Comics & Manga": ["Comic Book", "Manga Volume", "Graphic Novel", "Comic Collection",
                              "Superhero Comic", "Anime Art Book"],
            "Music": ["Vinyl Record", "CD Album", "Music Box", "Sheet Music", "Guitar Songbook"],
            "Movies & TV": ["DVD", "Blu-ray", "Box Set", "Documentary", "Collector's Edition"],
            "Magazines": ["Fashion Magazine", "Tech Magazine", "Sports Magazine", "Lifestyle Magazine"],
            "E-Books & Audiobooks": ["E-Book", "Audiobook", "E-Reader", "Audiobook Subscription"],
            "Stationery": ["Notebook", "Journal", "Planner", "Pens", "Pencils", "Markers",
                          "Sticky Notes", "Envelopes", "Letter Paper"]
        }
    },
    "Food & Beverage": {
        "subcategories": ["Snacks & Confectionery", "Beverages", "Coffee & Tea",
                         "Dairy & Eggs", "Bakery & Bread", "Frozen Foods",
                         "Condiments & Sauces", "Organic & Health Foods", "Instant & Ready Meals"],
        "brands": ["Nespresso"],
        "products": {
            "Snacks & Confectionery": ["Chocolate Bar", "Chips", "Cookies", "Crackers", "Candy",
                                      "Nuts", "Dried Fruits", "Popcorn", "Granola Bar"],
            "Beverages": ["Soda", "Juice", "Energy Drink", "Sports Drink", "Mineral Water",
                         "Flavored Water", "Coconut Water"],
            "Coffee & Tea": ["Coffee Beans", "Ground Coffee", "Instant Coffee", "Tea Bags",
                            "Loose Leaf Tea", "Matcha Powder", "Coffee Capsules"],
            "Dairy & Eggs": ["Milk", "Cheese", "Yogurt", "Butter", "Cream", "Eggs"],
            "Bakery & Bread": ["Bread", "Bagels", "Croissant", "Muffins", "Cake", "Pastries"],
            "Frozen Foods": ["Ice Cream", "Frozen Pizza", "Frozen Vegetables", "Frozen Meals",
                            "Frozen Seafood", "Frozen Meat"],
            "Condiments & Sauces": ["Ketchup", "Mustard", "Mayonnaise", "Soy Sauce", "Hot Sauce",
                                   "BBQ Sauce", "Salad Dressing", "Olive Oil"],
            "Organic & Health Foods": ["Organic Snacks", "Protein Bar", "Superfood Powder",
                                      "Organic Cereal", "Plant-Based Milk", "Vegan Snacks"],
            "Instant & Ready Meals": ["Instant Noodles", "Ready-to-Eat Meals", "Soup",
                                     "Microwave Meals", "Canned Food"]
        }
    },
    "Pet Supplies": {
        "subcategories": ["Dog Supplies", "Cat Supplies", "Fish & Aquarium",
                         "Bird Supplies", "Small Pets"],
        "brands": [],
        "products": {
            "Dog Supplies": ["Dog Food", "Dog Treats", "Dog Bed", "Dog Collar", "Dog Leash",
                            "Dog Toys", "Dog Crate", "Dog Shampoo", "Dog Bowl"],
            "Cat Supplies": ["Cat Food", "Cat Litter", "Cat Tree", "Cat Toys", "Cat Carrier",
                            "Cat Bed", "Cat Scratching Post", "Cat Treats"],
            "Fish & Aquarium": ["Fish Tank", "Aquarium Filter", "Fish Food", "Aquarium Heater",
                               "Air Pump", "Aquarium Decorations", "Fish Net"],
            "Bird Supplies": ["Bird Cage", "Bird Food", "Bird Toys", "Bird Perch",
                             "Bird Bath", "Bird Treats"],
            "Small Pets": ["Hamster Cage", "Guinea Pig Food", "Rabbit Hutch", "Small Animal Bedding",
                          "Exercise Wheel", "Small Animal Treats"]
        }
    }
}

# Product modifiers for variety
MODIFIERS = {
    "quality": ["Premium", "Professional", "Deluxe", "Essential", "Classic", "Pro", "Elite", 
                "Advanced", "Ultra", "Limited Edition", "Signature", "Original"],
    "size": ["Small", "Medium", "Large", "XL", "XXL", "Compact", "Mini", "Full-Size", "Travel-Size"],
    "color": ["Black", "White", "Navy Blue", "Gray", "Red", "Green", "Pink", "Brown", 
              "Beige", "Purple", "Orange", "Yellow", "Multicolor"],
    "material": ["Cotton", "Leather", "Stainless Steel", "Aluminum", "Bamboo", "Organic", 
                 "Recycled", "Synthetic", "Mesh", "Silicone"],
    "style": ["Modern", "Vintage", "Minimalist", "Bohemian", "Industrial", "Scandinavian", 
              "Contemporary", "Casual", "Formal", "Athletic"],
    "feature": ["Wireless", "Waterproof", "Portable", "Foldable", "Adjustable", "Rechargeable",
               "Smart", "Ergonomic", "Eco-Friendly", "Anti-Slip"]
}


def generate_product_title(category_name, subcategory, product, brand=None):
    """Generate a realistic product title"""
    parts = []
    
    # Add brand if available
    if brand:
        parts.append(brand)
    
    # Randomly add modifiers
    if random.random() > 0.5:
        modifier_type = random.choice(list(MODIFIERS.keys()))
        parts.append(random.choice(MODIFIERS[modifier_type]))
    
    # Add product name
    parts.append(product)
    
    # Sometimes add additional descriptor
    if random.random() > 0.7:
        additional = random.choice([
            "for Men", "for Women", "for Kids", "Set", "Bundle", 
            "2024 Edition", "Version 2.0", "Plus", "Lite"
        ])
        parts.append(additional)
    
    return " ".join(parts)


def generate_all_product_titles():
    """Generate 10,000+ product titles across all categories"""
    titles = []
    category_count = {}
    
    # Calculate products per category to reach 10,000
    num_categories = len(CATEGORIES)
    base_products_per_category = 10000 // num_categories
    
    for category_name, category_data in CATEGORIES.items():
        subcategories = category_data["subcategories"]
        brands = category_data.get("brands", [])
        products = category_data.get("products", {})
        
        products_generated = 0
        target_per_category = base_products_per_category + random.randint(0, 50)
        
        while products_generated < target_per_category:
            for subcat in subcategories:
                if subcat in products:
                    product_list = products[subcat]
                    for product in product_list:
                        # Generate variations with and without brands
                        brand = random.choice(brands) if brands and random.random() > 0.3 else None
                        title = generate_product_title(category_name, subcat, product, brand)
                        
                        titles.append({
                            "title": title,
                            "category": category_name,
                            "subcategory": subcat,
                            "brand": brand
                        })
                        products_generated += 1
                        
                        if products_generated >= target_per_category:
                            break
                    
                    if products_generated >= target_per_category:
                        break
            
            # If we still need more, generate additional variations
            if products_generated < target_per_category:
                for subcat in subcategories:
                    if subcat in products:
                        for product in products[subcat]:
                            for _ in range(3):  # Generate 3 variations
                                brand = random.choice(brands) if brands and random.random() > 0.3 else None
                                title = generate_product_title(category_name, subcat, product, brand)
                                
                                titles.append({
                                    "title": title,
                                    "category": category_name,
                                    "subcategory": subcat,
                                    "brand": brand
                                })
                                products_generated += 1
                                
                                if products_generated >= target_per_category:
                                    break
                            
                            if products_generated >= target_per_category:
                                break
                    
                    if products_generated >= target_per_category:
                        break
        
        category_count[category_name] = products_generated
    
    # Remove duplicates while preserving order
    seen = set()
    unique_titles = []
    for item in titles:
        if item["title"] not in seen:
            seen.add(item["title"])
            unique_titles.append(item)
    
    print(f"\n📊 Product Distribution by Category:")
    for cat, count in category_count.items():
        print(f"   {cat}: {count} products")
    print(f"\n✅ Total unique products generated: {len(unique_titles)}")
    
    return unique_titles


def call_mistral_api(product_title, retry_count=0):
    """Call Mistral AI API to generate product description"""
    headers = {
        "Content-Type": "application/json",
        "X-API-KEY": API_KEY
    }
    
    payload = {
        "agent_id": AGENT_ID,
        "inputs": f"Generate product description for: {product_title}"
    }
    
    try:
        response = requests.post(API_URL, headers=headers, json=payload, timeout=60)
        response.raise_for_status()
        
        data = response.json()
        
        # Extract content from response
        if "outputs" in data and len(data["outputs"]) > 0:
            content = data["outputs"][0].get("content", "")
            return {
                "success": True,
                "description": content,
                "conversation_id": data.get("conversation_id"),
                "tokens_used": data.get("usage", {}).get("total_tokens", 0)
            }
        else:
            return {
                "success": False,
                "error": "No output in response",
                "response": data
            }
            
    except requests.exceptions.Timeout:
        if retry_count < RETRY_ATTEMPTS:
            time.sleep(RETRY_DELAY)
            return call_mistral_api(product_title, retry_count + 1)
        return {"success": False, "error": "Request timeout"}
        
    except requests.exceptions.RequestException as e:
        if retry_count < RETRY_ATTEMPTS:
            time.sleep(RETRY_DELAY)
            return call_mistral_api(product_title, retry_count + 1)
        return {"success": False, "error": str(e)}


def load_progress():
    """Load progress from previous run"""
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, 'r') as f:
            return json.load(f)
    return {"processed_titles": [], "last_index": 0}


def save_progress(progress):
    """Save progress to file"""
    with open(PROGRESS_FILE, 'w') as f:
        json.dump(progress, f)


def load_existing_results():
    """Load existing results from output file"""
    if os.path.exists(OUTPUT_FILE):
        with open(OUTPUT_FILE, 'r') as f:
            return json.load(f)
    return []


def save_results(results):
    """Save results to output file"""
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)


def log_error(title, error):
    """Log errors to file"""
    with open(ERROR_LOG_FILE, 'a') as f:
        timestamp = datetime.now().isoformat()
        f.write(f"[{timestamp}] {title}: {error}\n")


def process_single_product(product_data, index, total):
    """Process a single product and return result"""
    title = product_data["title"]
    
    print(f"  [{index+1}/{total}] Processing: {title[:60]}...")
    
    result = call_mistral_api(title)
    
    if result["success"]:
        return {
            "title": title,
            "description": result["description"],
            "category": product_data.get("category"),
            "subcategory": product_data.get("subcategory"),
            "brand": product_data.get("brand")
        }
    else:
        log_error(title, result.get("error", "Unknown error"))
        return None


def main():
    """Main execution function"""
    print("=" * 60)
    print("🚀 Product Description Generator")
    print("   Using Mistral AI API")
    print("=" * 60)
    
    # Generate product titles
    print("\n📝 Generating product titles...")
    all_products = generate_all_product_titles()
    
    # Load previous progress
    progress = load_progress()
    results = load_existing_results()
    processed_set = set(progress.get("processed_titles", []))
    
    # Filter out already processed products
    remaining_products = [p for p in all_products if p["title"] not in processed_set]
    
    if processed_set:
        print(f"\n📂 Resuming from previous run: {len(processed_set)} already processed")
    
    print(f"\n🔄 Processing {len(remaining_products)} products...")
    print(f"   Rate limit: {REQUESTS_PER_SECOND} requests/second")
    print(f"   Estimated time: ~{len(remaining_products) / REQUESTS_PER_SECOND / 60:.1f} minutes\n")
    
    # Process products with rate limiting
    total = len(remaining_products)
    successful = 0
    failed = 0
    
    start_time = time.time()
    
    for i, product_data in enumerate(remaining_products):
        title = product_data["title"]
        
        # Rate limiting
        if i > 0:
            time.sleep(1 / REQUESTS_PER_SECOND)
        
        result = process_single_product(product_data, i, total)
        
        if result:
            results.append(result)
            processed_set.add(title)
            successful += 1
        else:
            failed += 1
        
        # Save progress every 10 products
        if (i + 1) % 10 == 0:
            progress["processed_titles"] = list(processed_set)
            progress["last_index"] = i
            save_progress(progress)
            save_results(results)
            
            elapsed = time.time() - start_time
            rate = (i + 1) / elapsed if elapsed > 0 else 0
            remaining_time = (total - i - 1) / rate if rate > 0 else 0
            
            print(f"\n   💾 Progress saved: {i+1}/{total} ({successful} success, {failed} failed)")
            print(f"   ⏱️  Estimated time remaining: {remaining_time/60:.1f} minutes\n")
    
    # Final save
    progress["processed_titles"] = list(processed_set)
    progress["completed"] = True
    save_progress(progress)
    save_results(results)
    
    elapsed_total = time.time() - start_time
    
    print("\n" + "=" * 60)
    print("✅ GENERATION COMPLETE!")
    print("=" * 60)
    print(f"\n📊 Summary:")
    print(f"   Total products processed: {total}")
    print(f"   Successful: {successful}")
    print(f"   Failed: {failed}")
    print(f"   Total time: {elapsed_total/60:.1f} minutes")
    print(f"\n📁 Output files:")
    print(f"   Results: {OUTPUT_FILE}")
    print(f"   Progress: {PROGRESS_FILE}")
    if failed > 0:
        print(f"   Errors: {ERROR_LOG_FILE}")
    print()


if __name__ == "__main__":
    main()



