package ToolBox;

import java.io.Serializable;

public class ChannelMessage implements Serializable {
    public long messageId;
    public long senderId;
    public String content;
    public byte[] mediaData;
    public long channelId;
    public String timestamp;

    public ChannelMessage(long messageId, long senderId, String content, byte[] mediaData, long channelId, String timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.mediaData = mediaData;
        this.channelId = channelId;
        this.timestamp = timestamp;
    }
}

