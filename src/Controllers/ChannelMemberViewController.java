package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.ChannelMessage;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
    @FXML private TextField userSearchField;
    @FXML private TextField messageSearchField;

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
        setupUserSearchFunctionality();
        setupMessageSearchFunctionality();

        // --- START: BUG FIX ---
        // Added the listener to make the chat list on the left functional
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openConversationView(newValue);
            }
        });
        // --- END: BUG FIX ---

        loadMessageHistory();
    }

    private void setupUserSearchFunctionality() {
        FilteredList<UserViewModel> filteredData = new FilteredList<>(allUsersList, p -> true);
        userSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(userViewModel -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return userViewModel.getFirstName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<UserViewModel> sortedData = new SortedList<>(filteredData);
        usersListView.setItems(sortedData);
    }

    private void setupMessageSearchFunctionality() {
        FilteredList<MessageViewModel> filteredMessages = new FilteredList<>(channelViewModel.messagesList, p -> true);
        messageSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMessages.setPredicate(messageViewModel -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (messageViewModel.getMessage() != null) {
                    return messageViewModel.getMessage().toLowerCase().contains(lowerCaseFilter);
                }
                return false;
            });
        });
        SortedList<MessageViewModel> sortedMessages = new SortedList<>(filteredMessages);
        messagesListView.setItems(sortedMessages);
    }

    // --- START: NEW METHOD TO HANDLE NAVIGATION ---
    private void openConversationView(UserViewModel selectedItem) {
        try {
            FXMLLoader loader;
            if (selectedItem.isChannel) {
                if (localUser.userId == selectedItem.creatorId) {
                    loader = new FXMLLoader(getClass().getResource("../Views/channelCreatorView.fxml"));
                    Parent root = loader.load();
                    ChannelCreatorViewController controller = loader.getController();
                    controller.initData(selectedItem, localUser, allUsersList, connection);
                    getStage().setScene(new Scene(root));
                } else {
                    loader = new FXMLLoader(getClass().getResource("../Views/channelMemberView.fxml"));
                    Parent root = loader.load();
                    ChannelMemberViewController controller = loader.getController();
                    controller.initData(selectedItem, localUser, allUsersList, connection);
                    getStage().setScene(new Scene(root));
                }
            } else { // It's a user chat
                loader = new FXMLLoader(getClass().getResource("../Views/userChat.fxml"));
                Parent root = loader.load();
                UserChatController controller = loader.getController();
                controller.initData(selectedItem, localUser, allUsersList, connection);
                getStage().setScene(new Scene(root));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // --- END: NEW METHOD ---

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
            getStage().setScene(new Scene(root));
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
            getStage().setScene(new Scene(root));
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
            getStage().setScene(new Scene(root));
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
            getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        return (Stage) usersListView.getScene().getWindow();
    }
}

