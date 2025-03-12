package org.example;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
//        Scanner console = new Scanner(System.in);
//        String ip = console.nextLine();
//        console.close();


        Client client = new Client("127.0.0.1");
        client.start();
        Thread.sleep(2000);

        client.downloadFile("fafa.jpg");


        while (true){}

    }
}