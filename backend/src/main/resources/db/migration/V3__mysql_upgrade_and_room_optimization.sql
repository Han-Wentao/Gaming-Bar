ALTER TABLE t_room
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX uk_room_user_one_room ON t_room_user(user_id);
CREATE INDEX idx_room_game_status ON t_room(game_id, status, create_time);
CREATE INDEX idx_message_room_create_time ON t_message(room_id, create_time, id);
