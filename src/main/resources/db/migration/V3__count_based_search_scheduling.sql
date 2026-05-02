ALTER TABLE searches
    ADD COLUMN IF NOT EXISTS check_count integer,
    ADD COLUMN IF NOT EXISTS completed_check_count integer;

ALTER TABLE searches
    DROP COLUMN IF EXISTS search_start_at,
    DROP COLUMN IF EXISTS check_finish_at;
