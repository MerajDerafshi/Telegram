package Models;

import javafx.scene.image.Image;

public class MessageViewModel {
    public long messageId; // Unique ID from the database
    public Image image;
    public String message;
    public String time;
    public boolean isOutgoing;
    public boolean isImage;
    public boolean isFile;
    public byte[] fileData;
    public String fileName;


    public MessageViewModel(long messageId, String message, String time, boolean isOutgoing) {
        this.messageId = messageId;
        this.message = message;
        this.time = time;
        this.isOutgoing = isOutgoing;
        this.isImage = false;
        this.isFile = false;
    }


    public MessageViewModel(long messageId, String message, String time, boolean isOutgoing, Image image) {
        this.messageId = messageId;
        this.message = message;
        this.time = time;
        this.isOutgoing = isOutgoing;
        this.isImage = true;
        this.image = image;
        this.isFile = false;
    }


    public MessageViewModel(long messageId, String fileName, byte[] fileData, String time, boolean isOutgoing) {
        this.messageId = messageId;
        this.fileName = fileName;
        this.fileData = fileData;
        this.message = "📄 " + fileName;
        this.time = time;
        this.isOutgoing = isOutgoing;
        this.isImage = false;
        this.isFile = true;
    }


    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public Image getImage() {
        return image;
    }
}
