DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'searches'
          AND column_name = 'status'
    ) THEN
        ALTER TABLE searches
            ALTER COLUMN status TYPE varchar(255)
            USING CASE status::text
                WHEN '0' THEN 'COMPLETED'
                WHEN '1' THEN 'PROCESSING'
                WHEN '2' THEN 'PARTIAL'
                WHEN '3' THEN 'FAILED'
                ELSE status::text
            END;
    END IF;
END $$;
