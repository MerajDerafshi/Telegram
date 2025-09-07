package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.ChannelMessage;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

public class ChannelMemberViewController {

    @FXML private Label channelNameLabel;
    @FXML private ListView<MessageViewModel> messagesListView;
    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button profileButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button newChannelButton;
    @FXML private Button logoutButton;
    @FXML private Button channelInfoButton;

    private UserViewModel localUser;
    private UserViewModel channelViewModel;
    private ObservableList<UserViewModel> allUsersList;
    private NetworkConnection connection;

    public void initData(UserViewModel channelViewModel, UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.channelViewModel = channelViewModel;
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;
        this.connection.receiveCallback = this::handleIncomingData;

        channelNameLabel.setText(channelViewModel.getFirstName());
        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
        messagesListView.setCellFactory(param -> new MessageCustomCellController());
        loadMessageHistory();
    }

    private void handleIncomingData(Serializable data) {
        if (data instanceof ChannelMessage) {
            ChannelMessage msg = (ChannelMessage) data;
            if (msg.channelId == channelViewModel.channelId) {
                Platform.runLater(this::loadMessageHistory);
            }
        }
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadChannelMessages(channelViewModel.channelId);
        channelViewModel.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for (DatabaseManager.Message msg : history) {
            boolean isOutgoing = false; // For a member, all messages are incoming
            String time = timeFormat.format(msg.createdAt);

            if (msg.mediaData != null && msg.mimeType != null) {
                if (msg.mimeType.startsWith("image/")) {
                    Image image = new Image(new ByteArrayInputStream(msg.mediaData));
                    channelViewModel.messagesList.add(new MessageViewModel(msg.id, "📷 image", time, isOutgoing, image));
                } else {
                    channelViewModel.messagesList.add(new MessageViewModel(msg.id, msg.fileName, msg.mediaData, time, isOutgoing));
                }
            } else if (msg.content != null) {
                channelViewModel.messagesList.add(new MessageViewModel(msg.id, msg.content, time, isOutgoing));
            }
        }
        messagesListView.setItems(channelViewModel.messagesList);
    }


    @FXML
    void openChannelInfo(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/channelInfo.fxml"));
            Parent root = loader.load();
            ChannelInfoController controller = loader.getController();
            controller.initData(channelViewModel, localUser, allUsersList, connection);
            Stage stage = (Stage) channelInfoButton.getScene().getWindow();
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
            controller.initData(localUser, allUsersList);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setScene(new Scene(root));
            stage.showAndWait();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

