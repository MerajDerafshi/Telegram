package ToolBox;

import java.io.Serializable;

public class ImageMessage implements Serializable {
    public byte[] imageData;
    public String sender;
    public String receiver;
    public String timestamp;

    public ImageMessage(byte[] imageData, String sender, String receiver, String timestamp) {
        this.imageData = imageData;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
    }
}
