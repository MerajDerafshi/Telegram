package Controllers;

import Models.UserViewModel;
import Models.MessageViewModel;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import static ToolBox.Utilities.getCurrentTime;

public class HomeController implements Initializable {

    @FXML
    private Label userNameLabel;
    @FXML
    private Label chatRoomNameLabel;
    @FXML
    private TextField messageField;
    @FXML
    private ListView<UserViewModel> usersListView;
    @FXML
    private ListView<MessageViewModel> messagesListView;

    NetworkConnection connection;
    private ObservableList<UserViewModel> usersList = FXCollections.observableArrayList();
    UserViewModel currentlySelectedUser, localUser;
    Image userImage = new Image("resources/img/smile.png");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        localUser = new UserViewModel(LogInController.userName, "message", getCurrentTime(), "0", userImage);
        userNameLabel.setText(localUser.getUserName());
        //examples for testing, not actual users
        usersList.addAll(
                new UserViewModel("Meraj", "Hey!", getCurrentTime(), "1", userImage),
                new UserViewModel("Amin", "Yo", getCurrentTime(), "0", userImage)
        );

        usersListView.setItems(usersList);
        usersListView.setCellFactory(param -> new UserCustomCellController() {{
            prefWidthProperty().bind(usersListView.widthProperty());
        }});
        messagesListView.setCellFactory(param -> new MessageCustomCellController() {{
            prefWidthProperty().bind(messagesListView.widthProperty());
        }});

        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentlySelectedUser = newValue;
                messagesListView.setItems(currentlySelectedUser.messagesList);
                chatRoomNameLabel.setText(currentlySelectedUser.userName);
                messagesListView.scrollTo(currentlySelectedUser.messagesList.size());
            }
        });

        connection = new NetworkConnection(data -> Platform.runLater(() -> handleIncomingData(data)),
                "127.0.0.1", false, 55555, LogInController.userName);
        connection.openConnection();

        usersListView.getSelectionModel().select(0);
    }

    private int findOrAddUser(String userName) {
        int index = findUser(userName);
        if (index != -1) return index;

        UserViewModel newUser = new UserViewModel(userName, "", getCurrentTime(), "0", userImage);
        usersList.add(newUser);
        return usersList.indexOf(newUser);
    }

    private void handleIncomingData(Object data) {
        try {
            if (data instanceof ToolBox.ImageMessage) {
                ToolBox.ImageMessage imgMsg = (ToolBox.ImageMessage) data;
                if (!imgMsg.receiver.equals(localUser.getUserName())) return;

                int senderIndex = findOrAddUser(imgMsg.sender);
                ByteArrayInputStream bais = new ByteArrayInputStream(imgMsg.imageData);
                BufferedImage bufferedImage = ImageIO.read(bais);
                Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);

                MessageViewModel imageMsg = new MessageViewModel("📷 image", imgMsg.timestamp, false, true, fxImage);
                usersList.get(senderIndex).messagesList.add(imageMsg);
                usersList.get(senderIndex).notificationsNumber.setValue(
                        String.valueOf(Integer.parseInt(usersList.get(senderIndex).notificationsNumber.getValue()) + 1));
                messagesListView.scrollTo(usersList.get(senderIndex).messagesList.size());
                return;
            }

            if (data instanceof ToolBox.FileMessage) {
                ToolBox.FileMessage fileMsg = (ToolBox.FileMessage) data;
                if (!fileMsg.receiver.equals(localUser.getUserName())) return;

                int senderIndex = findOrAddUser(fileMsg.sender);
                MessageViewModel fileModel = new MessageViewModel(fileMsg.fileName, fileMsg.fileData, fileMsg.timestamp, false);
                usersList.get(senderIndex).messagesList.add(fileModel);
                usersList.get(senderIndex).notificationsNumber.setValue(
                        String.valueOf(Integer.parseInt(usersList.get(senderIndex).notificationsNumber.getValue()) + 1));
                messagesListView.scrollTo(usersList.get(senderIndex).messagesList.size());
                return;
            }

            if (data instanceof String) {
                String msg = (String) data;
                if (msg.startsWith("SYSTEM_STATUS:")) {
                    System.out.println("[Status] " + msg);
                    return;
                }

                String[] parts = msg.split(">");
                if (parts.length < 4 || !parts[2].equals(localUser.getUserName())) return;

                String type = parts[0];
                String sender = parts[1];
                String receiver = parts[2];
                String content = parts[3];

                int senderIndex = findOrAddUser(sender);
                MessageViewModel textMsg = new MessageViewModel(content, getCurrentTime(), false, false, null);
                usersList.get(senderIndex).messagesList.add(textMsg);
                usersList.get(senderIndex).lastMessage.set(content);
                usersList.get(senderIndex).time.set(getCurrentTime());
                usersList.get(senderIndex).notificationsNumber.setValue(
                        String.valueOf(Integer.parseInt(usersList.get(senderIndex).notificationsNumber.getValue()) + 1));
                messagesListView.scrollTo(usersList.get(senderIndex).messagesList.size());
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to handle incoming data: " + e.getMessage());
            e.printStackTrace();
        }
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

        if (currentlySelectedUser == null) {
            System.out.println("No user selected. Cannot send message.");
            return;
        }

        try {
            currentlySelectedUser.messagesList.add(
                    new MessageViewModel(message, getCurrentTime(), true, false, null));
            connection.sendData("text>" + localUser.getUserName() + ">" + currentlySelectedUser.getUserName() + ">" + message);
            messageField.clear();
            messagesListView.scrollTo(currentlySelectedUser.messagesList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void attachFile(MouseEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Supported Files", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.docx")
            );
            File file = fileChooser.showOpenDialog(new Stage());
            if (file == null) return;

            String fileName = file.getName().toLowerCase();
            String timestamp = getCurrentTime();

            byte[] fileBytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileBytes);
            }

            if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                Image image = SwingFXUtils.toFXImage(ImageIO.read(file), null);
                currentlySelectedUser.messagesList.add(new MessageViewModel("📷 image", timestamp, true, true, image));

                ToolBox.ImageMessage imageMessage = new ToolBox.ImageMessage(fileBytes, localUser.getUserName(), currentlySelectedUser.getUserName(), timestamp);
                connection.sendData(imageMessage);
            } else {
                ToolBox.FileMessage fileMessage = new ToolBox.FileMessage(fileBytes, file.getName(), localUser.getUserName(), currentlySelectedUser.getUserName(), timestamp);
                currentlySelectedUser.messagesList.add(new MessageViewModel(fileMessage.fileName, fileMessage.fileData, timestamp, true));
                connection.sendData(fileMessage);
            }

            messagesListView.scrollTo(currentlySelectedUser.messagesList.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void closeApp(MouseEvent event) {
        try {
            connection.closeConnection();
            Main.stage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void minimizeApp(MouseEvent event) {
        Main.stage.setIconified(true);
    }

    int findUser(String userName) {
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).getUserName().equals(userName)) {
                return i;
            }
        }
        return -1;
    }

    @FXML void searchChatRoom(MouseEvent event) {}
    @FXML void settingsButtonClicked(MouseEvent event) {}
    @FXML void slideMenuClicked(MouseEvent event) {}
    @FXML void smileyButtonClicked(MouseEvent event) {}
}
