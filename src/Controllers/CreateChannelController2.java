package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CreateChannelController2 implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private String channelName;
    private byte[] channelAvatarData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        usersListView.setCellFactory(param -> new UserCustomCellController());
    }

    public void initData(String channelName, byte[] channelAvatarData) {
        this.channelName = channelName;
        this.channelAvatarData = channelAvatarData;
        loadUsers();
    }

    private void loadUsers() {
        ObservableList<UserViewModel> allUsers = FXCollections.observableArrayList(
                DatabaseManager.getAllUsers("").stream()
                        .map(dbUser -> {
                            Image avatar = new Image("resources/img/smile.png"); // Default
                            if (dbUser.avatar != null && dbUser.avatar.length > 0) {
                                avatar = new Image(new ByteArrayInputStream(dbUser.avatar));
                            }
                            return new UserViewModel(dbUser.firstName, dbUser.username, dbUser.phone, "", "", "0", avatar);
                        })
                        .collect(Collectors.toList())
        );
        usersListView.setItems(allUsers);
    }

    @FXML
    void createClicked(ActionEvent event) {
        ObservableList<UserViewModel> selectedUsers = usersListView.getSelectionModel().getSelectedItems();
        List<String> memberPhones = selectedUsers.stream().map(UserViewModel::getPhone).collect(Collectors.toList());

        String creatorPhone = LogInController.userName;
        if (!memberPhones.contains(creatorPhone)) {
            memberPhones.add(creatorPhone);
        }

        boolean success = DatabaseManager.createChannel(channelName, creatorPhone, memberPhones, channelAvatarData);

        if (success) {
            System.out.println("Channel created successfully!");
            cancelClicked(event);
        } else {
            System.out.println("Failed to create channel.");
        }
    }

    @FXML
    void cancelClicked(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}

