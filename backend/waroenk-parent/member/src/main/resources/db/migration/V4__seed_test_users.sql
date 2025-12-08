-- ============================================================================
-- V4: Seed 5000 test users with random addresses
-- Password: Testing@123 (BCrypt strength 12)
-- ============================================================================

-- Pre-computed BCrypt hash for "Testing@123" with strength 12
-- This hash was generated using: new BCryptPasswordEncoder(12).encode("Testing@123")
DO $$
DECLARE
    password_hash TEXT := '$2a$12$E6i6enoneTJbzk9mKbC4Zu4gWDmQs9aium5RBsZJiacJymAawGlQ2';
    user_count INT := 5000;
    i INT;
    new_user_id UUID;
    address_count INT;
    first_address_id UUID;
    j INT;
    random_gender CHAR(1);
    random_dob DATE;
    first_names TEXT[] := ARRAY[
        'Budi', 'Andi', 'Dewi', 'Siti', 'Agus', 'Eko', 'Rini', 'Sri', 'Dian', 'Rina',
        'Ahmad', 'Muhammad', 'Fitri', 'Indah', 'Hendra', 'Joko', 'Lina', 'Nina', 'Putri', 'Rizki',
        'Sari', 'Tono', 'Wati', 'Yani', 'Zaki', 'Ayu', 'Bagus', 'Citra', 'Doni', 'Eva',
        'Fajar', 'Gita', 'Hadi', 'Ika', 'Jaya', 'Kiki', 'Luki', 'Maya', 'Nani', 'Omar',
        'Pandu', 'Qori', 'Rudi', 'Sinta', 'Tari', 'Udin', 'Vera', 'Wawan', 'Xena', 'Yuda',
        'Zahra', 'Arif', 'Bella', 'Candra', 'Desi', 'Erwin', 'Fani', 'Galih', 'Hani', 'Irfan'
    ];
    last_names TEXT[] := ARRAY[
        'Santoso', 'Wijaya', 'Susanto', 'Pratama', 'Kusuma', 'Hidayat', 'Setiawan', 'Putra', 'Saputra', 'Wibowo',
        'Nugroho', 'Suryadi', 'Hartono', 'Gunawan', 'Siregar', 'Nasution', 'Harahap', 'Sitompul', 'Simbolon', 'Hutapea',
        'Manurung', 'Siahaan', 'Panjaitan', 'Hutabarat', 'Situmorang', 'Pardede', 'Sirait', 'Sinaga', 'Tampubolon', 'Aritonang',
        'Prasetyo', 'Suryanto', 'Kurniawan', 'Firmansyah', 'Ramadhan', 'Maulana', 'Saputro', 'Utomo', 'Prabowo', 'Aryanto'
    ];
    provinces TEXT[] := ARRAY['DKI Jakarta', 'Jawa Barat', 'Jawa Tengah', 'Jawa Timur', 'Banten', 'Yogyakarta', 'Bali', 'Sumatera Utara', 'Sumatera Selatan', 'Kalimantan Timur'];
    cities TEXT[] := ARRAY['Jakarta Selatan', 'Jakarta Pusat', 'Bandung', 'Surabaya', 'Semarang', 'Yogyakarta', 'Denpasar', 'Medan', 'Palembang', 'Balikpapan'];
    districts TEXT[] := ARRAY['Kebayoran Baru', 'Menteng', 'Coblong', 'Tegalsari', 'Semarang Tengah', 'Gondokusuman', 'Denpasar Barat', 'Medan Kota', 'Ilir Timur', 'Balikpapan Kota'];
    subdistricts TEXT[] := ARRAY['Senayan', 'Menteng', 'Dago', 'Wonokromo', 'Sekayu', 'Demangan', 'Dauh Puri', 'Petisah', 'Bukit Kecil', 'Klandasan'];
    streets TEXT[] := ARRAY['Jl. Sudirman', 'Jl. Thamrin', 'Jl. Gatot Subroto', 'Jl. Diponegoro', 'Jl. Ahmad Yani', 'Jl. Malioboro', 'Jl. Sunset Road', 'Jl. Pemuda', 'Jl. Merdeka', 'Jl. Asia Afrika'];
    labels TEXT[] := ARRAY['Rumah', 'Kantor', 'Apartemen', 'Kos', 'Rumah Orang Tua'];
    
    fname TEXT;
    lname TEXT;
    full_name TEXT;
    user_email TEXT;
    user_phone TEXT;
    province_idx INT;
    city_idx INT;
    postal_code TEXT;
BEGIN
    RAISE NOTICE 'Starting to seed % users...', user_count;
    
    FOR i IN 1..user_count LOOP
        -- Generate random name
        fname := first_names[1 + floor(random() * array_length(first_names, 1))::int];
        lname := last_names[1 + floor(random() * array_length(last_names, 1))::int];
        full_name := fname || ' ' || lname;
        
        -- Generate unique email and phone
        user_email := lower(fname) || '.' || lower(lname) || '.' || i || '@testmail.com';
        user_phone := '+628' || lpad((floor(random() * 900000000 + 100000000)::bigint)::text, 10, '0');
        
        -- Random gender
        random_gender := CASE WHEN random() < 0.5 THEN 'M' ELSE 'F' END;
        
        -- Random DOB between 1970 and 2005
        random_dob := '1970-01-01'::date + (floor(random() * 12775)::int);
        
        -- Insert user
        INSERT INTO users (id, fullname, dob, email, phone_number, gender, password_hash, created_at, updated_at)
        VALUES (
            uuid_generate_v4(),
            full_name,
            random_dob,
            user_email,
            user_phone,
            random_gender,
            password_hash,
            CURRENT_TIMESTAMP - (random() * interval '365 days'),
            CURRENT_TIMESTAMP
        )
        RETURNING id INTO new_user_id;
        
        -- Random number of addresses (0-5)
        address_count := floor(random() * 6)::int;
        first_address_id := NULL;
        
        FOR j IN 1..address_count LOOP
            province_idx := 1 + floor(random() * array_length(provinces, 1))::int;
            city_idx := 1 + floor(random() * array_length(cities, 1))::int;
            postal_code := (10000 + floor(random() * 89999)::int)::text;
            
            INSERT INTO addresses (
                id, user_id, longitude, latitude, country, postal_code, province, city, 
                district, subdistrict, street, details, label, created_at, updated_at
            )
            VALUES (
                uuid_generate_v4(),
                new_user_id,
                106.8 + (random() * 0.2),  -- Jakarta area longitude
                -6.2 + (random() * 0.1),   -- Jakarta area latitude
                'Indonesia',
                postal_code,
                provinces[province_idx],
                cities[city_idx],
                districts[1 + floor(random() * array_length(districts, 1))::int],
                subdistricts[1 + floor(random() * array_length(subdistricts, 1))::int],
                streets[1 + floor(random() * array_length(streets, 1))::int] || ' No. ' || (1 + floor(random() * 200)::int),
                CASE WHEN random() < 0.7 THEN 'Blok ' || chr(65 + floor(random() * 26)::int) || floor(random() * 20)::int ELSE NULL END,
                labels[1 + floor(random() * array_length(labels, 1))::int],
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            RETURNING id INTO first_address_id;
            
            -- Set first address as default
            IF j = 1 AND first_address_id IS NOT NULL THEN
                UPDATE users SET default_address_id = first_address_id WHERE id = new_user_id;
            END IF;
        END LOOP;
        
        -- Progress logging every 500 users
        IF i % 500 = 0 THEN
            RAISE NOTICE 'Created % users...', i;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Completed seeding % users!', user_count;
END $$;

