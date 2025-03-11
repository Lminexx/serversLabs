package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class Message implements Serializable {
    private MessageType type;
    private String data;


    public Message(MessageType type) {
        this.type = type;
    }
}
