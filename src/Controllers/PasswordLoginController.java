package Controllers;

import ToolBox.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PasswordLoginController implements Initializable {

    @FXML private Label greetingLabel;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    public static String phoneNumber;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        greetingLabel.setText("Hi! Enter password for " + phoneNumber);
    }

    @FXML
    void loginClicked(ActionEvent event) {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            errorLabel.setText("Password cannot be empty.");
            return;
        }


        if (DatabaseManager.verifyPassword(phoneNumber, password)) {
            LogInController.userName = phoneNumber;
            loadScene("../Views/homeView.fxml");
        } else {
            errorLabel.setText("Invalid password. Please try again.");
        }
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error: Could not load the application.");
        }
    }
}
