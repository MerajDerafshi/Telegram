package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ResourceBundle;

import static ToolBox.Utilities.getCurrentTime;


public class UserChatController implements Initializable {

    // --- FXML Components ---
    @FXML private ListView<UserViewModel> usersListView; // Left panel
    @FXML private Label chatRoomNameLabel;
    @FXML private TextField messageField;
    @FXML private ListView<MessageViewModel> messagesListView;

    private NetworkConnection connection;
    private UserViewModel currentlySelectedUser;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;

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
        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController() {{
            prefWidthProperty().bind(usersListView.widthProperty());
        }});

        chatRoomNameLabel.setText(selectedUser.getUserName());
        messagesListView.setItems(selectedUser.messagesList);
        messagesListView.setCellFactory(param -> new MessageCustomCellController() {{
            prefWidthProperty().bind(messagesListView.widthProperty());
        }});

        scrollToBottom();
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
                if (data instanceof ToolBox.ImageMessage) {
                    ToolBox.ImageMessage imgMsg = (ToolBox.ImageMessage) data;
                    UserViewModel senderUser = findUserByName(imgMsg.sender);
                    if (senderUser == null || !imgMsg.receiver.equals(localUser.getUserName())) return;

                    ByteArrayInputStream bais = new ByteArrayInputStream(imgMsg.imageData);
                    BufferedImage bufferedImage = ImageIO.read(bais);
                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    MessageViewModel imageMsg = new MessageViewModel("📷 image", imgMsg.timestamp, false, true, fxImage);
                    senderUser.messagesList.add(imageMsg);

                } else if (data instanceof ToolBox.FileMessage) {
                    ToolBox.FileMessage fileMsg = (ToolBox.FileMessage) data;
                    UserViewModel senderUser = findUserByName(fileMsg.sender);
                    if (senderUser == null || !fileMsg.receiver.equals(localUser.getUserName())) return;

                    MessageViewModel fileModel = new MessageViewModel(fileMsg.fileName, fileMsg.fileData, fileMsg.timestamp, false);
                    senderUser.messagesList.add(fileModel);

                } else if (data instanceof String) {
                    String msg = (String) data;
                    if (msg.startsWith("SYSTEM_STATUS:")) return;

                    String[] parts = msg.split(">");
                    if (parts.length < 4 || !parts[2].equals(localUser.getUserName())) return;
                    UserViewModel senderUser = findUserByName(parts[1]);
                    if(senderUser == null) return;

                    String content = parts[3];
                    MessageViewModel textMsg = new MessageViewModel(content, getCurrentTime(), false, false, null);
                    senderUser.messagesList.add(textMsg);
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
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        try {
            currentlySelectedUser.messagesList.add(
                    new MessageViewModel(message, getCurrentTime(), true, false, null));
            connection.sendData("text>" + localUser.getUserName() + ">" + currentlySelectedUser.getUserName() + ">" + message);
            messageField.clear();
            scrollToBottom();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void attachFile(MouseEvent event) {

    }

    private UserViewModel findUserByName(String username) {
        for (UserViewModel user : allUsersList) {
            if (user.getUserName().equals(username)) {
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

