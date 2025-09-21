package Controllers;

import Models.UserViewModel;
import ToolBox.ImageCropper;
import javafx.collections.ObservableList;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class CreateChannelController1 {

    @FXML private ImageView channelAvatarImageView;
    @FXML private TextField channelNameField;
    @FXML private Button nextButton;
    @FXML private Button cancelButton;
    @FXML private Button chooseAvatarButton;


    private byte[] avatarData;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersList;

    public void initData(UserViewModel localUser, ObservableList<UserViewModel> allUsers) {
        this.localUser = localUser;
        this.allUsersList = allUsers;
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
                channelAvatarImageView.setImage(fxImage);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(croppedImage, "png", baos);
                avatarData = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void nextButtonClicked(ActionEvent event) {
        String channelName = channelNameField.getText();
        if (channelName == null || channelName.trim().isEmpty()) {
            System.err.println("Channel name cannot be empty.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createChannel_group2.fxml"));
            Parent root = loader.load();

            CreateChannelController2 controller = loader.getController();
            controller.initData(channelName, avatarData, localUser, allUsersList);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Add Members");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            closeWindow();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void cancelButtonClicked(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}

