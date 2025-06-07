package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 55555;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void sendToClient(String receiver, String message) {
        ClientHandler receiverHandler = clients.get(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.out.println("Error sending to " + userName);
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());


                String initMsg = (String) in.readObject();
                if (initMsg.startsWith("INIT>")) {
                    userName = initMsg.split(">")[1];
                    clients.put(userName, this);
                    System.out.println(userName + " joined.");
                }


                String msg;
                while ((msg = (String) in.readObject()) != null) {
                    System.out.println("Received: " + msg);
                    String[] parts = msg.split(">");
                    if (parts.length >= 4 && parts[0].equals("text")) {
                        Server.sendToClient(parts[2], msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + userName);
            } finally {
                try {
                    if (userName != null) {
                        clients.remove(userName);
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
