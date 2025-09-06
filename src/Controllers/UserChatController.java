package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.DeleteMessage;
import ToolBox.FileMessage;
import ToolBox.ImageMessage;
import ToolBox.NetworkConnection;
import ToolBox.TextMessage;
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
import java.text.SimpleDateFormat;
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

    private NetworkConnection connection;
    private UserViewModel currentlySelectedUser;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;
    private long localUserId;

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
        this.connection.receiveCallback = this::handleIncomingData;

        DatabaseManager.getUserId(localUser.getPhone()).ifPresent(id -> this.localUserId = id);

        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController() {{
            prefWidthProperty().bind(usersListView.widthProperty());
        }});

        chatRoomNameLabel.setText(selectedUser.getFirstName());

        loadMessageHistory();

        messagesListView.setCellFactory(param -> {
            MessageCustomCellController cell = new MessageCustomCellController();
            cell.prefWidthProperty().bind(messagesListView.widthProperty());
            // Set the callback for the delete action
            cell.deleteCallback = () -> deleteMessage(cell.getItem());
            return cell;
        });

        scrollToBottom();
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

        // 1. Delete from Database
        boolean success = DatabaseManager.deleteMessage(messageVM.messageId);

        if (success) {
            // 2. Remove from local UI
            currentlySelectedUser.messagesList.remove(messageVM);

            // 3. Send delete instruction to the other user
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

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> {
            try {
                if (data instanceof ImageMessage) {
                    ImageMessage imgMsg = (ImageMessage) data;
                    UserViewModel senderUser = findUserByPhone(imgMsg.sender);
                    if (senderUser == null || !imgMsg.receiver.equals(localUser.getPhone())) return;

                    Image fxImage = new Image(new ByteArrayInputStream(imgMsg.imageData));
                    MessageViewModel imageMsg = new MessageViewModel(-1, "📷 image", imgMsg.timestamp, false, fxImage);
                    senderUser.messagesList.add(imageMsg);

                } else if (data instanceof FileMessage) {
                    FileMessage fileMsg = (FileMessage) data;
                    UserViewModel senderUser = findUserByPhone(fileMsg.sender);
                    if (senderUser == null || !fileMsg.receiver.equals(localUser.getPhone())) return;

                    MessageViewModel fileModel = new MessageViewModel(-1, fileMsg.fileName, fileMsg.fileData, fileMsg.timestamp, false);
                    senderUser.messagesList.add(fileModel);

                } else if (data instanceof TextMessage) {
                    TextMessage textMsg = (TextMessage) data;
                    if (!textMsg.receiver.equals(localUser.getPhone())) return;
                    UserViewModel senderUser = findUserByPhone(textMsg.sender);
                    if(senderUser == null) return;

                    MessageViewModel newMsg = new MessageViewModel(-1, textMsg.content, textMsg.timestamp, false);
                    senderUser.messagesList.add(newMsg);

                } else if (data instanceof DeleteMessage) {
                    DeleteMessage deleteMsg = (DeleteMessage) data;
                    // Find the user whose message is being deleted
                    UserViewModel relevantUser = findUserByPhone(deleteMsg.senderPhone);
                    if (relevantUser != null) {
                        relevantUser.messagesList.removeIf(msg -> msg.messageId == deleteMsg.messageId);
                    }
                }

                if (messagesListView != null) messagesListView.refresh();
                scrollToBottom();

            } catch (Exception e) {
                System.err.println("[ERROR] Failed to handle incoming data: " + e.getMessage());
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

    private void executeSendMessage() {
        String message = messageField.getText();
        if (message == null || message.trim().isEmpty()) return;

        try {
            String currentTime = getCurrentTime();
            DatabaseManager.saveMessage(localUser.getPhone(), currentlySelectedUser.getPhone(), message, null);

            // Reload to get the new message
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
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
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

