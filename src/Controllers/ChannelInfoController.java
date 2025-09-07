package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ChannelInfoController {

    @FXML private ImageView avatarImageView;
    @FXML private Label channelNameLabel;
    @FXML private Label bioLabel;
    @FXML private Label channelLinklabel;
    @FXML private Button leaveChannelButton;
    @FXML private Button closeButton;
    @FXML private ListView<UserViewModel> usersListView;

    private UserViewModel localUser;
    private UserViewModel channelViewModel;
    private ObservableList<UserViewModel> allUsersList;
    private NetworkConnection connection;

    public void initData(UserViewModel channelViewModel, UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.channelViewModel = channelViewModel;
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;

        channelNameLabel.setText(channelViewModel.getFirstName());
        if (channelViewModel.getAvatarImage() != null) {
            avatarImageView.setImage(channelViewModel.getAvatarImage());
        }
        // These are placeholders as requested
        bioLabel.setText("You can post messages here.");
        channelLinklabel.setText("@" + channelViewModel.getFirstName().replaceAll("\\s+", ""));

        usersListView.setItems(allUsers);
        usersListView.setCellFactory(param -> new UserCustomCellController());
    }

    @FXML
    void leaveChannel(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Leave this channel?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Are you sure you want to leave " + channelViewModel.getFirstName() + "?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            // CORRECTED: Passing userId instead of phone number
            boolean success = DatabaseManager.leaveChannel(channelViewModel.channelId, localUser.userId);
            if (success) {
                goHome();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to leave the channel.").show();
            }
        }
    }

    @FXML
    void closeInfo(ActionEvent event) {
        try {
            FXMLLoader loader;
            if (localUser.userId == channelViewModel.creatorId) {
                loader = new FXMLLoader(getClass().getResource("../Views/channelCreatorView.fxml"));
                Parent root = loader.load();
                ChannelCreatorViewController controller = loader.getController();
                controller.initData(channelViewModel, localUser, allUsersList, connection);
                Stage stage = (Stage) closeButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                loader = new FXMLLoader(getClass().getResource("../Views/channelMemberView.fxml"));
                Parent root = loader.load();
                ChannelMemberViewController controller = loader.getController();
                controller.initData(channelViewModel, localUser, allUsersList, connection);
                Stage stage = (Stage) closeButton.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void goHome() {
        try {
            Stage stage = (Stage) leaveChannelButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/homeView.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

