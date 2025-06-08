package Models;

import javafx.scene.image.Image;

public class MessageViewModel {
    Image image;
    String message;
    String time;
    public boolean isOutgoing;
    public boolean isImage;
    public boolean isFile;
    public byte[] fileData;
    public String fileName;

    public MessageViewModel(String message, String time, boolean isOutgoing, boolean isImage, Image image) {
        this.message = message;
        this.time = time;
        this.isOutgoing = isOutgoing;
        this.isImage = isImage;
        this.image = image;
        this.isFile = false;
    }

    public MessageViewModel(String fileName, byte[] fileData, String time, boolean isOutgoing) {
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

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
