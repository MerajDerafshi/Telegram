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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static ToolBox.Utilities.getCurrentTime;

public class HomeViewController implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button logoutButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button profileButton;


    private NetworkConnection connection;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String currentUserPhone = LogInController.userName;

        Optional<DatabaseManager.User> dbUserOptional = DatabaseManager.getUserByPhone(currentUserPhone);
        String localUserFirstName = dbUserOptional.map(user -> user.firstName).orElse(currentUserPhone);
        String localUserUsername = dbUserOptional.map(user -> user.username).orElse(currentUserPhone);


        localUser = new UserViewModel(localUserFirstName, localUserUsername, currentUserPhone, new Image("resources/img/smile.png"));

        allUsersList = FXCollections.observableArrayList(
                DatabaseManager.getAllUsers(currentUserPhone).stream()
                        .map(dbUser -> {
                            Image avatar = new Image("resources/img/smile.png"); // Default avatar
                            if (dbUser.avatar != null && dbUser.avatar.length > 0) {
                                avatar = new Image(new ByteArrayInputStream(dbUser.avatar));
                            }
                            return new UserViewModel(dbUser.firstName, dbUser.username, dbUser.phone, "Click to chat", getCurrentTime(), "0", avatar);
                        })
                        .collect(Collectors.toList())
        );

        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());

        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openChatView(newValue);
            }
        });

        connection = new NetworkConnection(this::handleIncomingData, "127.0.0.1", false, 55555, currentUserPhone);
        connection.openConnection();
    }

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> System.out.println("Data received on home screen: " + data));
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
}

