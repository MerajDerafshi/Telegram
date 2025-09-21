package ToolBox;

import java.io.Serializable;

public class TextMessage implements Serializable {
    public String content;
    public String sender;
    public String receiver;
    public String timestamp;

    public TextMessage(String content, String sender, String receiver, String timestamp) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
    }
}
