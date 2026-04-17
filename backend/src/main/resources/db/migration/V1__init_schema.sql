CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(11) NOT NULL UNIQUE,
    nickname VARCHAR(20) NOT NULL DEFAULT '',
    avatar VARCHAR(255) NOT NULL DEFAULT '',
    credit_score INT NOT NULL DEFAULT 100,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone ON t_user(phone);

CREATE TABLE t_game (
    id INT PRIMARY KEY,
    game_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'enabled',
    sort_no INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status_sort ON t_game(status, sort_no);

CREATE TABLE t_sms_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(11) NOT NULL,
    sms_code VARCHAR(6) NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    used_status TINYINT NOT NULL DEFAULT 0,
    used_time TIMESTAMP NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone_status ON t_sms_code(phone, used_status);
CREATE INDEX idx_expired_at ON t_sms_code(expired_at);

CREATE TABLE t_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    owner_id BIGINT NOT NULL,
    max_player INT NOT NULL,
    current_player INT NOT NULL DEFAULT 0,
    type VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_id ON t_room(game_id);
CREATE INDEX idx_owner_id ON t_room(owner_id);
CREATE INDEX idx_status ON t_room(status);
CREATE INDEX idx_create_time ON t_room(create_time);

CREATE TABLE t_room_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    join_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_room_user UNIQUE (room_id, user_id)
);

CREATE INDEX idx_room_id ON t_room_user(room_id);
CREATE INDEX idx_user_id ON t_room_user(user_id);

CREATE TABLE t_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_room_id ON t_message(room_id);
CREATE INDEX idx_message_room_id_id ON t_message(room_id, id);
