package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.ChannelMessage;
import ToolBox.DatabaseManager;
import ToolBox.NetworkConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

public class ChannelMemberViewController {

    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Label chatRoomNameLabel;
    @FXML private ListView<MessageViewModel> messagesListView;

    private UserViewModel currentChannel;
    private ObservableList<UserViewModel> allUsersAndChannelsList;
    private NetworkConnection connection;
    private long channelId;

    public void initData(UserViewModel selectedChannel, ObservableList<UserViewModel> allUsersAndChannels, NetworkConnection connection) {
        this.currentChannel = selectedChannel;
        this.allUsersAndChannelsList = allUsersAndChannels;
        this.connection = connection;
        this.channelId = selectedChannel.getChannelId();

        if (this.connection != null) {
            this.connection.receiveCallback = this::handleIncomingData;
        }

        usersListView.setItems(allUsersAndChannelsList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
        chatRoomNameLabel.setText(selectedChannel.getFirstName());

        loadMessageHistory();

        messagesListView.setCellFactory(param -> {
            MessageCustomCellController cell = new MessageCustomCellController();
            cell.prefWidthProperty().bind(messagesListView.widthProperty());
            return cell;
        });

        scrollToBottom();
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadChannelMessages(channelId);
        currentChannel.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for (DatabaseManager.Message msg : history) {
            // In the member view, all messages are treated as incoming.
            String time = timeFormat.format(msg.createdAt);
            currentChannel.messagesList.add(new MessageViewModel(msg.id, msg.content, time, false));
        }
        messagesListView.setItems(currentChannel.messagesList);
    }

    private void handleIncomingData(Serializable data) {
        Platform.runLater(() -> {
            if (data instanceof ChannelMessage) {
                ChannelMessage channelMsg = (ChannelMessage) data;
                if (channelMsg.channelId == this.channelId) {
                    loadMessageHistory();
                    scrollToBottom();
                }
            }
        });
    }

    private void scrollToBottom() {
        if (currentChannel != null && !currentChannel.messagesList.isEmpty()) {
            messagesListView.scrollTo(currentChannel.messagesList.size() - 1);
        }
    }
}
