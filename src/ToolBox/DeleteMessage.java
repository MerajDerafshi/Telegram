package ToolBox;

import java.io.Serializable;

public class DeleteMessage implements Serializable {
    public final long messageId;
    public final String senderPhone;
    public final String receiverPhone;

    public DeleteMessage(long messageId, String senderPhone, String receiverPhone) {
        this.messageId = messageId;
        this.senderPhone = senderPhone;
        this.receiverPhone = receiverPhone;
    }
}
