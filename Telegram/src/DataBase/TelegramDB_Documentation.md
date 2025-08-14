# Database Documentation for Telegram-like Desktop Program

This document describes the schema and relationships of a messaging application modeled after Telegram. It explains each table’s purpose, columns, constraints, and how they interrelate.

---

## Tables Overview

- [users](#users)
- [contacts](#contacts)
- [conversations](#conversations)
- [conversation_participants](#conversation_participants)
- [media](#media)
- [forward](#forward)
- [messages](#messages)
- [message_status](#message_status)
- [sessions](#sessions)

---

## users

Holds user profiles, authentication details, preferences, and timestamps for account activity.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique identifier for each user.  
- `first_name` (TEXT): User’s given name.  
- `last_name` (TEXT): User’s family name.  
- `username` (VARCHAR(30), UNIQUE): Unique handle used for mentions and search.  
- `country` (VARCHAR(30)): The name of the user's country.  
- `phone` (VARCHAR(20), UNIQUE, NOT NULL): Verified phone number required for account creation.  
- `email` (TEXT): Optional email address.  
- `password_hash` (TEXT, NOT NULL): Secure hash of the user’s password.  
- `bio` (TEXT): Short biography or status message.  
- `avatar_url` (TEXT): Link to the user’s profile image.  
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Timestamp when the account was created.  
- `last_seen` (TIMESTAMP): Last activity timestamp.  
- `theme` (TEXT): User’s chosen interface theme.  
- `notifications_enabled` (BOOLEAN, DEFAULT TRUE): Whether notifications are turned on.  
- `language_` (TEXT): Preferred user interface language.  

### **Constraints:**

- Primary key on `id`  
- Unique constraints on `username` and `phone`  

---

## contacts

Manages user-to-user relationships, including blocked contacts.

### **Columns and Descriptions:**

- `id` (BIGINT): The user who adds another to contacts.  
- `contact_id` (BIGINT): The user being added.  
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): When the contact was added.  
- `blocked` (BOOLEAN, DEFAULT FALSE): Whether the contact is blocked.  

### **Constraints:**

- Composite primary key on (`id`, `contact_id`)  
- Foreign keys: `id` and `contact_id` reference `users(id)`  

---

## conversations

Stores metadata for all chats: private, group, or channel.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique identifier for each conversation.  
- `type` (ENUM('private', 'group', 'channel'), NOT NULL): Type of the conversation.  
- `title` (TEXT): Display name (for groups or channels).  
- `creator_id` (BIGINT): User who created the conversation.  
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Creation timestamp.  
- `picture_url` (TEXT): Icon/image of the conversation.  

### **Constraints:**

- Foreign key on `creator_id` references `users(id)`  

---

## conversation_participants

Tracks which users are part of which conversations, their roles, and notification preferences.

### **Columns and Descriptions:**

- `id` (BIGINT): Conversation ID.  
- `user_id` (BIGINT): User participating in the conversation.  
- `joined_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): When the user joined.  
- `role` (ENUM('owner', 'admin', 'member'), DEFAULT 'member'): Role of participant.  
- `muted` (BOOLEAN, DEFAULT FALSE): If user muted the conversation.  

### **Constraints:**

- Composite primary key on (`id`, `user_id`)  
- Foreign keys:  
  - `id` → `conversations(id)`  
  - `user_id` → `users(id)`  

---

## media

Stores uploaded files like images, videos, and documents.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique identifier for each media.  
- `uploader_id` (BIGINT): User who uploaded the file.  
- `file_name` (TEXT): Original name of the file.  
- `file_path` (TEXT): File location (URL or server path).  
- `mime_type` (TEXT): MIME type (e.g., `image/jpeg`).  
- `size_bytes` (BIGINT): Size of the file in bytes.  
- `uploaded_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Upload timestamp.  

### **Constraints:**

- Foreign key on `uploader_id` references `users(id)`  

---

## forward

Captures metadata of forwarded messages.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique forward record ID.  
- `author` (BIGINT): Original author of the forwarded message.  
- `from_channel` (BOOLEAN, DEFAULT FALSE): Whether the message is from a channel.  
- `channel_id` (BIGINT): Source channel ID if forwarded from a channel.  

### **Constraints:**

- Foreign key on `channel_id` references `conversations(id)`  

---

## messages

Stores all messages including content, media, replies, and status.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique message ID.  
- `conversation_id` (BIGINT): Related conversation.  
- `sender_id` (BIGINT): User who sent the message.  
- `forward_id` (BIGINT): Reference to forwarded message metadata.  
- `content` (TEXT): Text content.  
- `media_id` (BIGINT): Attached media (if any).  
- `reply_to_id` (BIGINT): Message being replied to (if any).  
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Time of sending.  
- `edited_at` (TIMESTAMP): Last edit timestamp.  
- `is_deleted` (BOOLEAN, DEFAULT FALSE): Soft deletion flag.  
- `view_count` (BIGINT): View count (mainly for channels).  

### **Constraints:**

- Foreign keys:  
  - `conversation_id` → `conversations(id)`  
  - `sender_id` → `users(id)`  
  - `media_id` → `media(id)`  
  - `reply_to_id` → `messages(id)`  
  - `forward_id` → `forward(id)`  

---

## message_status

Tracks delivery and read status for each message-recipient pair.

### **Columns and Descriptions:**

- `id` (BIGINT): Message ID.  
- `user_id` (BIGINT): Recipient user ID.  
- `status` (ENUM('delivered', 'seen', 'failed'), DEFAULT 'delivered'): Delivery status.  
- `timestamp` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Status update time.  

### **Constraints:**

- Composite primary key on (`id`, `user_id`)  
- Foreign keys:  
  - `id` → `messages(id)`  
  - `user_id` → `users(id)`  

---

## sessions

Manages user sessions across devices.

### **Columns and Descriptions:**

- `id` (BIGINT, PRIMARY KEY): Unique session ID.  
- `user_id` (BIGINT): User who owns the session.  
- `device_info` (TEXT): Device/browser details.  
- `ip_address` (TEXT): IP address of client.  
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Session creation time.  
- `last_active` (TIMESTAMP): Last activity time.  
- `is_active` (BOOLEAN, DEFAULT TRUE): Whether the session is active.  

### **Constraints:**

- Foreign key on `user_id` references `users(id)`  
