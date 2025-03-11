package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        Client client = new Client("127.0.0.1");
        client.start();
        Thread.sleep(2000);

        client.send(new Message(MessageType.SEND, "похуй"));
        client.sendFile("lminexx.jpg");


        while (true){}

    }
}