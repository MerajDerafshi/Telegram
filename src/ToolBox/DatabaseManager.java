package ToolBox;

import java.sql.*;
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

    // --- Inner Classes for Data Models ---
    public static final class User {
        public final long id;
        public final String firstName;
        public final String lastName;
        public final String username;
        public final String country;
        public final String phone;
        public final String email;
        public final String bio;
        public final byte[] avatar;

        public User(long id, String firstName, String lastName, String username,
                    String country, String phone, String email, String bio, byte[] avatar) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.country = country;
            this.phone = phone;
            this.email = email;
            this.bio = bio;
            this.avatar = avatar;
        }
    }

    public static final class Channel {
        public final long id;
        public final String title;
        public final long creatorId;
        public final byte[] avatar;

        public Channel(long id, String title, long creatorId, byte[] avatar) {
            this.id = id;
            this.title = title;
            this.creatorId = creatorId;
            this.avatar = avatar;
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

    public static List<String> getChannelMemberPhones(long channelId) {
        List<String> memberPhones = new ArrayList<>();
        String sql = "SELECT u.phone FROM users u " +
                "JOIN conversation_participants cp ON u.id = cp.user_id " +
                "WHERE cp.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                memberPhones.add(rs.getString("phone"));
            }
        } catch (SQLException e) {
            logSql("getChannelMemberPhones", e);
        }
        return memberPhones;
    }

    // --- START: NEW METHOD ---
    public static List<Message> loadChannelMessages(long channelId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, m.content, m.created_at, " +
                "med.media_data, med.file_name, med.mime_type " +
                "FROM messages m LEFT JOIN media med ON m.media_id = med.id " +
                "WHERE m.conversation_id = ? ORDER BY m.created_at ASC";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
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
            logSql("loadChannelMessages", e);
        }
        return messages;
    }
    // --- END: NEW METHOD ---


    public static boolean updateUserProfile(String oldPhone, String newFirstName, String newPhone, String newUsername, String newBio) {
        String sql = "UPDATE users SET first_name = ?, phone = ?, username = ?, bio = ? WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newFirstName);
            ps.setString(2, newPhone);
            ps.setString(3, newUsername);
            ps.setString(4, newBio);
            ps.setString(5, oldPhone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("updateUserProfile", e);
            return false;
        }
    }

    public static boolean updatePassword(String phone, String newHashedPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("updatePassword", e);
            return false;
        }
    }

    public static boolean updateAvatar(String phone, byte[] avatarData) {
        String sql = "UPDATE users SET avatar = ? WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBytes(1, avatarData);
            ps.setString(2, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("updateAvatar", e);
            return false;
        }
    }

    public static byte[] getAvatar(String phone) {
        String sql = "SELECT avatar FROM users WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBytes("avatar");
            }
        } catch (SQLException e) {
            logSql("getAvatar", e);
        }
        return null;
    }


    public static boolean deleteMessage(long messageId) {
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

            try (PreparedStatement psDeleteMsg = conn.prepareStatement(deleteMessageSql)) {
                psDeleteMsg.setLong(1, messageId);
                int affectedRows = psDeleteMsg.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }
            }

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
            try (Connection conn2 = getConnection()) {
                if (conn2 != null) conn2.rollback();
            } catch (SQLException ex) {
                logSql("deleteMessageRollback", ex);
            }
            return false;
        }
    }

    public static boolean deleteChatHistory(String localUserPhone, String otherUserPhone) {
        Optional<Long> user1IdOpt = getUserId(localUserPhone);
        Optional<Long> user2IdOpt = getUserId(otherUserPhone);

        if (user1IdOpt.isEmpty() || user2IdOpt.isEmpty()) return false;

        long conversationId = findOrCreatePrivateConversation(user1IdOpt.get(), user2IdOpt.get());
        if (conversationId == -1) return true; // No conversation to delete

        String selectMediaSql = "SELECT media_id FROM messages WHERE conversation_id = ? AND media_id IS NOT NULL";
        List<Long> mediaIds = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement psSelect = conn.prepareStatement(selectMediaSql)) {
            psSelect.setLong(1, conversationId);
            ResultSet rs = psSelect.executeQuery();
            while (rs.next()) {
                mediaIds.add(rs.getLong("media_id"));
            }
        } catch (SQLException e) {
            logSql("deleteChatHistory-selectMedia", e);
            return false;
        }

        String deleteMessagesSql = "DELETE FROM messages WHERE conversation_id = ?";
        String deleteMediaSql = "DELETE FROM media WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psDeleteMessages = conn.prepareStatement(deleteMessagesSql)) {
                psDeleteMessages.setLong(1, conversationId);
                psDeleteMessages.executeUpdate();
            }

            if (!mediaIds.isEmpty()) {
                try (PreparedStatement psDeleteMedia = conn.prepareStatement(deleteMediaSql)) {
                    for (Long mediaId : mediaIds) {
                        psDeleteMedia.setLong(1, mediaId);
                        psDeleteMedia.addBatch();
                    }
                    psDeleteMedia.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            logSql("deleteChatHistory-transaction", e);
            try(Connection conn2 = getConnection()){
                if(conn2 != null) conn2.rollback();
            } catch (SQLException ex) {
                logSql("deleteChatHistory-rollback", ex);
            }
            return false;
        }
    }

    public static List<User> getAllUsers(String currentUserPhone) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, username, country, phone, email, bio, avatar FROM users WHERE phone != ?";
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
                        rs.getBytes("avatar")
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

    private static long findOrCreateSelfConversation(long userId) {
        String findSql = "SELECT c.id FROM conversations c " +
                "JOIN conversation_participants cp ON c.id = cp.id " +
                "WHERE c.type = 'private' AND c.creator_id = ? " +
                "GROUP BY c.id HAVING COUNT(cp.user_id) = 1";

        try (Connection conn = getConnection(); PreparedStatement psFind = conn.prepareStatement(findSql)) {
            psFind.setLong(1, userId);
            ResultSet rs = psFind.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            logSql("findSelfConversation", e);
        }

        String createConvSql = "INSERT INTO conversations (id, type, creator_id) VALUES (?, 'private', ?)";
        String addPartSql = "INSERT INTO conversation_participants (id, user_id) VALUES (?, ?)";
        long newConversationId = Math.abs(System.nanoTime() + userId);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCreate = conn.prepareStatement(createConvSql);
                 PreparedStatement psAdd = conn.prepareStatement(addPartSql)) {

                psCreate.setLong(1, newConversationId);
                psCreate.setLong(2, userId);
                psCreate.executeUpdate();

                psAdd.setLong(1, newConversationId);
                psAdd.setLong(2, userId);
                psAdd.executeUpdate();

                conn.commit();
                return newConversationId;
            } catch (SQLException e) {
                conn.rollback();
                logSql("createSelfConversation", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logSql("transactionSetupSelf", e);
        }
        return -1;
    }

    public static long findOrCreatePrivateConversation(long user1Id, long user2Id) {
        if (user1Id == user2Id) {
            return findOrCreateSelfConversation(user1Id);
        }

        long lowerId = Math.min(user1Id, user2Id);
        long higherId = Math.max(user1Id, user2Id);

        String findSql = "SELECT cp.id FROM conversation_participants cp " +
                "WHERE cp.id IN (SELECT id FROM conversation_participants WHERE user_id = ?) " +
                "AND cp.user_id = ? " +
                "AND (SELECT COUNT(*) FROM conversation_participants WHERE id = cp.id) = 2";


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
        long newConversationId = Math.abs(System.nanoTime() + lowerId + higherId);

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
        long mediaId = System.nanoTime();
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
            ps.setLong(1, System.nanoTime());
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
        final String sql = "SELECT id, first_name, last_name, username, country, phone, email, bio, avatar FROM users WHERE phone = ? LIMIT 1";
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
                            rs.getBytes("avatar")
                    ));
                }
            }
        } catch (SQLException e) {
            logSql("getUserByPhone", e);
        }
        return Optional.empty();
    }

    public static long createChannel(String title, byte[] avatar, long creatorId, List<Long> memberIds) {
        String createConvSql = "INSERT INTO conversations (id, type, title, creator_id, picture) VALUES (?, 'channel', ?, ?, ?)";
        String addPartSql = "INSERT INTO conversation_participants (id, user_id, role) VALUES (?, ?, ?)";
        long newConversationId = Math.abs(System.nanoTime() + creatorId);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCreate = conn.prepareStatement(createConvSql);
                 PreparedStatement psAdd = conn.prepareStatement(addPartSql)) {

                psCreate.setLong(1, newConversationId);
                psCreate.setString(2, title);
                psCreate.setLong(3, creatorId);
                if (avatar != null) {
                    psCreate.setBytes(4, avatar);
                } else {
                    psCreate.setNull(4, Types.BLOB);
                }
                psCreate.executeUpdate();

                psAdd.setLong(1, newConversationId);
                psAdd.setLong(2, creatorId);
                psAdd.setString(3, "owner");
                psAdd.addBatch();

                for (Long memberId : memberIds) {
                    psAdd.setLong(1, newConversationId);
                    psAdd.setLong(2, memberId);
                    psAdd.setString(3, "member");
                    psAdd.addBatch();
                }
                psAdd.executeBatch();

                conn.commit();
                return newConversationId;

            } catch (SQLException e) {
                conn.rollback();
                logSql("createChannel", e);
            }
        } catch (SQLException e) {
            logSql("transactionSetupChannel", e);
        }
        return -1;
    }

    public static List<Channel> getChannelsForUser(String phone) {
        List<Channel> channels = new ArrayList<>();
        getUserId(phone).ifPresent(userId -> {
            String sql = "SELECT c.id, c.title, c.creator_id, c.picture FROM conversations c " +
                    "JOIN conversation_participants cp ON c.id = cp.id " +
                    "WHERE cp.user_id = ? AND c.type = 'channel'";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    channels.add(new Channel(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getLong("creator_id"),
                            rs.getBytes("picture")
                    ));
                }
            } catch (SQLException e) {
                logSql("getChannelsForUser", e);
            }
        });
        return channels;
    }

    public static long saveChannelMessage(long channelId, long senderId, String content, Long mediaId) {
        String sql = "INSERT INTO messages (id, conversation_id, sender_id, content, media_id) VALUES (?, ?, ?, ?, ?)";
        long messageId = System.nanoTime();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, channelId);
            ps.setLong(3, senderId);
            ps.setString(4, content);

            if (mediaId != null) {
                ps.setLong(5, mediaId);
            } else {
                ps.setNull(5, Types.BIGINT);
            }

            if (ps.executeUpdate() > 0) {
                return messageId;
            }
        } catch (SQLException e) {
            logSql("saveChannelMessage", e);
        }
        return -1;
    }

    public static boolean leaveChannel(long channelId, long userId) {
        String sql = "DELETE FROM conversation_participants WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, channelId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logSql("leaveChannel", e);
            return false;
        }
    }

    public static boolean deleteChannel(long channelId) {
        String deleteParticipantsSql = "DELETE FROM conversation_participants WHERE id = ?";
        String deleteMessagesSql = "DELETE FROM messages WHERE conversation_id = ?";
        String deleteConversationSql = "DELETE FROM conversations WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(deleteParticipantsSql);
                 PreparedStatement ps2 = conn.prepareStatement(deleteMessagesSql);
                 PreparedStatement ps3 = conn.prepareStatement(deleteConversationSql)) {

                ps1.setLong(1, channelId);
                ps1.executeUpdate();

                ps2.setLong(1, channelId);
                ps2.executeUpdate();

                ps3.setLong(1, channelId);
                ps3.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logSql("deleteChannel", e);
                return false;
            }
        } catch (SQLException e) {
            logSql("transactionDeleteChannel", e);
            return false;
        }
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

    public static void updateLastSeen(String phone) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.executeUpdate();
        } catch (SQLException e) {
            logSql("updateLastSeen", e);
        }
    }

    public static Optional<Timestamp> getLastSeen(String phone) {
        String sql = "SELECT last_seen FROM users WHERE phone = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(rs.getTimestamp("last_seen"));
            }
        } catch (SQLException e) {
            logSql("getLastSeen", e);
        }
        return Optional.empty();
    }

    private static void logSql(String op, Exception e) {
        System.err.println("[DB] " + op + " failed: " + e.getMessage());
    }
}

