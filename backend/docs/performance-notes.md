# Performance Notes

## Query focus

- `t_room(game_id, status, create_time)`: supports room lobby filtering and sorting.
- `t_room(status)`: keeps status filtering cheap for waiting/ready room scans.
- `t_message(room_id, create_time, id)` and `t_message(room_id, id)`: support room history reads and message-id cursor pagination.
- `t_room_user(user_id)` unique index: enforces one-person-one-room at the database layer.

## EXPLAIN workflow

Run the statements in [explain.sql](/D:/Gaming-Bar/backend/docs/explain.sql) against MySQL after `docker-compose up`.

Expected checks:

- room list should prefer `idx_room_game_status` or `idx_status`
- room member lookup should use `uk_room_user_one_room` / `idx_room_id`
- message history should use `idx_message_room_id_id` or `idx_message_room_create_time`
- hot room query should avoid full table scans once room volume grows

## Concurrency validation

- automated test: `RoomConcurrencyTest`
- scenario: 100 concurrent users join the same 10-person room
- expected result: exactly 9 joins succeed after the owner, room occupancy remains 10, no overfill
