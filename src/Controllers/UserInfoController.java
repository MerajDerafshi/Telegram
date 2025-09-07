package Controllers;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserInfoController implements Initializable {

    @FXML private ImageView avatarImageView;
    @FXML private Label nameLabel;
    @FXML private Label statusLabel;
    @FXML private Label phoneLabel;
    @FXML private Label bioLabel;
    @FXML private Button savedMessagesButton;
    @FXML private Button profileButton;
    @FXML private Label usernameLabel;
    @FXML private Button deleteChatButton;
    @FXML private Button closeButton;
    @FXML private Button logoutButton;
    @FXML private ListView<UserViewModel> usersListView;

    private UserViewModel selectedUser;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;
    private NetworkConnection connection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openChatView(newValue);
            }
        });
    }

    public void initData(UserViewModel selectedUser, UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.selectedUser = selectedUser;
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;

        loadUserInfo();
        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
    }

    private void loadUserInfo() {
        DatabaseManager.getUserByPhone(selectedUser.getPhone()).ifPresent(dbUser -> {
            nameLabel.setText(dbUser.firstName);
            phoneLabel.setText(dbUser.phone);
            bioLabel.setText(dbUser.bio != null ? dbUser.bio : "No bio");
            usernameLabel.setText("@" + dbUser.username);

            byte[] avatarBytes = dbUser.avatar;
            if (avatarBytes != null && avatarBytes.length > 0) {
                avatarImageView.setImage(new Image(new ByteArrayInputStream(avatarBytes)));
            } else {
                avatarImageView.setImage(new Image("resources/img/smile.png"));
            }
        });

        // Fetch and display last seen status
        Optional<Timestamp> lastSeen = DatabaseManager.getLastSeen(selectedUser.getPhone());
        if (connection.isUserOnline(selectedUser.getPhone())) {
            statusLabel.setText("online");
        } else {
            lastSeen.ifPresent(timestamp -> statusLabel.setText(formatLastSeen(timestamp)));
        }
    }

    private String formatLastSeen(Timestamp lastSeen) {
        if (lastSeen == null) {
            return "last seen a long time ago";
        }

        long diff = new Date().getTime() - lastSeen.getTime();
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffDays > 0) {
            return "last seen on " + new SimpleDateFormat("MMM dd").format(lastSeen);
        } else if (diffHours > 0) {
            return String.format("last seen %d hours ago", diffHours);
        } else if (diffMinutes > 0) {
            return String.format("last seen %d minutes ago", diffMinutes);
        } else {
            return "last seen just now";
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
    void deleteChatHistory(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Chat History");
        alert.setHeaderText("Are you sure you want to delete the entire chat history with " + selectedUser.getFirstName() + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = DatabaseManager.deleteChatHistory(localUser.getPhone(), selectedUser.getPhone());
            if (success) {
                selectedUser.messagesList.clear();
                // Navigate back to the home screen
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/homeView.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) deleteChatButton.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to delete chat history.");
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    void closeInfo(ActionEvent event) {
        openChatView(selectedUser);
    }

    private void openChatView(UserViewModel user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/userChat.fxml"));
            Parent root = loader.load();
            UserChatController controller = loader.getController();
            controller.initData(user, localUser, allUsersList, connection);
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
