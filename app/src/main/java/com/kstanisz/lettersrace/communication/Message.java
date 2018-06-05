package com.kstanisz.lettersrace.communication;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;
    private Integer value;

    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
