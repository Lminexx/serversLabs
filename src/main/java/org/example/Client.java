package org.example;

import lombok.Getter;

import java.io.*;
import java.net.Socket;

@Getter
public class Client {
    private Connection connection;
    private String ipAddress;
    private final int PORT = 12345;
    public Client(String ipAddress){
        this.ipAddress = ipAddress;
    }
    public class SocketThread extends Thread {
        @Override
        public void run() {
            try(Socket socket = new Socket(ipAddress, PORT)) {
                Client.this.connection = new Connection(socket);
                clientMainLoop();
            } catch (IOException e){
                System.out.println("Ошибка подключения клиента к серверу");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                System.out.println("Получено сообщение: " + message.getType() + " - " + message.getData());
                switch (message.getType()) {
                    case RECEIVE -> System.out.println("Поступило сообщение клиенту");
                    case HTTP150 -> System.out.println("жду");
                    case HTTP226 -> System.out.println("ура я загрузил файл четко чечетко");
                    case HTTP550 -> System.out.println("я лох");
                }
            }
        }
    }
    public void start() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
    }


    public void sendFile(String name) throws IOException {
        File file = new File(name);
        if (!file.exists()) {
            System.out.println("Файл не найден!");
            return;
        }
        
        connection.send(new Message(MessageType.SEND_FILE, file.getName()));
        
        long fileSize = file.length();
        connection.getOut().writeLong(fileSize);
        connection.getOut().flush();
        
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                connection.getOut().write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                System.out.println("Отправлено " + totalSent + " из " + fileSize + " байт");
            }
            connection.getOut().flush();
        }
    }


    public void send(Message message) throws IOException {
        connection.send(message);
    }


}
