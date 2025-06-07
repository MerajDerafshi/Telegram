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

        // Sample users for testing UI
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
            currentlySelectedUser = newValue;
            messagesListView.setItems(currentlySelectedUser.messagesList);
            chatRoomNameLabel.setText(currentlySelectedUser.userName);
            messagesListView.scrollTo(currentlySelectedUser.messagesList.size());
        });

        connection = new NetworkConnection(data -> Platform.runLater(() -> handleIncomingData(data)),
                "127.0.0.1", false, 55555, LogInController.userName);
        connection.openConnection();

        usersListView.getSelectionModel().select(0);
    }

    private void handleIncomingData(Object data) {
        try {
            if (!(data instanceof String)) return;

            String msg = data.toString();

            if (data instanceof ToolBox.ImageMessage) {
                ToolBox.ImageMessage imgMsg = (ToolBox.ImageMessage) data;
                if (!imgMsg.receiver.equals(localUser.getUserName())) return;

                int senderIndex = findUser(imgMsg.sender);
                if (senderIndex == -1) {
                    usersList.add(new UserViewModel(imgMsg.sender, "", getCurrentTime(), "0", userImage));
                    senderIndex = usersList.size() - 1;
                }


                ByteArrayInputStream bais = new ByteArrayInputStream(imgMsg.imageData);
                BufferedImage bufferedImage;
                try {
                    bufferedImage = ImageIO.read(bais);
                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    usersList.get(senderIndex).messagesList.add(
                            new MessageViewModel("", imgMsg.timestamp, false, true, fxImage));
                    messagesListView.scrollTo(usersList.get(senderIndex).messagesList.size());
                    usersList.get(senderIndex).notificationsNumber.setValue(
                            String.valueOf(Integer.parseInt(usersList.get(senderIndex).notificationsNumber.getValue()) + 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            if (msg.startsWith("SYSTEM_STATUS:")) {
                System.out.println("Status: " + msg);
                return;
            }

            String[] messageInfo = msg.split(">");
            if (messageInfo.length < 4) return;

            String type = messageInfo[0];
            String sender = messageInfo[1];
            String receiver = messageInfo[2];
            String content = messageInfo[3];

            if (!receiver.equals(localUser.getUserName())) return;

            int senderIndex = findUser(sender);
            if (senderIndex == -1) {
                usersList.add(new UserViewModel(sender, "", getCurrentTime(), "0", userImage));
                senderIndex = usersList.size() - 1;
            }

            UserViewModel senderUser = usersList.get(senderIndex);
            senderUser.time.setValue(getCurrentTime());
            senderUser.lastMessage.setValue(content);
            senderUser.notificationsNumber.setValue(String.valueOf(
                    Integer.parseInt(senderUser.notificationsNumber.getValue()) + 1
            ));

            senderUser.messagesList.add(new MessageViewModel(content, getCurrentTime(), false, false, null));
            messagesListView.scrollTo(senderUser.messagesList.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void sendMessage(ActionEvent event) {
        try {
            String message = messageField.getText();
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
            File imageFile = fileChooser.showOpenDialog(new Stage());
            if (imageFile == null) return;

            BufferedImage bufferedImage = ImageIO.read(imageFile);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);


            currentlySelectedUser.messagesList.add(
                    new MessageViewModel("", getCurrentTime(), true, true, image));
            messagesListView.scrollTo(currentlySelectedUser.messagesList.size());


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            ToolBox.ImageMessage imageMessage = new ToolBox.ImageMessage(
                    imageBytes,
                    localUser.getUserName(),
                    currentlySelectedUser.getUserName(),
                    getCurrentTime()
            );
            connection.sendData(imageMessage);

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

    // unused buttons
    @FXML void searchChatRoom(MouseEvent event) {}
    @FXML void settingsButtonClicked(MouseEvent event) {}
    @FXML void slideMenuClicked(MouseEvent event) {}
    @FXML void smileyButtonClicked(MouseEvent event) {}
    @FXML void vocalMessageClicked(MouseEvent event) {}
}
