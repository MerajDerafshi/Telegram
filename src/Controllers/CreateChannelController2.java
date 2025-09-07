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
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CreateChannelController2 implements Initializable {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private String channelName;
    private byte[] channelAvatar;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> userList;


    public void initData(String channelName, byte[] channelAvatar, UserViewModel localUser, ObservableList<UserViewModel> allUsers) {
        this.channelName = channelName;
        this.channelAvatar = channelAvatar;
        this.localUser = localUser;

        this.userList = allUsers.stream()
                .filter(u -> !u.isChannel)
                .peek(u -> u.setSelected(false))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        usersListView.setItems(userList);
        usersListView.setCellFactory(param -> new AddUserCellController());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    void createChannel(ActionEvent event) {
        List<UserViewModel> selectedUsers = userList.stream()
                .filter(UserViewModel::isSelected)
                .collect(Collectors.toList());

        if (selectedUsers.isEmpty()) {
            System.out.println("No users selected for the channel.");
            return;
        }

        List<Long> memberIds = selectedUsers.stream()
                .map(user -> user.userId)
                .collect(Collectors.toList());

        long channelId = DatabaseManager.createChannel(channelName, channelAvatar, localUser.userId, memberIds);

        if (channelId != -1) {
            System.out.println("Channel created with ID: " + channelId);
            closeWindow();
        } else {
            System.err.println("Failed to create channel.");
        }
    }

    @FXML
    void cancelCreation(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}

