package Controllers;

import ToolBox.DatabaseManager;
import ToolBox.PasswordUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CreateAccountController implements Initializable {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField; // To show plain text password
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Text passwordStrengthText;
    @FXML private Button createAccountButton;
    @FXML private Label errorLabel;

    public static String phoneNumber;
    public static String country;
    private int currentPasswordStrength = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind the visible text field to the password field
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Add a listener to the checkbox to toggle visibility
        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
            } else {
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
            }
        });

        // Add a listener to the password field to check strength in real-time
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentPasswordStrength = calculatePasswordStrength(newValue);
            updatePasswordStrengthText(passwordStrengthText, currentPasswordStrength);
        });
    }

    @FXML
    void createAccountClicked(ActionEvent event) {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (firstName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("First Name, Username, and Password are required.");
            return;
        }


        if (currentPasswordStrength < 3) {
            errorLabel.setText("Password is too weak. Please choose a stronger one.");
            return;
        }
        if (password.length() < 6)
        {
            errorLabel.setText("password should be minimum 6 characters");
            return;
        }

        // --- HASH THE PASSWORD ---
        String hashedPassword = PasswordUtils.hashPassword(password);

        boolean success = DatabaseManager.createUser(firstName, lastName, username, country, phoneNumber, hashedPassword);

        if (success) {
            LogInController.userName = phoneNumber;
            loadScene("../Views/home_view.fxml");
        } else {
            errorLabel.setText("Could not create account. Username may be taken.");
        }
    }


    private int calculatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength++;
        return strength;
    }


    private void updatePasswordStrengthText(Text text, int strength) {
        String[] descriptions = {"Very Weak", "Weak", "Moderate", "Strong", "Very Strong", "Excellent"};
        Color[] colors = {Color.RED, Color.ORANGE, Color.GOLD, Color.YELLOWGREEN, Color.GREEN, Color.DARKCYAN};

        int index = Math.min(strength, descriptions.length - 1);
        if (passwordField.getText().isEmpty()) {
            text.setText("Password Strength:");
            text.setFill(Color.web("#636d78"));
        } else {
            text.setText("Password Strength: " + descriptions[index]);
            text.setFill(colors[index]);
        }
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) createAccountButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error: Could not load the application.");
        }
    }
}
