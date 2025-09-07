package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.ChannelMessage;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

public class ChannelCreatorViewController {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Label chatRoomNameLabel;
    @FXML private TextField messageField;
    @FXML private ListView<MessageViewModel> messagesListView;
    @FXML private Button logoutButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button profileButton;

    private UserViewModel currentChannel;
    private UserViewModel localUser;
    private ObservableList<UserViewModel> allUsersAndChannelsList;
    private NetworkConnection connection;
    private long localUserId;
    private long channelId;

    public void initData(UserViewModel selectedChannel, UserViewModel localUser, ObservableList<UserViewModel> allUsersAndChannels, NetworkConnection connection) {
        this.currentChannel = selectedChannel;
        this.localUser = localUser;
        this.allUsersAndChannelsList = allUsersAndChannels;
        this.connection = connection;

        if (this.connection != null) {
            this.connection.receiveCallback = this::handleIncomingData;
        }

        DatabaseManager.getUserId(localUser.getPhone()).ifPresent(id -> this.localUserId = id);
        this.channelId = currentChannel.getChannelId();

        usersListView.setItems(allUsersAndChannelsList);
        usersListView.setCellFactory(param -> new UserCustomCellController());

        chatRoomNameLabel.setText(selectedChannel.getFirstName());

        loadMessageHistory();

        messagesListView.setCellFactory(param -> {
            MessageCustomCellController cell = new MessageCustomCellController();
            cell.prefWidthProperty().bind(messagesListView.widthProperty());
            // No delete callback for channels
            return cell;
        });

        scrollToBottom();
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadChannelMessages(channelId);
        currentChannel.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for(DatabaseManager.Message msg : history) {
            // For channels, all messages from the creator are outgoing.
            String time = timeFormat.format(msg.createdAt);
            currentChannel.messagesList.add(new MessageViewModel(msg.id, msg.content, time, true));
        }
        messagesListView.setItems(currentChannel.messagesList);
    }

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> {
            if (data instanceof ChannelMessage) {
                ChannelMessage channelMsg = (ChannelMessage) data;
                if (channelMsg.channelId == this.channelId) {
                    // It's for this channel, but since the creator sent it, it's already displayed.
                    // This is mainly for members. But we might refresh just in case.
                    loadMessageHistory();
                    scrollToBottom();
                }
            }
        });
    }

    @FXML
    void sendMessageOnEnter(ActionEvent event) {
        executeSendMessage();
    }

    @FXML
    void sendMessageIconClicked(MouseEvent event) {
        executeSendMessage();
    }

    private void executeSendMessage() {
        String message = messageField.getText();
        if (message == null || message.trim().isEmpty()) return;

        try {
            DatabaseManager.saveChannelMessage(channelId, localUserId, message);
            loadMessageHistory();
            scrollToBottom();

            ChannelMessage channelMessage = new ChannelMessage(channelId, message);
            connection.sendData(channelMessage);

            messageField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scrollToBottom() {
        if (currentChannel != null && !currentChannel.messagesList.isEmpty()) {
            messagesListView.scrollTo(currentChannel.messagesList.size() - 1);
        }
    }

    // Placeholder navigation methods, assuming they are similar to UserChatController
    @FXML void openProfile(MouseEvent event) { /* similar to UserChatController */ }
    @FXML void openSavedMessages(MouseEvent event) { /* similar to UserChatController */ }
    @FXML void logoutClicked(ActionEvent event) { /* similar to UserChatController */ }
}
