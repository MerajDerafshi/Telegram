package Controllers;

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
        primaryStage.getIcons().add(new Image("resources/img/app.png"));
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("../Views/loginStarter.fxml")));

        primaryStage.setScene(new Scene(root));
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setTitle("Telegram");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
