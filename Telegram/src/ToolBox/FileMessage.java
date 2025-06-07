package ToolBox;

import java.io.Serializable;

public class FileMessage implements Serializable {
    public byte[] fileData;
    public String fileName;
    public String sender;
    public String receiver;
    public String timestamp;

    public FileMessage(byte[] fileData, String fileName, String sender, String receiver, String timestamp) {
        this.fileData = fileData;
        this.fileName = fileName;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
    }
}
