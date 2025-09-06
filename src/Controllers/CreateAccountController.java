package Controllers;

import ToolBox.DatabaseManager;
import ToolBox.PasswordUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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
    @FXML private TextField visiblePasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Text passwordStrengthText;
    @FXML private Button createAccountButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    public static String phoneNumber;
    public static String country;
    private int currentPasswordStrength = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleConfirmPasswordField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            togglePasswordVisibility(isNowSelected);
        });

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentPasswordStrength = calculatePasswordStrength(newValue);
            updatePasswordStrengthText(passwordStrengthText, currentPasswordStrength);
        });
    }

    private void togglePasswordVisibility(boolean isVisible) {
        visiblePasswordField.setVisible(isVisible);
        visiblePasswordField.setManaged(isVisible);
        passwordField.setVisible(!isVisible);
        passwordField.setManaged(!isVisible);

        visibleConfirmPasswordField.setVisible(isVisible);
        visibleConfirmPasswordField.setManaged(isVisible);
        confirmPasswordField.setVisible(!isVisible);
        confirmPasswordField.setManaged(!isVisible);
    }

    @FXML
    void backButtonClicked(ActionEvent event) {
        loadScene("../Views/login.fxml", event);
    }

    @FXML
    void createAccountClicked(ActionEvent event) {
        errorLabel.setText("");
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("First Name, Username, and Password are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match. Please try again.");
            return;
        }

        if (currentPasswordStrength < 2) {
            errorLabel.setText("Password is too weak. Please choose a stronger one.");
            return;
        }

        String hashedPassword = PasswordUtils.hashPassword(password);
        boolean success = DatabaseManager.createUser(firstName, lastName, username, country, phoneNumber, hashedPassword);

        if (success) {
            LogInController.userName = phoneNumber;
            loadScene("../Views/homeView.fxml", event);
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

    private void loadScene(String fxmlFile, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error: Could not load the application.");
        }
    }
}

