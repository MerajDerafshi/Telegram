package Controllers;

import ToolBox.ImageCropper;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class CreateChannelController1 {

    @FXML private ImageView channelAvatarImageView;
    @FXML private Button channelAvatarButton;
    @FXML private TextField channelNameField;
    @FXML private Button nextButton;
    @FXML private Button cancelButton;

    private byte[] channelAvatarData;

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
                channelAvatarImageView.setImage(fxImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(croppedImage, "png", baos);
                channelAvatarData = baos.toByteArray();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void nextClicked(ActionEvent event) {
        String channelName = channelNameField.getText();
        if (channelName.trim().isEmpty()) {
            System.out.println("Channel name cannot be empty.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createChannel_group2.fxml"));
            Parent root = loader.load();

            CreateChannelController2 controller = loader.getController();
            controller.initData(channelName, channelAvatarData);

            Stage stage = (Stage) nextButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void cancelClicked(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}

