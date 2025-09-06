package ToolBox;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseManager {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/Telegram";
    private static final String DB_USER = "Telegram_Admin0";
    private static final String DB_PASSWORD = "Telegram_Admin_@Pass3000";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL driver not found on classpath.");
        }
    }

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
        c.setAutoCommit(true);
        return c;
    }

    public static final class User {
        public final long id;
        public final String firstName;
        public final String lastName;
        public final String username;
        public final String country;
        public final String phone;
        public final String email;
        public final String bio;
        public final String avatarUrl;

        public User(long id, String firstName, String lastName, String username,
                    String country, String phone, String email, String bio, String avatarUrl) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.country = country;
            this.phone = phone;
            this.email = email;
            this.bio = bio;
            this.avatarUrl = avatarUrl;
        }
    }

    public static final class Message {
        public final long id;
        public final long conversationId;
        public final long senderId;
        public final String content;
        public final Timestamp createdAt;

        public Message(long id, long conversationId, long senderId, String content, Timestamp createdAt) {
            this.id = id;
            this.conversationId = conversationId;
            this.senderId = senderId;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    public static List<User> getAllUsers(String currentUserPhone) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, username, country, phone, email, bio, avatar_url FROM users WHERE phone != ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUserPhone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getLong("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("username"),
                        rs.getString("country"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("bio"),
                        rs.getString("avatar_url")
                ));
            }
        } catch (SQLException e) {
            logSql("getAllUsers", e);
        }
        return users;
    }


    public static boolean userExists(String phoneNumber) {
        String sql = "SELECT COUNT(*) FROM users WHERE phone = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean userExistsByUsername(String username) {
        final String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logSql("userExistsByUsername", e);
            return false;
        }
    }

    public static boolean createUser(String firstName, String lastName, String username, String country, String phone, String passwordHash) {
        String sql = "INSERT INTO users (first_name, last_name, username, country, phone, password_hash, id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, username);
            ps.setString(4, country);
            ps.setString(5, phone);
            ps.setString(6, passwordHash); // Storing the hashed password
            ps.setLong(7, Math.abs((long)phone.hashCode() + System.currentTimeMillis()));
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Optional<User> getUserByPhone(String phone) {
        final String sql = """
            SELECT id, first_name, last_name, username, country, phone, email, bio, avatar_url
            FROM users WHERE phone = ? LIMIT 1
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("username"),
                            rs.getString("country"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("bio"),
                            rs.getString("avatar_url")
                    ));
                }
            }
        } catch (SQLException e) {
            logSql("getUserByPhone", e);
        }
        return Optional.empty();
    }

    public static boolean verifyPassword(String phoneNumber, String plainPassword) {
        String sql = "SELECT password_hash FROM users WHERE phone = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phoneNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHashedPassword = rs.getString("password_hash");
                return PasswordUtils.checkPassword(plainPassword, storedHashedPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void updateLastSeen(long userId, Instant when) {
        final String sql = "UPDATE users SET last_seen = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(when));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logSql("updateLastSeen", e);
        }
    }

    public static boolean addContact(long userId, long contactId) {
        final String sql = """
            INSERT INTO contacts (id, contact_id, blocked) VALUES (?, ?, FALSE)
            ON DUPLICATE KEY UPDATE blocked = VALUES(blocked)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, contactId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("addContact", e);
            return false;
        }
    }

    public static boolean setContactBlocked(long userId, long contactId, boolean blocked) {
        final String sql = "UPDATE contacts SET blocked = ? WHERE id = ? AND contact_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, blocked);
            ps.setLong(2, userId);
            ps.setLong(3, contactId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("setContactBlocked", e);
            return false;
        }
    }

    public static Optional<Long> createConversation(String type, String title, long creatorId, String pictureUrl) {
        final String sql = """
            INSERT INTO conversations (type, title, creator_id, picture_url)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type);
            ps.setString(2, nullIfEmpty(title));
            ps.setLong(3, creatorId);
            ps.setString(4, nullIfEmpty(pictureUrl));
            if (ps.executeUpdate() == 0) return Optional.empty();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return Optional.of(keys.getLong(1));
            }
        } catch (SQLException e) {
            logSql("createConversation", e);
        }
        return Optional.empty();
    }

    public static boolean addParticipant(long conversationId, long userId, String role) {
        final String sql = """
            INSERT INTO conversation_participants (id, user_id, role, muted)
            VALUES (?, ?, ?, FALSE)
            ON DUPLICATE KEY UPDATE role = VALUES(role)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setLong(2, userId);
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("addParticipant", e);
            return false;
        }
    }

    public static Optional<Long> saveMessage(long conversationId, long senderId,
                                             String content, Long mediaId,
                                             Long forwardId, Long replyToId) {
        final String sql = """
            INSERT INTO messages (conversation_id, sender_id, forward_id, content, media_id, reply_to_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, conversationId);
            ps.setLong(2, senderId);
            if (forwardId == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, forwardId);
            ps.setString(4, nullIfEmpty(content));
            if (mediaId == null) ps.setNull(5, Types.BIGINT); else ps.setLong(5, mediaId);
            if (replyToId == null) ps.setNull(6, Types.BIGINT); else ps.setLong(6, replyToId);
            if (ps.executeUpdate() == 0) return Optional.empty();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return Optional.of(keys.getLong(1));
            }
        } catch (SQLException e) {
            logSql("saveMessage", e);
        }
        return Optional.empty();
    }

    public static boolean setMessageStatus(long messageId, long userId, String status) {
        final String sql = """
            INSERT INTO message_status (id, user_id, status)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE status = VALUES(status), timestamp = CURRENT_TIMESTAMP
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, userId);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("setMessageStatus", e);
            return false;
        }
    }

    public static List<Message> listMessages(long conversationId, int limit, Long beforeMessageId) {
        final String sqlNewest = """
            SELECT id, conversation_id, sender_id, content, created_at
            FROM messages
            WHERE conversation_id = ?
            ORDER BY id DESC
            LIMIT ?
            """;
        final String sqlBefore = """
            SELECT id, conversation_id, sender_id, content, created_at
            FROM messages
            WHERE conversation_id = ? AND id < ?
            ORDER BY id DESC
            LIMIT ?
            """;
        List<Message> out = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(beforeMessageId == null ? sqlNewest : sqlBefore)) {
            ps.setLong(1, conversationId);
            if (beforeMessageId == null) {
                ps.setInt(2, Math.max(1, Math.min(100, limit)));
            } else {
                ps.setLong(2, beforeMessageId);
                ps.setInt(3, Math.max(1, Math.min(100, limit)));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Message(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getLong("sender_id"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            logSql("listMessages", e);
        }
        return out;
    }

    public static Optional<Long> startSession(long userId, String deviceInfo, String ipAddress) {
        final String sql = """
            INSERT INTO sessions (user_id, device_info, ip_address, is_active)
            VALUES (?, ?, ?, TRUE)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, nullIfEmpty(deviceInfo));
            ps.setString(3, nullIfEmpty(ipAddress));
            if (ps.executeUpdate() == 0) return Optional.empty();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return Optional.of(keys.getLong(1));
            }
        } catch (SQLException e) {
            logSql("startSession", e);
        }
        return Optional.empty();
    }

    public static void touchSession(long sessionId) {
        final String sql = "UPDATE sessions SET last_active = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logSql("touchSession", e);
        }
    }

    public static void endSession(long sessionId) {
        final String sql = "UPDATE sessions SET is_active = FALSE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logSql("endSession", e);
        }
    }

    private static String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static void logSql(String op, Exception e) {
        System.err.println("[DB] " + op + " failed: " + e.getMessage());
    }
}
