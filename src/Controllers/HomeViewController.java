package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static ToolBox.Utilities.getCurrentTime;

public class HomeViewController implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button logoutButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button profileButton;
    @FXML private Button newChannelButton;

    private NetworkConnection connection;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String currentUserPhone = LogInController.userName;

        Optional<DatabaseManager.User> dbUserOptional = DatabaseManager.getUserByPhone(currentUserPhone);
        if (dbUserOptional.isEmpty()) {
            return;
        }
        DatabaseManager.User dbUser = dbUserOptional.get();

        Image localUserAvatar = new Image("resources/img/smile.png");
        if (dbUser.avatar != null && dbUser.avatar.length > 0) {
            localUserAvatar = new Image(new ByteArrayInputStream(dbUser.avatar));
        }
        localUser = new UserViewModel(dbUser.firstName, dbUser.username, dbUser.phone, localUserAvatar);
        localUser.userId = dbUser.id;

        loadConversations();

        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openConversationView(newValue);
            }
        });

        connection = new NetworkConnection(this::handleIncomingData, "127.0.0.1", false, 55555, currentUserPhone);
        connection.openConnection();
    }

    private void loadConversations() {
        allUsersList = FXCollections.observableArrayList();

        List<UserViewModel> users = DatabaseManager.getAllUsers(localUser.getPhone()).stream()
                .map(user -> {
                    Image avatar = new Image("resources/img/smile.png");
                    if (user.avatar != null && user.avatar.length > 0) {
                        avatar = new Image(new ByteArrayInputStream(user.avatar));
                    }
                    UserViewModel uvm = new UserViewModel(user.firstName, user.username, user.phone, "Click to chat", getCurrentTime(), "0", avatar);
                    uvm.userId = user.id;
                    return uvm;
                })
                .collect(Collectors.toList());
        allUsersList.addAll(users);

        List<DatabaseManager.Channel> channels = DatabaseManager.getChannelsForUser(localUser.getPhone());
        for (DatabaseManager.Channel channel : channels) {
            Image avatar = new Image("resources/img/smile.png");
            if (channel.avatar != null && channel.avatar.length > 0) {
                avatar = new Image(new ByteArrayInputStream(channel.avatar));
            }
            UserViewModel channelVM = new UserViewModel(channel.title, "@" + channel.title.replaceAll("\\s+", ""), null, avatar);
            channelVM.isChannel = true;
            channelVM.channelId = channel.id;
            channelVM.creatorId = channel.creatorId;
            allUsersList.add(channelVM);
        }

        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
    }


    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> System.out.println("Data received on home screen: " + data));
    }

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
            } else {
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

    @FXML
    void openNewChannel(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createChannel1.fxml"));
            Parent root = loader.load();
            CreateChannelController1 controller = loader.getController();
            // CORRECTED: Passing only the required arguments
            controller.initData(localUser, allUsersList);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Create New Channel");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadConversations();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openProfile(MouseEvent event) {
        loadSceneFromEvent(event, "../Views/Profile.fxml");
    }

    @FXML
    void openSavedMessages(MouseEvent event) {
        loadSceneFromEvent(event, "../Views/saveMessageChat.fxml");
    }

    @FXML
    void logoutClicked(ActionEvent event) {
        try {
            if (connection != null) {
                connection.closeConnection();
            }
            Parent root = FXMLLoader.load(getClass().getResource("../Views/loginStarter.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSceneFromEvent(MouseEvent event, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            if (fxmlFile.contains("Profile")) {
                ProfileController controller = loader.getController();
                controller.initData(localUser, allUsersList, connection);
            } else if (fxmlFile.contains("saveMessageChat")) {
                SavedMessagesController controller = loader.getController();
                controller.initData(localUser, allUsersList, connection);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        return (Stage) usersListView.getScene().getWindow();
    }
}

