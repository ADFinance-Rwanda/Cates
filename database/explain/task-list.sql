-- Filtered task list used by GET /api/tasks?status=TODO
-- Run against the application database after it has enough rows to matter.
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, status, priority, created_at
FROM tasks
WHERE organization_id = 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6'
  AND status = 'TODO'
ORDER BY created_at DESC
LIMIT 20;
