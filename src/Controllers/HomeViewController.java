package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static ToolBox.Utilities.getCurrentTime;

public class HomeViewController implements Initializable {

    @FXML
    private ListView<UserViewModel> usersListView;

    private NetworkConnection connection;
    private final ObservableList<UserViewModel> usersList = FXCollections.observableArrayList();
    private UserViewModel localUser;
    private final Image userImage = new Image("resources/img/smile.png");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserAndConnection();
        setupListView();
    }

    public void initData(NetworkConnection connection, ObservableList<UserViewModel> usersList, UserViewModel localUser) {
        this.connection = connection;
        this.usersList.setAll(usersList);
        this.localUser = localUser;
        this.connection.receiveCallback = this::handleIncomingData;
    }


    private void setupUserAndConnection() {
        String userPhone = LogInController.userName;
        DatabaseManager.User dbUser = DatabaseManager.getUserByPhone(userPhone).orElse(null);
        if (dbUser != null) {
            localUser = new UserViewModel(dbUser.firstName, dbUser.phone, "message", getCurrentTime(), "0", userImage);
        } else {
            localUser = new UserViewModel(userPhone, userPhone, "message", getCurrentTime(), "0", userImage);
        }


        List<DatabaseManager.User> allDbUsers = DatabaseManager.getAllUsers(userPhone);
        for (DatabaseManager.User user : allDbUsers) {
            usersList.add(new UserViewModel(user.firstName, user.phone, "Click to chat", getCurrentTime(), "0", userImage));
        }

        connection = new NetworkConnection(this::handleIncomingData, "127.0.0.1", false, 8080, userPhone);
        connection.openConnection();
    }

    private void setupListView() {
        usersListView.setItems(usersList);
        usersListView.setCellFactory(param -> new UserCustomCellController() {{
            prefWidthProperty().bind(usersListView.widthProperty());
        }});

        // Add listener to switch scenes when a user is selected
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openChatView(newValue);
            }
        });
    }

    private void openChatView(UserViewModel selectedUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/userChat.fxml"));
            Parent root = loader.load();

            UserChatController controller = loader.getController();
            controller.initData(selectedUser, localUser, usersList, connection);

            Stage stage = (Stage) usersListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> {
            System.out.println("HomeViewController received: " + data.toString());
        });
    }
}

