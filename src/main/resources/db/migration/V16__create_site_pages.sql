CREATE TABLE IF NOT EXISTS site_page_blocks (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    page_key VARCHAR(40) NOT NULL,
    block_type VARCHAR(40) NOT NULL,
    display_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    content JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_site_page_blocks_lookup
    ON site_page_blocks (store_id, page_key, published, display_order)
    WHERE is_deleted = FALSE;
