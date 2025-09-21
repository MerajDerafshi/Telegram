package Controllers;

import Models.UserViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.function.Consumer;

public class ManageUserCellController extends ListCell<UserViewModel> {

    @FXML private Pane parent;
    @FXML private ImageView avatarImageView;
    @FXML private Label userNameLabel;
    @FXML private Label roleLabel;
    @FXML private Button promoteButton;
    @FXML private Button removeButton;

    public Consumer<UserViewModel> promoteCallback;
    public Consumer<UserViewModel> removeCallback;

    @Override
    protected void updateItem(UserViewModel item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../Views/manageUserCell.fxml"));
            fxmlLoader.setController(this);
            try {
                fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            userNameLabel.setText(item.getFirstName());
            roleLabel.setText(item.role);

            if (item.getAvatarImage() != null) {
                avatarImageView.setImage(item.getAvatarImage());
                Circle clip = new Circle(22.5, 22.5, 22.5);
                avatarImageView.setClip(clip);
            }


            if (!"member".equals(item.role)) {
                promoteButton.setVisible(false);
                removeButton.setVisible(false);
            }

            setGraphic(parent);
        }
    }

    @FXML
    void promoteUser(ActionEvent event) {
        if (promoteCallback != null) {
            promoteCallback.accept(getItem());
        }
    }

    @FXML
    void removeUser(ActionEvent event) {
        if (removeCallback != null) {
            removeCallback.accept(getItem());
        }
    }
}

