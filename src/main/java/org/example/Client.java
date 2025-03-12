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
                    case HTTP226 -> System.out.println("Файл был успешно загружен.");
                    case HTTP550 -> System.out.println("Не удалось загрузить файл.");
                    case FILE_NOT_EXIST -> System.out.println(message.getData());
                    case SEND_FILE -> saveFileClient(message.getData());
                }
            }
        }
    }
    public void start() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
    }


    public void saveFileClient(String fileName) throws IOException {
        File file = new File(fileName);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            long fileSize = connection.getIn().readLong();
            System.out.println("Ожидаемый размер файла: " + fileSize + " байт");


            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalReceived = 0;

            while (totalReceived < fileSize && (bytesRead = connection.getIn().read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
                System.out.println("Получено " + totalReceived + " из " + fileSize + " байт");
            }

            bos.flush();

            if (totalReceived == fileSize) {
                connection.send(new Message(MessageType.HTTP226, "Файл загружен"));
            } else {
                System.out.println("Ошибка: получено неверное количество байт");
            }
        } catch (IOException e) {
            e.printStackTrace();
            connection.send(new Message(MessageType.HTTP550, "Ошибка загрузки файла"));
        }
    }

    public void downloadFile(String name) throws IOException {
        connection.send(new Message(MessageType.RECEIVE_FILE, name));
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
