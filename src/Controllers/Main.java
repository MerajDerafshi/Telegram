package Controllers;

import ToolBox.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/resources/img/app.png"))));
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("../Views/loginStarter.fxml")));

        Scene scene = new Scene(root);
        // Apply the default (dark) theme immediately on startup.
        ThemeManager.applyTheme(scene);

        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setTitle("Telegram");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

