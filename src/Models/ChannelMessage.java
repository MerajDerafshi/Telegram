package ToolBox;

import java.io.Serializable;

public class ChannelMessage implements Serializable {
    public final long channelId;
    public final String content;

    public ChannelMessage(long channelId, String content) {
        this.channelId = channelId;
        this.content = content;
    }
}
