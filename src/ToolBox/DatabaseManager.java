package ToolBox;

import java.sql.*;
import java.text.SimpleDateFormat;
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
        public final long senderId;
        public final String content;
        public final Timestamp createdAt;
        public final byte[] mediaData;
        public final String fileName;
        public final String mimeType;

        public Message(long id, long senderId, String content, Timestamp createdAt, byte[] mediaData, String fileName, String mimeType) {
            this.id = id;
            this.senderId = senderId;
            this.content = content;
            this.createdAt = createdAt;
            this.mediaData = mediaData;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }
    }

    public static boolean deleteMessage(long messageId) {
        // First, get the media_id before deleting the message
        String selectMediaSql = "SELECT media_id FROM messages WHERE id = ?";
        String deleteMessageSql = "DELETE FROM messages WHERE id = ?";
        String deleteMediaSql = "DELETE FROM media WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            Long mediaId = null;
            try (PreparedStatement psSelect = conn.prepareStatement(selectMediaSql)) {
                psSelect.setLong(1, messageId);
                ResultSet rs = psSelect.executeQuery();
                if (rs.next()) {
                    mediaId = rs.getLong("media_id");
                    if (rs.wasNull()) {
                        mediaId = null;
                    }
                }
            }

            // Delete the message
            try (PreparedStatement psDeleteMsg = conn.prepareStatement(deleteMessageSql)) {
                psDeleteMsg.setLong(1, messageId);
                int affectedRows = psDeleteMsg.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false; // Message not found
                }
            }

            // If there was associated media, delete it
            if (mediaId != null) {
                try (PreparedStatement psDeleteMedia = conn.prepareStatement(deleteMediaSql)) {
                    psDeleteMedia.setLong(1, mediaId);
                    psDeleteMedia.executeUpdate();
                }
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            logSql("deleteMessage", e);
            try (Connection conn = getConnection()) {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logSql("deleteMessageRollback", ex);
            }
            return false;
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

    public static Optional<Long> getUserId(String phone) {
        String sql = "SELECT id FROM users WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getLong("id"));
            }
        } catch (SQLException e) {
            logSql("getUserId", e);
        }
        return Optional.empty();
    }
    public static long findOrCreatePrivateConversation(long user1Id, long user2Id) {
        long lowerId = Math.min(user1Id, user2Id);
        long higherId = Math.max(user1Id, user2Id);

        String findSql = "SELECT cp1.id FROM conversation_participants cp1 " +
                "JOIN conversation_participants cp2 ON cp1.id = cp2.id " +
                "WHERE cp1.user_id = ? AND cp2.user_id = ?";

        try (Connection conn = getConnection(); PreparedStatement psFind = conn.prepareStatement(findSql)) {
            psFind.setLong(1, lowerId);
            psFind.setLong(2, higherId);
            ResultSet rs = psFind.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logSql("findConversation", e);
        }

        String createConvSql = "INSERT INTO conversations (id, type, creator_id) VALUES (?, 'private', ?)";
        String addPartSql = "INSERT INTO conversation_participants (id, user_id) VALUES (?, ?)";
        long newConversationId = Math.abs(System.currentTimeMillis() + lowerId + higherId);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCreate = conn.prepareStatement(createConvSql);
                 PreparedStatement psAdd1 = conn.prepareStatement(addPartSql);
                 PreparedStatement psAdd2 = conn.prepareStatement(addPartSql)) {

                psCreate.setLong(1, newConversationId);
                psCreate.setLong(2, lowerId);
                psCreate.executeUpdate();

                psAdd1.setLong(1, newConversationId);
                psAdd1.setLong(2, lowerId);
                psAdd1.executeUpdate();

                psAdd2.setLong(1, newConversationId);
                psAdd2.setLong(2, higherId);
                psAdd2.executeUpdate();

                conn.commit();
                return newConversationId;
            } catch (SQLException e) {
                conn.rollback();
                logSql("createConversation", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logSql("transactionSetup", e);
        }
        return -1;
    }

    public static Optional<Long> saveMediaAndGetId(long uploaderId, byte[] mediaData, String fileName, String mimeType) {
        String sql = "INSERT INTO media (id, uploader_id, file_name, mime_type, media_data, size_bytes) VALUES (?, ?, ?, ?, ?, ?)";
        long mediaId = System.currentTimeMillis();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, mediaId);
            ps.setLong(2, uploaderId);
            ps.setString(3, fileName);
            ps.setString(4, mimeType);
            ps.setBytes(5, mediaData);
            ps.setLong(6, mediaData.length);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                return Optional.of(mediaId);
            }
        } catch (SQLException e) {
            logSql("saveMediaAndGetId", e);
        }
        return Optional.empty();
    }


    public static boolean saveMessage(String senderPhone, String receiverPhone, String content, Long mediaId) {
        Optional<Long> senderIdOpt = getUserId(senderPhone);
        Optional<Long> receiverIdOpt = getUserId(receiverPhone);

        if (senderIdOpt.isEmpty() || receiverIdOpt.isEmpty()) return false;

        long senderId = senderIdOpt.get();
        long receiverId = receiverIdOpt.get();
        long conversationId = findOrCreatePrivateConversation(senderId, receiverId);
        if (conversationId == -1) return false;

        String sql = "INSERT INTO messages (id, conversation_id, sender_id, content, media_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, conversationId);
            ps.setLong(3, senderId);
            ps.setString(4, content);

            if (mediaId != null) {
                ps.setLong(5, mediaId);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("saveMessage", e);
            return false;
        }
    }

    public static List<Message> loadMessages(String user1Phone, String user2Phone) {
        List<Message> messages = new ArrayList<>();
        Optional<Long> user1IdOpt = getUserId(user1Phone);
        Optional<Long> user2IdOpt = getUserId(user2Phone);

        if (user1IdOpt.isEmpty() || user2IdOpt.isEmpty()) return messages;

        long conversationId = findOrCreatePrivateConversation(user1IdOpt.get(), user2IdOpt.get());
        if (conversationId == -1) return messages;

        String sql = "SELECT m.id, m.sender_id, m.content, m.created_at, " +
                "med.media_data, med.file_name, med.mime_type " +
                "FROM messages m LEFT JOIN media med ON m.media_id = med.id " +
                "WHERE m.conversation_id = ? ORDER BY m.created_at ASC";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getLong("id"),
                        rs.getLong("sender_id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getBytes("media_data"),
                        rs.getString("file_name"),
                        rs.getString("mime_type")
                ));
            }
        } catch (SQLException e) {
            logSql("loadMessages", e);
        }
        return messages;
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

    public static boolean createUser(String firstName, String lastName, String username, String country, String phone, String passwordHash) {
        String sql = "INSERT INTO users (first_name, last_name, username, country, phone, password_hash, id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, username);
            ps.setString(4, country);
            ps.setString(5, phone);
            ps.setString(6, passwordHash);
            ps.setLong(7, Math.abs((long)phone.hashCode() + System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Optional<User> getUserByPhone(String phone) {
        final String sql = "SELECT id, first_name, last_name, username, country, phone, email, bio, avatar_url FROM users WHERE phone = ? LIMIT 1";
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

    private static void logSql(String op, Exception e) {
        System.err.println("[DB] " + op + " failed: " + e.getMessage());
    }
}

