-- ─────────────────────────── Smart Cost Pro — PostgreSQL schema ───────────────────────────
-- Run as: psql -U postgres -f schema.sql
-- Or: psql $DATABASE_URL -f schema.sql
--
-- All tables are scoped by user_id (UUID).

-- ─────────────── Extensions ───────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- for gen_random_uuid()

-- ─────────────── Users ───────────────
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    google_id       VARCHAR(255) UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    biometric_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- ─────────────── OTPs ───────────────
CREATE TABLE IF NOT EXISTS password_reset_otps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed        BOOLEAN NOT NULL DEFAULT FALSE,
    attempts        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_otps_email ON password_reset_otps(email);
CREATE INDEX IF NOT EXISTS idx_otps_expires ON password_reset_otps(expires_at);

-- ─────────────── Expenses (unified) ───────────────
CREATE TABLE IF NOT EXISTS expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    category        VARCHAR(60) NOT NULL,
    expense_date    DATE NOT NULL,
    payment_method  VARCHAR(20) NOT NULL DEFAULT 'cash',
    notes           VARCHAR(2000),
    details_json    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_date ON expenses(user_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_user_type ON expenses(user_id, type);

-- ─────────────── Monthly income ───────────────
CREATE TABLE IF NOT EXISTS monthly_income (
    month           VARCHAR(7) NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    salary          DECIMAL(18,2) NOT NULL DEFAULT 0,
    bonuses         DECIMAL(18,2) NOT NULL DEFAULT 0,
    other_income    DECIMAL(18,2) NOT NULL DEFAULT 0,
    other_income_note VARCHAR(255),
    details_json    JSONB,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, month)
);

-- ─────────────── Bank certificates ───────────────
CREATE TABLE IF NOT EXISTS bank_certificates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bank_name       VARCHAR(100) NOT NULL,
    certificate_number VARCHAR(100),
    amount          DECIMAL(18,2) NOT NULL,
    annual_rate     DECIMAL(6,2) NOT NULL DEFAULT 0,
    duration        VARCHAR(50) NOT NULL DEFAULT '1 year',
    issue_date      DATE NOT NULL,
    maturity_date   DATE,
    profit_date     DATE,
    profit_amount   DECIMAL(18,2) DEFAULT 0,
    monthly_equivalent_profit DECIMAL(18,2) DEFAULT 0,
    profit_frequency VARCHAR(20) DEFAULT 'monthly',
    notes           VARCHAR(2000),
    details_json    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_certs_user ON bank_certificates(user_id);

-- ─────────────── Car trips ───────────────
CREATE TABLE IF NOT EXISTS car_income_trips (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trip_date       DATE NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    trip_type       VARCHAR(20) NOT NULL DEFAULT 'cash',
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trips_user_date ON car_income_trips(user_id, trip_date);

-- ─────────────── Current account ───────────────
CREATE TABLE IF NOT EXISTS current_account_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    description     VARCHAR(255) NOT NULL,
    amount          DECIMAL(18,2) NOT NULL,
    txn_date        DATE NOT NULL,
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_curr_user_date ON current_account_transactions(user_id, txn_date);

-- ─────────────── Students ───────────────
CREATE TABLE IF NOT EXISTS students (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    stage           VARCHAR(20) NOT NULL DEFAULT 'prep',
    age             INT NOT NULL,
    national_id     VARCHAR(14) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, national_id)
);

CREATE INDEX IF NOT EXISTS idx_students_user ON students(user_id);

-- ─────────────── Associations ───────────────
CREATE TABLE IF NOT EXISTS associations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    value           DECIMAL(18,2) NOT NULL,
    member_count    INT NOT NULL,
    installment     DECIMAL(18,2) NOT NULL,
    role_number     INT NOT NULL,
    start_date      DATE NOT NULL,
    payout_date     DATE NOT NULL,
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assoc_user ON associations(user_id);

-- ─────────────── Vehicle fuel ───────────────
CREATE TABLE IF NOT EXISTS vehicle_fuel (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fuel_type       VARCHAR(30) NOT NULL,
    quantity        DECIMAL(10,2) NOT NULL,
    unit_price      DECIMAL(10,2) NOT NULL,
    odometer        DECIMAL(10,2),
    fuel_date       DATE NOT NULL,
    fuel_time       VARCHAR(10),
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fuel_user_date ON vehicle_fuel(user_id, fuel_date);

-- ─────────────── Vehicle maintenance ───────────────
CREATE TABLE IF NOT EXISTS vehicle_maintenance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    maintenance_type VARCHAR(100),
    supply_name     VARCHAR(200),
    supply_price    DECIMAL(18,2) DEFAULT 0,
    labor_description VARCHAR(500),
    labor_price     DECIMAL(18,2) DEFAULT 0,
    maint_date      DATE NOT NULL,
    total           DECIMAL(18,2) DEFAULT 0,
    notes           VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_maint_user_date ON vehicle_maintenance(user_id, maint_date);

-- ─────────────── Vehicle oils ───────────────
CREATE TABLE IF NOT EXISTS vehicle_oils (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_type    VARCHAR(50) NOT NULL,
    brand           VARCHAR(50) DEFAULT 'Mobil',
    product_name    VARCHAR(200),
    quantity        DECIMAL(10,2) DEFAULT 1,
    price           DECIMAL(18,2) DEFAULT 0,
    odometer        DECIMAL(10,2),
    suggested_interval_km INT,
    notes           VARCHAR(500),
    oil_date        DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_oil_user_date ON vehicle_oils(user_id, oil_date);

-- ─────────────── Vehicle accidents ───────────────
CREATE TABLE IF NOT EXISTS vehicle_accidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id      VARCHAR(100),
    title           VARCHAR(200) NOT NULL,
    photo_url       VARCHAR(500),
    accident_date   DATE NOT NULL,
    accident_time   VARCHAR(10),
    location        VARCHAR(255),
    estimated_damage DECIMAL(18,2),
    notes           VARCHAR(1000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_accident_user_date ON vehicle_accidents(user_id, accident_date);

-- ─────────────── Budgets ───────────────
CREATE TABLE IF NOT EXISTS budgets (
    month           VARCHAR(7) NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total           DECIMAL(18,2) DEFAULT 0,
    house           DECIMAL(18,2) DEFAULT 0,
    medical         DECIMAL(18,2) DEFAULT 0,
    work            DECIMAL(18,2) DEFAULT 0,
    personal        DECIMAL(18,2) DEFAULT 0,
    fuel            DECIMAL(18,2) DEFAULT 0,
    maintenance     DECIMAL(18,2) DEFAULT 0,
    oils            DECIMAL(18,2) DEFAULT 0,
    education       DECIMAL(18,2) DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, month)
);

-- ─────────────── Section order ───────────────
CREATE TABLE IF NOT EXISTS section_orders (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    section_id      VARCHAR(50) NOT NULL,
    sort_order      INT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, section_id)
);

-- ─────────────── updated_at trigger ───────────────
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY[
        'users', 'password_reset_otps', 'expenses', 'monthly_income',
        'bank_certificates', 'car_income_trips', 'current_account_transactions',
        'students', 'associations', 'vehicle_fuel', 'vehicle_maintenance',
        'vehicle_oils', 'vehicle_accidents', 'budgets', 'section_orders'
    ])
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS set_timestamp ON %I', t);
        EXECUTE format('CREATE TRIGGER set_timestamp BEFORE UPDATE ON %I
                        FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();', t);
    END LOOP;
END $$;
