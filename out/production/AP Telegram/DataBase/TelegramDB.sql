-- Made by @yoaminoo
-- This is a database for a Telegram-like desktop program made by MySQL
-- For more information please read the documentation

-- **WARNING!** : DO NOT RUN THE BELOW LINE! EXCEPT IF YOU WANT TO DELETE THE ENTIRE DATABASE
-- ~@#DROP DATABASE telegram~@#;$$

-- CREATE DATABASE telegram;
-- USE telegram;


CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    first_name TEXT,
    last_name TEXT,
    username VARCHAR(30) UNIQUE,
    country VARCHAR(30),
    phone VARCHAR(20) UNIQUE NOT NULL,
    email TEXT,
    password_hash TEXT NOT NULL,
    bio TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP,
	theme TEXT,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    language_ TEXT
);

CREATE TABLE contacts (
    id BIGINT,
    contact_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    blocked BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (id, contact_id),
    FOREIGN KEY (id) REFERENCES users(id),
    FOREIGN KEY (contact_id) REFERENCES users(id)
);

CREATE TABLE conversations (
    id BIGINT PRIMARY KEY,
    type ENUM('private', 'group', 'channel') NOT NULL,
    title TEXT,
    creator_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    picture_url TEXT,
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE conversation_participants (
    id BIGINT,
    user_id BIGINT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role ENUM('owner', 'admin', 'member') DEFAULT 'member',
    muted BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (id, user_id),
    FOREIGN KEY (id) REFERENCES conversations(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE media (
    id BIGINT PRIMARY KEY,
    uploader_id BIGINT,
    file_name TEXT,
    file_path TEXT,
    mime_type TEXT,
    size_bytes BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE TABLE forward (
	id BIGINT PRIMARY KEY,
    auther BIGINT,
    from_channel BOOLEAN DEFAULT FALSE,
    channel_id BIGINT,
    FOREIGN KEY (channel_id) REFERENCES conversations(id)
);

CREATE TABLE messages (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT,
    sender_id BIGINT,
    forward_id BIGINT,
    content TEXT,
    media_id BIGINT,
    reply_to_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    view_count BIGINT,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (media_id) REFERENCES media(id),
    FOREIGN KEY (reply_to_id) REFERENCES messages(id),
    FOREIGN KEY (forward_id) REFERENCES forward(id)
);

CREATE TABLE message_status (
    id BIGINT,
    user_id BIGINT,
    status ENUM('delivered', 'seen', 'failed') DEFAULT 'delivered',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, user_id),
    FOREIGN KEY (id) REFERENCES messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE sessions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    device_info TEXT,
    ip_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);