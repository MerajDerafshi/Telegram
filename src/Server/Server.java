package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 8080;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server started on port: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToClient(String receiver, Serializable data) {
        ClientHandler receiverHandler = clients.get(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendMessage(data);
        } else {
            System.out.println("Could not find client: " + receiver);
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

        public void sendMessage(Serializable data) {
            try {
                if (out != null) {
                    out.writeObject(data);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Error sending message to " + userName + ": " + e.getMessage());
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
                    System.out.println(userName + " has joined the chat.");
                } else {
                    System.err.println("Initialization failed for client: " + socket.getInetAddress());
                    return;
                }

                Object receivedObject;
                while ((receivedObject = in.readObject()) != null) {
                    if (receivedObject instanceof String) {
                        String msg = (String) receivedObject;
                        System.out.println("[Text Received] from " + userName + ": " + msg);
                        String[] parts = msg.split(">");
                        if (parts.length >= 4 && parts[0].equals("text")) {
                            Server.sendToClient(parts[2], msg);
                        }
                    } else if (receivedObject instanceof ToolBox.ImageMessage) {
                        ToolBox.ImageMessage imgMsg = (ToolBox.ImageMessage) receivedObject;
                        System.out.println("[Image Received] from " + imgMsg.sender + " to " + imgMsg.receiver);
                        Server.sendToClient(imgMsg.receiver, imgMsg);
                    } else if (receivedObject instanceof ToolBox.FileMessage) {
                        ToolBox.FileMessage fileMsg = (ToolBox.FileMessage) receivedObject;
                        System.out.println("[File Received] from " + fileMsg.sender + " to " + fileMsg.receiver);
                        Server.sendToClient(fileMsg.receiver, fileMsg);
                    } else {
                        System.out.println("[Unknown Data Type Received] from " + userName + ": " + receivedObject.getClass().getName());
                    }
                }
            } catch (EOFException e) {
                System.out.println("Client " + (userName != null ? userName : socket.getInetAddress()) + " has disconnected.");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("An error occurred with client " + (userName != null ? userName : "") + ": " + e.getMessage());
            } finally {
                if (userName != null) {
                    clients.remove(userName);
                    System.out.println(userName + " has been removed from the client list.");
                }
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
