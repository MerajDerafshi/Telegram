package Controllers;

import ToolBox.DatabaseManager;
import ToolBox.ThemeManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginStarterController implements Initializable {

    @FXML private ChoiceBox<String> countryComboBox;
    @FXML private TextField countryCodeField;
    @FXML private TextField phoneNumberField;
    @FXML private Button nextButton;
    @FXML private Label errorLabel;

    private final Map<String, String> countryCodes = new HashMap<>();

    public static String fullPhoneNumber;
    public static String country;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        countryCodes.put("Iran", "+98");
        countryCodes.put("USA", "+1");
        countryCodes.put("UK", "+44");
        countryCodes.put("France", "+33");

        countryComboBox.setItems(FXCollections.observableArrayList(countryCodes.keySet()));

        countryComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            countryCodeField.setText(countryCodes.get(newValue));
        });

        countryComboBox.getSelectionModel().select("Iran");
    }

    @FXML
    void nextClicked(ActionEvent event) {
        String countryCode = countryCodeField.getText();
        String phone = phoneNumberField.getText().trim();
        country = countryComboBox.getValue();

        if (countryCode.isEmpty() || country == null) {
            errorLabel.setText("Please select a country.");
            return;
        }
        if (phone.isEmpty() || !phone.matches("\\d{10}")) {
            errorLabel.setText("Please enter a valid 10-digit phone number.");
            return;
        }

        fullPhoneNumber = countryCode + phone;

        PasswordLoginController.phoneNumber = fullPhoneNumber;
        CreateAccountController.phoneNumber = fullPhoneNumber;
        CreateAccountController.country = country;

        boolean userExists = DatabaseManager.userExists(fullPhoneNumber);

        if (userExists) {
            loadScene("../Views/passwordLogin.fxml");
        } else {
            loadScene("../Views/createAccount.fxml");
        }
    }

    private void loadScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) nextButton.getScene().getWindow();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene); // Apply theme to the newly created scene
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error: Could not load the next screen.");
        }
    }
}

