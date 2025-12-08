-- Enable UUID extension (safe if already exists)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable CITEXT extension for case-insensitive text
CREATE EXTENSION IF NOT EXISTS "citext";


----------------------------------------------------------------
-- USERS TABLE
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables WHERE tablename = 'users'
    ) THEN
        CREATE TABLE users (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

            fullname CITEXT NOT NULL,  -- Case-insensitive for searches
            dob DATE NULL,

            email VARCHAR(255) UNIQUE,  -- Will be lowercased via trigger
            phone_number VARCHAR(50) UNIQUE,

            gender CHAR(1) CHECK (gender IN ('M', 'F', 'O')),

            password_hash TEXT NOT NULL,

            default_address_id UUID NULL,

            created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;


----------------------------------------------------------------
-- FUNCTION: Lowercase email before insert/update
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION lowercase_email()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email IS NOT NULL THEN
        NEW.email := LOWER(TRIM(NEW.email));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


----------------------------------------------------------------
-- TRIGGER: Auto-lowercase email on users table
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_users_lowercase_email'
    ) THEN
        CREATE TRIGGER trg_users_lowercase_email
            BEFORE INSERT OR UPDATE OF email ON users
            FOR EACH ROW
            EXECUTE FUNCTION lowercase_email();
    END IF;
END $$;


----------------------------------------------------------------
-- ADDRESSES TABLE
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables WHERE tablename = 'addresses'
    ) THEN
        CREATE TABLE addresses (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

            longitude DECIMAL(10,7) NULL,
            latitude  DECIMAL(10,7) NULL,

            country VARCHAR(125) NOT NULL,
            postal_code VARCHAR(50) NOT NULL,
            province VARCHAR(125) NOT NULL,
            city VARCHAR(125) NOT NULL,
            district VARCHAR(125) NOT NULL,
            subdistrict VARCHAR(125) NOT NULL,

            street VARCHAR(255) NOT NULL,
            details VARCHAR(255),

            label VARCHAR(125) NOT NULL,

            created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;


----------------------------------------------------------------
-- Add FK for Default Address AFTER table exists
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_default_address'
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT fk_users_default_address
        FOREIGN KEY (default_address_id)
        REFERENCES addresses(id)
        ON DELETE SET NULL;
    END IF;
END $$;


----------------------------------------------------------------
-- TOKENS TABLE
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables WHERE tablename = 'tokens'
    ) THEN
        CREATE TABLE tokens (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

            refresh_token TEXT NOT NULL,
            expires_at TIMESTAMPTZ NOT NULL,

            created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;


----------------------------------------------------------------
-- SYSTEM_PARAMETERS TABLE
----------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables WHERE tablename = 'system_parameters'
    ) THEN
        CREATE TABLE system_parameters (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

            variable CITEXT NOT NULL UNIQUE,  -- Case-insensitive for lookups
            data VARCHAR(255),
            description TEXT NOT NULL,

            created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END $$;


----------------------------------------------------------------
-- INDEXES
----------------------------------------------------------------
-- (Index creation is safe; IF NOT EXISTS support added in PG 9.5+)

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_fullname ON users(fullname);  -- CITEXT index for name search
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);  -- For sorting by registration date

-- Addresses table indexes
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);  -- FK lookup (critical for joins)
CREATE INDEX IF NOT EXISTS idx_addresses_location ON addresses(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_addresses_city ON addresses(city);  -- Common filter
CREATE INDEX IF NOT EXISTS idx_addresses_postal_code ON addresses(postal_code);  -- Common filter

-- Tokens table indexes
CREATE INDEX IF NOT EXISTS idx_tokens_expiry ON tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_tokens_refresh ON tokens(refresh_token);  -- For token validation lookup

-- System parameters index
CREATE INDEX IF NOT EXISTS idx_system_params_variable ON system_parameters(variable);  -- Fast variable lookup
