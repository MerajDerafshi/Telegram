package Controllers;

import Models.UserViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class AddUserCellController extends ListCell<UserViewModel> {

    @FXML private Pane parent;
    @FXML private ImageView avatarImageView;
    @FXML private Label userNameLabel;

    public AddUserCellController() {

    }

    @Override
    protected void updateItem(UserViewModel item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../Views/addUserCell.fxml"));
            fxmlLoader.setController(this);
            try {
                fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            userNameLabel.setText(item.getFirstName());
            if(item.getAvatarImage() != null){
                avatarImageView.setImage(item.getAvatarImage());
                Circle clip = new Circle(20, 20, 20); // Clip to a circle
                avatarImageView.setClip(clip);
            }

            // Change background based on selection state
            if (isSelected()) {
                parent.setStyle("-fx-background-color: #2B5278;"); // Selected color
            } else {
                parent.setStyle("-fx-background-color: #17212B;"); // Default color
            }

            setGraphic(parent);
        }
    }
}

