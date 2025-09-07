package Models;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;


public class UserViewModel {
    public String firstName; // For users, this is their name. For channels, it's the channel title.
    public String username; // The unique username
    public String phone;
    public SimpleStringProperty lastMessage;
    public SimpleStringProperty time;
    public SimpleStringProperty notificationsNumber;
    public Image avatarImage;
    public ObservableList<MessageViewModel> messagesList;

    // -- Channel Specific Fields --
    private boolean isChannel = false;
    private long channelId = -1;
    private boolean isCreator = false;


    /**
     * Constructor for users displayed in the list.
     */
    public UserViewModel(String firstName, String username, String phone, String lastMessage, String time, String notificationsNumber, Image avatarImage) {
        this.firstName = firstName;
        this.username = username;
        this.phone = phone;
        this.lastMessage = new SimpleStringProperty(lastMessage);
        this.time = new SimpleStringProperty(time);
        this.notificationsNumber = new SimpleStringProperty(notificationsNumber);
        this.avatarImage = avatarImage;
        messagesList = FXCollections.observableArrayList();
    }

    /**
     * Simplified constructor for the local user object.
     */
    public UserViewModel(String firstName, String username, String phone, Image avatarImage) {
        this.firstName = firstName;
        this.username = username;
        this.phone = phone;
        this.lastMessage = new SimpleStringProperty("");
        this.time = new SimpleStringProperty("");
        this.notificationsNumber = new SimpleStringProperty("0");
        this.avatarImage = avatarImage;
        messagesList = FXCollections.observableArrayList();
    }

    /**
     * Constructor for Channels.
     */
    public UserViewModel(long channelId, String channelName, String lastMessage, String time, Image avatarImage, boolean isCreator) {
        this.channelId = channelId;
        this.firstName = channelName; // Re-using firstName for channel title
        this.lastMessage = new SimpleStringProperty(lastMessage);
        this.time = new SimpleStringProperty(time);
        this.avatarImage = avatarImage;
        this.isChannel = true;
        this.isCreator = isCreator;
        this.messagesList = FXCollections.observableArrayList();
        // Other fields can be null or default for channels
        this.username = "channel";
        this.phone = String.valueOf(channelId);
        this.notificationsNumber = new SimpleStringProperty("0");
    }


    public String getFirstName() {
        return firstName;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public String getLastMessage() {
        return lastMessage.get();
    }

    public SimpleStringProperty lastMessageProperty() {
        return lastMessage;
    }

    public SimpleStringProperty timeProperty() {
        return time;
    }

    public String getNotificationsNumber() {
        return notificationsNumber.get();
    }

    public SimpleStringProperty notificationsNumberProperty() {
        return notificationsNumber;
    }

    public Image getAvatarImage() {
        return avatarImage;
    }

    // -- Channel Specific Getters --
    public boolean isChannel() { return isChannel; }
    public long getChannelId() { return channelId; }
    public boolean isCreator() { return isCreator; }

}
