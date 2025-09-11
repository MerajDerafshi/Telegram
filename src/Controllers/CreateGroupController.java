package Controllers;


import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.ImageCropper;


import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupController {


    @FXML private TextField groupNameField;
    @FXML private ImageView avatarView;
    @FXML private Button chooseImageButton;
    @FXML private Button removeImageButton;
    @FXML private Button nextButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabel;


    private UserViewModel localUser;
    private byte[] avatarBytes;

    private List<Long> selectedMemberIds = new ArrayList<>();


    public void initData(UserViewModel localUser) {this.localUser = localUser;
    }


    @FXML
    private void chooseImage(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose group picture");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File f = fc.showOpenDialog(getStage());
        if (f != null) {
            try {
                Image img = new Image(f.toURI().toString());
                BufferedImage buffered = SwingFXUtils.fromFXImage(img, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(buffered, "png", baos);
                this.avatarBytes = baos.toByteArray();
                this.avatarView.setImage(img);
            } catch (IOException ex) {
                showError("Could not read image.");
            }
        }
    }
    @FXML
    private void removeImage(ActionEvent e) {
        avatarView.setImage(null);
        avatarBytes = null;
    }


    @FXML
    private void cancel(ActionEvent e) {
        close();
    }


    @FXML
    private void createGroup(ActionEvent e) {
        String title = groupNameField.getText() == null ? "" : groupNameField.getText().trim();
        if (title.isEmpty()) {
            showError("Please enter a group name.");
            return;
        }
        if (localUser == null) {
            showError("No current user.");
            return;
        }
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(localUser.creatorId);
        memberIds.addAll(selectedMemberIds);


        long newConversationId = DatabaseManager.createGroup(title, avatarBytes, localUser.creatorId
                , memberIds);
        if (newConversationId <= 0) {
            showError("Failed to create group.");
            return;
        }

        close();
    }


    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }


    private Stage getStage() { return (Stage) nextButton.getScene().getWindow(); }
    private void close() { getStage().close(); }
}