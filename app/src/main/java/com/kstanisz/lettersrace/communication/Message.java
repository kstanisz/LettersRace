package com.kstanisz.lettersrace.communication;

import java.io.Serializable;

public class Message implements Serializable {
    private String ownerId;
    private MessageType type;

    public Message(String ownerId, MessageType type) {
        this.ownerId = ownerId;
        this.type = type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public MessageType getType() {
        return type;
    }
}
