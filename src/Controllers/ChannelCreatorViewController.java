package Controllers;

import Models.MessageViewModel;
import Models.UserViewModel;
import ToolBox.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import static ToolBox.Utilities.getCurrentTime;

public class ChannelCreatorViewController {

    @FXML private Label channelNameLabel;
    @FXML private ListView<MessageViewModel> messagesListView;
    @FXML private TextField messageField;
    @FXML private ListView<UserViewModel> usersListView;
    @FXML private Button profileButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button newChannelButton;
    @FXML private Button logoutButton;
    @FXML private Button channelInfoButton;
    @FXML private Button voiceRecordButton;
    @FXML private Button manageMembersButton;
    @FXML private TextField userSearchField;
    @FXML private TextField messageSearchField;

    private UserViewModel localUser;
    private UserViewModel channelViewModel;
    private ObservableList<UserViewModel> allUsersList;
    private NetworkConnection connection;

    private VoiceRecorder voiceRecorder;
    private boolean isRecording = false;

    public void initData(UserViewModel channelViewModel, UserViewModel localUser, ObservableList<UserViewModel> allUsers, NetworkConnection connection) {
        this.channelViewModel = channelViewModel;
        this.localUser = localUser;
        this.allUsersList = allUsers;
        this.connection = connection;
        this.connection.receiveCallback = this::handleIncomingData;

        channelNameLabel.setText(channelViewModel.getFirstName());
        usersListView.setItems(allUsersList);
        usersListView.setCellFactory(param -> new UserCustomCellController());
        messagesListView.setCellFactory(param -> new MessageCustomCellController());
        setupUserSearchFunctionality();
        setupMessageSearchFunctionality();


        // Added the listener to make the chat list on the left functional
        usersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                openConversationView(newValue);
            }
        });


        loadMessageHistory();
    }


    private void setupUserSearchFunctionality() {
        FilteredList<UserViewModel> filteredData = new FilteredList<>(allUsersList, p -> true);
        userSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(userViewModel -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return userViewModel.getFirstName().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<UserViewModel> sortedData = new SortedList<>(filteredData);
        usersListView.setItems(sortedData);
    }

    private void setupMessageSearchFunctionality() {
        FilteredList<MessageViewModel> filteredMessages = new FilteredList<>(channelViewModel.messagesList, p -> true);
        messageSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMessages.setPredicate(messageViewModel -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (messageViewModel.getMessage() != null) {
                    return messageViewModel.getMessage().toLowerCase().contains(lowerCaseFilter);
                }
                return false;
            });
        });
        SortedList<MessageViewModel> sortedMessages = new SortedList<>(filteredMessages);
        messagesListView.setItems(sortedMessages);
    }

    private void openConversationView(UserViewModel selectedItem) {
        try {
            FXMLLoader loader;
            if (selectedItem.isChannel) {
                if (localUser.userId == selectedItem.creatorId) {
                    loader = new FXMLLoader(getClass().getResource("../Views/channelCreatorView.fxml"));
                    Parent root = loader.load();
                    ChannelCreatorViewController controller = loader.getController();
                    controller.initData(selectedItem, localUser, allUsersList, connection);
                    getStage().setScene(new Scene(root));
                } else {
                    loader = new FXMLLoader(getClass().getResource("../Views/channelMemberView.fxml"));
                    Parent root = loader.load();
                    ChannelMemberViewController controller = loader.getController();
                    controller.initData(selectedItem, localUser, allUsersList, connection);
                    getStage().setScene(new Scene(root));
                }
            } else { // It's a user chat
                loader = new FXMLLoader(getClass().getResource("../Views/userChat.fxml"));
                Parent root = loader.load();
                UserChatController controller = loader.getController();
                controller.initData(selectedItem, localUser, allUsersList, connection);
                getStage().setScene(new Scene(root));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleIncomingData(Serializable data) {
        if (data instanceof ChannelMessage) {
            ChannelMessage msg = (ChannelMessage) data;
            if (msg.channelId == channelViewModel.channelId) {
                Platform.runLater(this::loadMessageHistory);
            }
        }
    }

    private void loadMessageHistory() {
        List<DatabaseManager.Message> history = DatabaseManager.loadChannelMessages(channelViewModel.channelId);
        channelViewModel.messagesList.clear();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        for(DatabaseManager.Message msg : history) {
            boolean isOutgoing = msg.senderId == localUser.userId;
            String time = timeFormat.format(msg.createdAt);

            if (msg.mediaData != null && msg.mimeType != null) {
                if (msg.mimeType.startsWith("image/")) {
                    Image image = new Image(new ByteArrayInputStream(msg.mediaData));
                    channelViewModel.messagesList.add(new MessageViewModel(msg.id, "📷 image", time, isOutgoing, image));
                } else {
                    channelViewModel.messagesList.add(new MessageViewModel(msg.id, msg.fileName, msg.mediaData, time, isOutgoing));
                }
            } else if (msg.content != null){
                channelViewModel.messagesList.add(new MessageViewModel(msg.id, msg.content, time, isOutgoing));
            }
        }
        messagesListView.setItems(channelViewModel.messagesList);
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
        String messageContent = messageField.getText();
        if (messageContent == null || messageContent.trim().isEmpty()) return;

        long messageId = DatabaseManager.saveChannelMessage(channelViewModel.channelId, localUser.userId, messageContent, null);
        if (messageId != -1) {
            MessageViewModel sentMessage = new MessageViewModel(messageId, messageContent, getCurrentTime(), true);
            channelViewModel.messagesList.add(sentMessage);
            messagesListView.refresh();
            messagesListView.scrollTo(channelViewModel.messagesList.size() - 1);

            try {
                ChannelMessage channelMessage = new ChannelMessage(messageId, localUser.userId, messageContent, null, channelViewModel.channelId, getCurrentTime());
                connection.sendData(channelMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            messageField.clear();
        }
    }

    @FXML
    void attachFile(MouseEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Send");
            File file = fileChooser.showOpenDialog(getStage());
            if (file == null) return;

            String fileName = file.getName();
            byte[] fileBytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileBytes);
            }

            String mimeType = getMimeType(fileName);

            Optional<Long> mediaIdOpt = DatabaseManager.saveMediaAndGetId(localUser.userId, fileBytes, fileName, mimeType);
            if(mediaIdOpt.isEmpty()) {
                System.err.println("Failed to save media to database.");
                return;
            }
            long mediaId = mediaIdOpt.get();
            long messageId = DatabaseManager.saveChannelMessage(channelViewModel.channelId, localUser.userId, null, mediaId);

            if(messageId != -1) {
                loadMessageHistory();
                ChannelMessage channelMessage = new ChannelMessage(messageId, localUser.userId, fileName, fileBytes, channelViewModel.channelId, getCurrentTime());
                connection.sendData(channelMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void toggleVoiceRecording(MouseEvent event) {
        if (!isRecording) {
            voiceRecorder = new VoiceRecorder();
            voiceRecorder.startRecording();
            isRecording = true;
            System.out.println("Started recording...");
        } else {
            byte[] voiceData = voiceRecorder.stopRecording();
            isRecording = false;
            System.out.println("Stopped recording, sending voice message...");
            sendVoiceMessage(voiceData);
        }
    }

    @FXML
    void openManageMembers(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/manageChannelMembers.fxml"));
            Parent root = loader.load();
            ManageChannelMembersController controller = loader.getController();
            controller.initData(channelViewModel);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle("Manage Members");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendVoiceMessage(byte[] voiceData) {
        if (voiceData == null || voiceData.length == 0) return;

        try {
            String fileName = "voice_message_" + System.currentTimeMillis() + ".wav";
            String mimeType = "audio/wav";

            Optional<Long> mediaIdOpt = DatabaseManager.saveMediaAndGetId(localUser.userId, voiceData, fileName, mimeType);
            if(mediaIdOpt.isEmpty()) {
                System.err.println("Failed to save voice message to database.");
                return;
            }
            long mediaId = mediaIdOpt.get();
            long messageId = DatabaseManager.saveChannelMessage(channelViewModel.channelId, localUser.userId, null, mediaId);

            if(messageId != -1) {
                loadMessageHistory();
                ChannelMessage channelMessage = new ChannelMessage(messageId, localUser.userId, fileName, voiceData, channelViewModel.channelId, getCurrentTime());
                connection.sendData(channelMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMimeType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }

        switch (extension) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "mp4": return "video/mp4";
            default: return "application/octet-stream";
        }
    }

    @FXML
    void openChannelInfo(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/channelInfo.fxml"));
            Parent root = loader.load();
            ChannelInfoController controller = loader.getController();
            controller.initData(channelViewModel, localUser, allUsersList, connection);
            getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/Profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.initData(localUser, allUsersList, connection);
            getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openSavedMessages(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/saveMessageChat.fxml"));
            Parent root = loader.load();
            SavedMessagesController controller = loader.getController();
            controller.initData(localUser, allUsersList, connection);
            getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void openNewChannel(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../Views/createChannel1.fxml"));
            Parent root = loader.load();
            CreateChannelController1 controller = loader.getController();
            controller.initData(localUser, allUsersList);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void logoutClicked(ActionEvent event) {
        try {
            if (connection != null) {
                connection.closeConnection();
            }
            Parent root = FXMLLoader.load(getClass().getResource("../Views/loginStarter.fxml"));
            getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        return (Stage) usersListView.getScene().getWindow();
    }
}

