-- ============================================================
-- Distributed Inventory Logistics Engine — Database Bootstrap
-- ============================================================

-- 1. Inventory Items table
CREATE TABLE IF NOT EXISTS inventory_items (
    id            BIGSERIAL    PRIMARY KEY,
    sku           VARCHAR(50)  NOT NULL UNIQUE,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    warehouse_id  VARCHAR(20)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 2. Inventory Logs table (audit trail for every stock movement)
CREATE TABLE IF NOT EXISTS inventory_logs (
    id                BIGSERIAL    PRIMARY KEY,
    warehouse_id      VARCHAR(20)  NOT NULL,
    sku               VARCHAR(50)  NOT NULL,
    action            VARCHAR(20)  NOT NULL,
    quantity_change   INTEGER      NOT NULL,
    resulting_quantity INTEGER,
    timestamp         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    details           TEXT
);

-- ============================================================
-- COMPOSITE INDEX
-- Accelerates the most common analytical query pattern:
--   "Show me all activity in warehouse X between time A and B"
--
-- PostgreSQL uses a B-tree on (warehouse_id, timestamp), which
-- lets the planner do an index-only scan when both columns
-- appear in the WHERE clause.
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_inventory_logs_warehouse_timestamp
    ON inventory_logs (warehouse_id, timestamp);

-- Additional supporting index for SKU-based log lookups
CREATE INDEX IF NOT EXISTS idx_inventory_logs_sku
    ON inventory_logs (sku);

-- ============================================================
-- SEED DATA
-- ============================================================
INSERT INTO inventory_items (sku, product_name, quantity, warehouse_id)
VALUES
    ('SKU-001', 'Industrial Servo Motor',       500,  'WH-EAST'),
    ('SKU-002', 'Hydraulic Pressure Valve',     320,  'WH-EAST'),
    ('SKU-003', 'Carbon Fiber Panel 4x8',       150,  'WH-WEST'),
    ('SKU-004', 'Titanium Fastener Kit',        1200, 'WH-WEST'),
    ('SKU-005', 'Precision Ball Bearing 6205',  800,  'WH-NORTH'),
    ('SKU-006', 'Copper Busbar 200A',           250,  'WH-NORTH'),
    ('SKU-007', 'Thermal Interface Compound',   600,  'WH-SOUTH'),
    ('SKU-008', 'Stainless Flex Coupling',      420,  'WH-SOUTH')
ON CONFLICT (sku) DO NOTHING;
