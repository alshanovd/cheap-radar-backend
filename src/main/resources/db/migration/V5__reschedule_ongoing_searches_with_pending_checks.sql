UPDATE searches
SET status = 'SCHEDULED'
WHERE status = 'ONGOING'
  AND next_check_at IS NOT NULL
  AND (check_count IS NULL
    OR completed_check_count IS NULL
    OR completed_check_count < check_count);
