package org.example;
import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private static final int PORT = 12345;
    private final List<Connection> clients = Collections.synchronizedList(new ArrayList<>());
    public String ipAddress;
    private ServerSocket serverSocket;
    private ObjectOutputStream out;
    private final String rootDirectory = "root_directory";

    public void startServer() throws IOException {
        try {
            serverSocket = new ServerSocket(PORT);
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Сервер запущен на IP: " + ipAddress);
            System.out.println("Ожидаем подключения клиентов...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Новый клиент подключился");
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandler.start();
            }
        } catch (BindException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Connection clientConnection;

        public ClientHandler(Socket socket) throws IOException {
            this.clientConnection = new Connection(socket);
        }
        private void userConnecting(Connection connection) throws IOException {
            clients.add(connection);
        }

        private void serverMainLoop(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                Message gettedMessage = connection.receive();
                switch (gettedMessage.getType()){
                    case SEND -> System.out.println("Hello world \n Поступило сообщение SEND");
                    case SEND_FILE -> saveFileServer(gettedMessage.getData());
                }
            }
        }

        private void saveFileServer(String fileName) throws IOException {
            new File(rootDirectory).mkdir();
            File file = new File(rootDirectory, fileName);
            
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                long fileSize = clientConnection.getIn().readLong();
                System.out.println("Ожидаемый размер файла: " + fileSize + " байт");
                
                clientConnection.send(new Message(MessageType.HTTP150, "Ожидаю файл " + fileName));
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalReceived = 0;
                
                while (totalReceived < fileSize && (bytesRead = clientConnection.getIn().read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                    System.out.println("Получено " + totalReceived + " из " + fileSize + " байт");
                }
                
                bos.flush();
                
                if (totalReceived == fileSize) {
                    System.out.println("Файл загружен, отправляю HTTP226");
                    clientConnection.send(new Message(MessageType.HTTP226, "Файл загружен"));
                    System.out.println("Сообщение HTTP226 отправлено успешно");
                } else {
                    System.out.println("Ошибка: получено неверное количество байт");
                    clientConnection.send(new Message(MessageType.HTTP550, "Ошибка загрузки файла"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                clientConnection.send(new Message(MessageType.HTTP550, "Ошибка загрузки файла"));
            }
        }

        @Override
        public void run() {
            System.out.println("Установлено новое соединение с " + clientConnection.getRemoteSocketAddress());
            try {
                userConnecting(clientConnection);
                serverMainLoop(clientConnection);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.startServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
