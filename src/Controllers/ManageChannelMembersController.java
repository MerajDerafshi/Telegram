package Controllers;

import Models.UserViewModel;
import ToolBox.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.List;

public class ManageChannelMembersController {

    @FXML private ListView<UserViewModel> membersListView;
    @FXML private Button doneButton;

    private UserViewModel channelViewModel;
    private ObservableList<UserViewModel> membersList;

    public void initData(UserViewModel channelViewModel) {
        this.channelViewModel = channelViewModel;
        loadMembers();
    }

    private void loadMembers() {
        membersList = FXCollections.observableArrayList();
        List<DatabaseManager.ChannelMember> dbMembers = DatabaseManager.getChannelMembers(channelViewModel.channelId);

        for (DatabaseManager.ChannelMember member : dbMembers) {
            Image avatar = new Image("resources/img/smile.png");
            if (member.avatar != null && member.avatar.length > 0) {
                avatar = new Image(new ByteArrayInputStream(member.avatar));
            }
            UserViewModel uvm = new UserViewModel(member.firstName, member.username, member.phone, avatar);
            uvm.userId = member.id;
            uvm.role = member.role;
            membersList.add(uvm);
        }

        membersListView.setItems(membersList);
        membersListView.setCellFactory(param -> {
            ManageUserCellController cell = new ManageUserCellController();
            cell.promoteCallback = this::promoteMember;
            cell.removeCallback = this::removeMember;
            return cell;
        });
    }

    private void promoteMember(UserViewModel user) {
        boolean success = DatabaseManager.promoteUserToAdmin(channelViewModel.channelId, user.userId);
        if (success) {
            loadMembers(); // Refresh the list to show the new role
        }
    }

    private void removeMember(UserViewModel user) {
        boolean success = DatabaseManager.removeUserFromChannel(channelViewModel.channelId, user.userId);
        if (success) {
            loadMembers(); // Refresh the list
        }
    }

    @FXML
    void done(ActionEvent event) {
        Stage stage = (Stage) doneButton.getScene().getWindow();
        stage.close();
    }
}
