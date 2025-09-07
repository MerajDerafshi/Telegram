package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static ToolBox.Utilities.getCurrentTime;

public class UserChatController implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Label chatRoomNameLabel;
    @FXML private TextField messageField;
    @FXML private ListView<MessageViewModel> messagesListView;
    @FXML private Button logoutButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button profileButton;
    @FXML private Button userInfoButton;
    @FXML private Label lastSeenLabel;
    @FXML private Button voiceRecordButton;

    private NetworkConnection connection;
    private UserViewModel currentlySelectedUser;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;
    private long localUserId;

    private VoiceRecorder voiceRecorder;
    private boolean isRecording = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != currentlySelectedUser) {
                openChatView(newValue);
            }
        });
    }

    public void initData(UserViewModel selectedUser, UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.currentlySelectedUser = selectedUser;
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;
        if (this.connection != null) {
            this.connection.receiveCallback = this::handleIncomingData;
        }

        DatabaseManager.getUserId(localUser.getPhone()).ifPresent(id -> this.localUserId = id);

        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());

        chatRoomNameLabel.setText(selectedUser.getFirstName());

        if (connection != null && connection.isUserOnline(selectedUser.getPhone())) {
            lastSeenLabel.setText("online");
        } else {
            DatabaseManager.getLastSeen(selectedUser.getPhone()).ifPresent(timestamp -> {
                lastSeenLabel.setText(formatLastSeen(timestamp));
            });
        }

        loadMessageHistory();

        messagesListView.setCellFactory(param -> {
            MessageCustomCellController cell = new MessageCustomCellController();
            cell.prefWidthProperty().bind(messagesListView.widthProperty());
            cell.deleteCallback = () -> deleteMessage(cell.getItem());
            return cell;
        });

        scrollToBottom();
    }

    private String formatLastSeen(Timestamp lastSeen) {
        if (lastSeen == null) return "last seen a long time ago";
        long diff = new Date().getTime() - lastSeen.getTime();
        long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffDays > 0) return "last seen on " + new SimpleDateFormat("MMM dd").format(lastSeen);
        if (diffHours > 0) return "last seen " + diffHours + " hours ago";
        if (diffMinutes > 0) return "last seen " + diffMinutes + " minutes ago";
        return "last seen just now";
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadMessages(localUser.getPhone(), currentlySelectedUser.getPhone());
        currentlySelectedUser.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for(DatabaseManager.Message msg : history) {
            boolean isOutgoing = msg.senderId == localUserId;
            String time = timeFormat.format(msg.createdAt);

            if (msg.mediaData != null && msg.mimeType != null) {
                if (msg.mimeType.startsWith("image/")) {
                    Image image = new Image(new ByteArrayInputStream(msg.mediaData));
                    currentlySelectedUser.messagesList.add(new MessageViewModel(msg.id, "📷 image", time, isOutgoing, image));
                } else {
                    currentlySelectedUser.messagesList.add(new MessageViewModel(msg.id, msg.fileName, msg.mediaData, time, isOutgoing));
                }
            } else if (msg.content != null){
                currentlySelectedUser.messagesList.add(new MessageViewModel(msg.id, msg.content, time, isOutgoing));
            }
        }
        messagesListView.setItems(currentlySelectedUser.messagesList);
    }

    private void deleteMessage(MessageViewModel messageVM) {
        if (messageVM == null) return;

        boolean success = DatabaseManager.deleteMessage(messageVM.messageId);

        if (success) {
            currentlySelectedUser.messagesList.remove(messageVM);
            try {
                DeleteMessage deleteInstruction = new DeleteMessage(messageVM.messageId, localUser.getPhone(), currentlySelectedUser.getPhone());
                connection.sendData(deleteInstruction);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void openChatView(UserViewModel selectedUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/userChat.fxml"));
            Parent root = loader.load();
            UserChatController controller = loader.getController();
            controller.initData(selectedUser, localUser, allUsersList, connection);
            Stage stage = (Stage) usersListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openSavedMessages(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/saveMessageChat.fxml"));
            Parent root = loader.load();
            SavedMessagesController controller = loader.getController();
            controller.initData(localUser, allUsersList, connection);
            Stage stage = (Stage) savedMessagesButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/Profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.initData(localUser, allUsersList, connection);
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openUserInfo(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/userInfo.fxml"));
            Parent root = loader.load();
            UserInfoController controller = loader.getController();
            controller.initData(currentlySelectedUser, localUser, allUsersList, connection);
            Stage stage = (Stage) userInfoButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> {
            try {
                UserViewModel senderUser = null;
                if (data instanceof ImageMessage) {
                    ImageMessage imgMsg = (ImageMessage) data;
                    if (!imgMsg.receiver.equals(localUser.getPhone())) return;
                    senderUser = findUserByPhone(imgMsg.sender);
                    if (senderUser == null) return;

                    Image fxImage = new Image(new ByteArrayInputStream(imgMsg.imageData));
                    senderUser.messagesList.add(new MessageViewModel(-1, "📷 image", imgMsg.timestamp, false, fxImage));

                } else if (data instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) data;
                    if (!fileMsg.receiver.equals(localUser.getPhone())) return;
                    senderUser = findUserByPhone(fileMsg.sender);
                    if (senderUser == null) return;

                    senderUser.messagesList.add(new MessageViewModel(-1, fileMsg.fileName, fileMsg.fileData, fileMsg.timestamp, false));

                } else if (data instanceof TextMessage) {
                    TextMessage textMsg = (TextMessage) data;
                    if (!textMsg.receiver.equals(localUser.getPhone())) return;
                    senderUser = findUserByPhone(textMsg.sender);
                    if(senderUser == null) return;

                    senderUser.messagesList.add(new MessageViewModel(-1, textMsg.content, textMsg.timestamp, false));

                } else if (data instanceof DeleteMessage) {
                    DeleteMessage deleteMsg = (DeleteMessage) data;
                    senderUser = findUserByPhone(deleteMsg.senderPhone);
                    if (senderUser != null) {
                        senderUser.messagesList.removeIf(msg -> msg.messageId == deleteMsg.messageId);
                    }
                }

                if (senderUser != null && senderUser.getPhone().equals(currentlySelectedUser.getPhone())) {
                    messagesListView.refresh();
                    scrollToBottom();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void sendMessageOnEnter(ActionEvent event) {
        executeSendMessage();
    }

    @FXML
    void sendMessageIconClicked(MouseEvent event) {
        executeSendMessage();
    }

    @FXML
    void toggleVoiceRecording(MouseEvent event) {
        if (!isRecording) {
            voiceRecorder = new VoiceRecorder();
            voiceRecorder.startRecording();
            isRecording = true;
            // You can change the button icon here to indicate recording
            System.out.println("Started recording...");
        } else {
            byte[] voiceData = voiceRecorder.stopRecording();
            isRecording = false;
            // Change button icon back
            System.out.println("Stopped recording, sending voice message...");
            sendVoiceMessage(voiceData);
        }
    }

    private void sendVoiceMessage(byte[] voiceData) {
        if (voiceData == null || voiceData.length == 0) return;

        try {
            String fileName = "voice_message_" + System.currentTimeMillis() + ".mp3";
            String timestamp = getCurrentTime();
            String mimeType = getMimeType(fileName);

            Optional<Long> mediaIdOpt = DatabaseManager.saveMediaAndGetId(localUserId, voiceData, fileName, mimeType);
            if(mediaIdOpt.isEmpty()) {
                System.err.println("Failed to save voice message to database.");
                return;
            }
            long mediaId = mediaIdOpt.get();

            DatabaseManager.saveMessage(localUser.getPhone(), currentlySelectedUser.getPhone(), null, mediaId);
            loadMessageHistory();
            scrollToBottom();

            FileMessage voiceMessage = new FileMessage(voiceData, fileName, localUser.getPhone(), currentlySelectedUser.getPhone(), timestamp);
            connection.sendData(voiceMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void executeSendMessage() {
        String message = messageField.getText();
        if (message == null || message.trim().isEmpty()) return;

        try {
            String currentTime = getCurrentTime();
            DatabaseManager.saveMessage(localUser.getPhone(), currentlySelectedUser.getPhone(), message, null);

            loadMessageHistory();
            scrollToBottom();

            TextMessage textMessage = new TextMessage(message, localUser.getPhone(), currentlySelectedUser.getPhone(), currentTime);
            connection.sendData(textMessage);

            messageField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void attachFile(MouseEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Send");
            File file = fileChooser.showOpenDialog(new Stage());
            if (file == null) return;

            String fileName = file.getName();
            String timestamp = getCurrentTime();

            byte[] fileBytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileBytes);
            }

            String mimeType = getMimeType(fileName);

            Optional<Long> mediaIdOpt = DatabaseManager.saveMediaAndGetId(localUserId, fileBytes, fileName, mimeType);
            if(mediaIdOpt.isEmpty()) {
                System.err.println("Failed to save media to database.");
                return;
            }
            long mediaId = mediaIdOpt.get();

            if (mimeType.startsWith("image/")) {
                ImageMessage imageMessage = new ImageMessage(fileBytes, localUser.getPhone(), currentlySelectedUser.getPhone(), timestamp);
                connection.sendData(imageMessage);
            } else {
                FileMessage fileMessage = new FileMessage(fileBytes, fileName, localUser.getPhone(), currentlySelectedUser.getPhone(), timestamp);
                connection.sendData(fileMessage);
            }

            DatabaseManager.saveMessage(localUser.getPhone(), currentlySelectedUser.getPhone(), null, mediaId);
            loadMessageHistory();
            scrollToBottom();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void logoutClicked(ActionEvent event) {
        try {
            if (connection != null) {
                connection.closeConnection();
            }
            Parent root = FXMLLoader.load(getClass().getResource("../Views/loginStarter.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getMimeType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }

        switch (extension) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "mp3": return "audio/mpeg";
            default: return "application/octet-stream";
        }
    }

    private UserViewModel findUserByPhone(String phone) {
        for (UserViewModel user : allUsersList) {
            if (user.getPhone().equals(phone)) {
                return user;
            }
        }
        return null;
    }

    private void scrollToBottom() {
        if (currentlySelectedUser != null && !currentlySelectedUser.messagesList.isEmpty()) {
            messagesListView.scrollTo(currentlySelectedUser.messagesList.size() - 1);
        }
    }
}

