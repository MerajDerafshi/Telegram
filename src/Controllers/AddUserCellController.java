package Controllers;

import Models.UserViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
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
    @FXML private CheckBox selectCheckBox;

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
            if (item.getAvatarImage() != null) {
                avatarImageView.setImage(item.getAvatarImage());
                Circle clip = new Circle(22.5, 22.5, 22.5);
                avatarImageView.setClip(clip);
            }

            selectCheckBox.selectedProperty().bindBidirectional(item.selectedProperty());

            parent.setOnMouseClicked(event -> {
                item.setSelected(!item.isSelected());
            });

            setGraphic(parent);
        }
    }
}

