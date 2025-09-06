package Server;

import ToolBox.DeleteMessage;
import ToolBox.TextMessage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 55555;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

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
            System.out.println("Could not find client: " + receiver + ". User may be offline.");
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String userPhone;

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
                System.err.println("Error sending message to " + userPhone + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                String initMsg = (String) in.readObject();
                if (initMsg.startsWith("INIT>")) {
                    userPhone = initMsg.split(">")[1];
                    clients.put(userPhone, this);
                    System.out.println(userPhone + " has joined the chat.");
                } else {
                    System.err.println("Initialization failed for client: " + socket.getInetAddress());
                    return;
                }

                Object receivedObject;
                while ((receivedObject = in.readObject()) != null) {
                    if (receivedObject instanceof TextMessage) {
                        TextMessage textMsg = (TextMessage) receivedObject;
                        System.out.println("[Text Received] from " + textMsg.sender + " to " + textMsg.receiver);
                        Server.sendToClient(textMsg.receiver, textMsg);
                    } else if (receivedObject instanceof ToolBox.ImageMessage) {
                        ToolBox.ImageMessage imgMsg = (ToolBox.ImageMessage) receivedObject;
                        System.out.println("[Image Received] from " + imgMsg.sender + " to " + imgMsg.receiver);
                        Server.sendToClient(imgMsg.receiver, imgMsg);
                    } else if (receivedObject instanceof ToolBox.FileMessage) {
                        ToolBox.FileMessage fileMsg = (ToolBox.FileMessage) receivedObject;
                        System.out.println("[File Received] from " + fileMsg.sender + " to " + fileMsg.receiver);
                        Server.sendToClient(fileMsg.receiver, fileMsg);
                    } else if (receivedObject instanceof DeleteMessage) {
                        DeleteMessage deleteMsg = (DeleteMessage) receivedObject;
                        System.out.println("[Delete Request] from " + deleteMsg.senderPhone + " for message " + deleteMsg.messageId);
                        Server.sendToClient(deleteMsg.receiverPhone, deleteMsg);
                    } else {
                        System.out.println("[Unknown Data Type Received] from " + userPhone + ": " + receivedObject.getClass().getName());
                    }
                }
            } catch (EOFException e) {
                System.out.println("Client " + (userPhone != null ? userPhone : socket.getInetAddress()) + " has disconnected.");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("An error occurred with client " + (userPhone != null ? userPhone : "") + ": " + e.getMessage());
            } finally {
                if (userPhone != null) {
                    clients.remove(userPhone);
                    System.out.println(userPhone + " has been removed from the client list.");
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

