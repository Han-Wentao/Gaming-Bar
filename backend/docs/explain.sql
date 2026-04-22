EXPLAIN
SELECT *
FROM t_room
WHERE status IN ('waiting', 'ready')
  AND game_id = 1
ORDER BY create_time DESC, id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT r.*
FROM t_room r
JOIN t_room_user ru ON ru.room_id = r.id
WHERE ru.user_id = 10001
  AND r.status IN ('waiting', 'ready')
ORDER BY r.create_time DESC, r.id DESC
LIMIT 20 OFFSET 0;

EXPLAIN
SELECT *
FROM t_message
WHERE room_id = 10001
  AND id < 5000
ORDER BY id DESC
LIMIT 50;

EXPLAIN
SELECT *
FROM t_room
WHERE status IN ('waiting', 'ready')
ORDER BY current_player DESC, create_time DESC, id DESC
LIMIT 5;
