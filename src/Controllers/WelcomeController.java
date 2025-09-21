package Controllers;

import ToolBox.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    @FXML private VBox parent;
    @FXML private Button buttonMode;
    @FXML private SVGPath svgMode;
    @FXML private ImageView headerImage;

    private final String sunIcon = "M12 7a5 5 0 1 0 5 5 5 5 0 0 0-5-5zM12 9a3 3 0 1 1-3 3 3 3 0 0 1 3-3z";
    private final String moonIcon = "M2.03009 12.42C2.39009 17.57 6.76009 21.76 11.9901 21.99C15.6801 22.15 18.9801 20.43 20.9601 17.72C21.7801 16.61 21.3401 15.87 19.9701 16.12C19.3001 16.24 18.6101 16.29 17.8901 16.26C13.0001 16.06 9.00009 11.97 8.98009 7.13996C8.97009 5.83996 9.24009 4.60996 9.73009 3.48996C10.2701 2.24996 9.62009 1.65996 8.37009 2.18996C4.41009 3.85996 1.70009 7.84996 2.03009 12.42Z";

    // Corrected to use absolute paths from the classpath root.
    private final Image lightHeader = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/resources/img/loginStarter.jpg")));
    private final Image darkHeader = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/resources/img/loginStarter.jpg")));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateThemeVisuals(); // Set initial icon/image based on default theme
        buttonMode.setOnAction(event -> toggleTheme());
    }

    private void toggleTheme() {
        ThemeManager.isLightMode = !ThemeManager.isLightMode;
        ThemeManager.applyTheme(parent.getScene()); // Apply stylesheet
        updateThemeVisuals(); // Update icon and image
    }

    private void updateThemeVisuals() {
        if (ThemeManager.isLightMode) {
            svgMode.setContent(sunIcon);
            headerImage.setImage(lightHeader);
        } else {
            svgMode.setContent(moonIcon);
            headerImage.setImage(darkHeader);
        }
    }

    @FXML
    void startMessagingClicked(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("../Views/login.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene); // Apply the selected theme to the new scene
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load login.fxml");
        }
    }
}

