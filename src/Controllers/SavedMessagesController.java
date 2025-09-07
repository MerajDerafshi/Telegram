package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

public class SavedMessagesController implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Label chatRoomNameLabel;
    @FXML private TextField messageField;
    @FXML private ListView<MessageViewModel> messagesListView;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button newChannelButton;

    private NetworkConnection connection;
    private UserViewModel localUser;
    private UserViewModel savedMessagesUser;
    private ObservableList<UserViewModel> allUsersList;
    private long localUserId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openChatView(newValue);
            }
        });
    }

    public void initData(UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;
        if (this.connection != null) {
            this.connection.receiveCallback = this::handleIncomingData;
        }

        this.savedMessagesUser = new UserViewModel(localUser.getFirstName(), localUser.getUsername(), localUser.getPhone(), localUser.getAvatarImage());

        DatabaseManager.getUserId(localUser.getPhone()).ifPresent(id -> this.localUserId = id);

        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());

        chatRoomNameLabel.setText("Saved Messages");

        loadMessageHistory();

        messagesListView.setCellFactory(param -> {
            MessageCustomCellController cell = new MessageCustomCellController();
            cell.prefWidthProperty().bind(messagesListView.widthProperty());
            cell.deleteCallback = () -> deleteMessage(cell.getItem());
            return cell;
        });

        scrollToBottom();
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadMessages(localUser.getPhone(), localUser.getPhone());
        savedMessagesUser.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for(DatabaseManager.Message msg : history) {
            boolean isOutgoing = true;
            String time = timeFormat.format(msg.createdAt);

            if (msg.mediaData != null && msg.mimeType != null) {
                if (msg.mimeType.startsWith("image/")) {
                    Image image = new Image(new ByteArrayInputStream(msg.mediaData));
                    savedMessagesUser.messagesList.add(new MessageViewModel(msg.id, "📷 image", time, isOutgoing, image));
                } else {
                    savedMessagesUser.messagesList.add(new MessageViewModel(msg.id, msg.fileName, msg.mediaData, time, isOutgoing));
                }
            } else if (msg.content != null){
                savedMessagesUser.messagesList.add(new MessageViewModel(msg.id, msg.content, time, isOutgoing));
            }
        }
        messagesListView.setItems(savedMessagesUser.messagesList);
    }

    private void deleteMessage(MessageViewModel messageVM) {
        if (messageVM == null) return;
        boolean success = DatabaseManager.deleteMessage(messageVM.messageId);
        if (success) {
            savedMessagesUser.messagesList.remove(messageVM);
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
        // Not needed for saved messages
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

        DatabaseManager.saveMessage(localUser.getPhone(), localUser.getPhone(), message, null);
        loadMessageHistory();
        scrollToBottom();
        messageField.clear();
    }

    @FXML
    void attachFile(MouseEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Save");
            File file = fileChooser.showOpenDialog(new Stage());
            if (file == null) return;

            String fileName = file.getName();
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

            DatabaseManager.saveMessage(localUser.getPhone(), localUser.getPhone(), null, mediaId);
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

    @FXML
    void openNewChannel(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createChannel1.fxml"));
            Parent root = loader.load();
            CreateChannelController1 controller = loader.getController();
            // CORRECTED: Passing the correct arguments
            controller.initData(localUser, allUsersList);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            goHome();
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

    private void scrollToBottom() {
        if (savedMessagesUser != null && !savedMessagesUser.messagesList.isEmpty()) {
            messagesListView.scrollTo(savedMessagesUser.messagesList.size() - 1);
        }
    }

    private void goHome() {
        try {
            Stage stage = (Stage) newChannelButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/homeView.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

