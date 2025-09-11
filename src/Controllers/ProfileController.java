package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.ImageCropper;
import ToolBox.NetworkConnection;
import ToolBox.PasswordUtils;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private ImageView avatarImageView;
    @FXML private TextField bioTextField;
    @FXML private TextField nameTextField;
    @FXML private TextField phoneTextField;
    @FXML private TextField usernameTextField;
    @FXML private PasswordField passwordField;
    @FXML private Button profileButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button logoutButton;
    @FXML private ListView<UserViewModel> usersListView;

    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;
    private NetworkConnection connection;
    private byte[] newAvatarData;

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

        loadUserProfile();
        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
    }

    @FXML
    private void groupClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createGroup.fxml"));
            Parent root = loader.load();

            CreateGroupController controller = loader.getController();
            controller.initData(localUser);


            Stage dialog = new Stage();
            dialog.setTitle("New Group");
            dialog.initOwner(((Node) event.getSource()).getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUserProfile() {
        DatabaseManager.getUserByPhone(localUser.getPhone()).ifPresent(dbUser -> {
            nameTextField.setText(dbUser.firstName);
            phoneTextField.setText(dbUser.phone);
            usernameTextField.setText(dbUser.username);
            bioTextField.setText(dbUser.bio);

            byte[] avatarBytes = dbUser.avatar;
            if (avatarBytes != null && avatarBytes.length > 0) {
                avatarImageView.setImage(new Image(new ByteArrayInputStream(avatarBytes)));
            } else {
                avatarImageView.setImage(new Image("resources/img/smile.png"));
            }
        });
    }

    @FXML
    void chooseAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            try {
                BufferedImage originalImage = ImageIO.read(selectedFile);
                BufferedImage croppedImage = ImageCropper.cropToSquare(originalImage);
                Image fxImage = SwingFXUtils.toFXImage(croppedImage, null);
                avatarImageView.setImage(fxImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(croppedImage, "png", baos);
                newAvatarData = baos.toByteArray();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void submitChanges(ActionEvent event) {
        String newName = nameTextField.getText();
        String newPhone = phoneTextField.getText();
        String newUsername = usernameTextField.getText();
        String newBio = bioTextField.getText();
        String newPassword = passwordField.getText();

        boolean success = DatabaseManager.updateUserProfile(localUser.getPhone(), newName, newPhone, newUsername, newBio);

        if (newAvatarData != null) {
            DatabaseManager.updateAvatar(newPhone, newAvatarData);
        }

        if (!newPassword.isEmpty()) {
            String hashedPassword = PasswordUtils.hashPassword(newPassword);
            DatabaseManager.updatePassword(newPhone, hashedPassword);
        }

        if (success) {
            System.out.println("Profile updated successfully!");
            localUser.firstName = newName;
            localUser.phone = newPhone;
            LogInController.userName = newPhone;
        } else {
            System.out.println("Failed to update profile.");
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

