package ToolBox;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class NetworkConnection {

    private ConnectionThread connectionThread;
    public Consumer<Serializable> receiveCallback;
    public String ip;
    public boolean isServer;
    public int port;
    public String userName;
    private static final Set<String> onlineUsers = new HashSet<>();


    public NetworkConnection(Consumer<Serializable> receiveCallback, String ip, boolean isServer, int port, String userName) {
        this.receiveCallback = receiveCallback;
        this.ip = ip;
        this.isServer = isServer;
        this.port = port;
        this.userName = userName;
        this.connectionThread = new ConnectionThread();
    }

    public boolean isUserOnline(String phone) {
        return onlineUsers.contains(phone);
    }

    public void openConnection() {
        if (!connectionThread.isAlive()) {
            if (connectionThread.getState() == Thread.State.TERMINATED) {
                this.connectionThread = new ConnectionThread();
            }
            connectionThread.start();
        }
    }

    public void sendData(Serializable data) throws IOException {
        if (connectionThread.outputStream == null || connectionThread.socket == null || connectionThread.socket.isClosed()) {
            throw new IOException("Connection not established.");
        }
        connectionThread.outputStream.writeObject(data);
        connectionThread.outputStream.flush();
    }

    public void closeConnection() throws IOException {
        if (connectionThread.socket != null && !connectionThread.socket.isClosed()) {
            connectionThread.socket.close();
        }
        if (connectionThread.serverSocket != null && !connectionThread.serverSocket.isClosed()) {
            connectionThread.serverSocket.close();
        }
        connectionThread.interrupt();
    }

    private class ConnectionThread extends Thread {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;
        private ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                if (isServer) {
                    serverSocket = new ServerSocket(port);
                    System.out.println("[Server] Waiting for a client...");
                    socket = serverSocket.accept();
                    System.out.println("[Server] Client connected.");
                } else {
                    socket = new Socket(ip, port);
                    System.out.println("[Client] Connected to server.");
                }

                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();

                // Send INIT message
                outputStream.writeObject("INIT>" + userName);
                outputStream.flush();
                onlineUsers.add(userName);


                inputStream = new ObjectInputStream(socket.getInputStream());


                if (receiveCallback != null) {
                    receiveCallback.accept("SYSTEM_STATUS:CONNECTION_SUCCESS");
                }

                while (!socket.isClosed()) {
                    try {
                        Serializable data = (Serializable) inputStream.readObject();
                        if (data instanceof String && ((String) data).startsWith("ONLINE_USERS:")) {
                            String[] users = ((String) data).substring(13).split(",");
                            onlineUsers.clear();
                            for (String user : users) {
                                onlineUsers.add(user);
                            }
                        } else if (receiveCallback != null) {
                            receiveCallback.accept(data);
                        }
                    } catch (EOFException e) {
                        System.out.println("[Network] Connection closed.");
                        break;
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                if (receiveCallback != null) {
                    receiveCallback.accept("SYSTEM_STATUS:ERROR: " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                onlineUsers.remove(userName);
                cleanup();
            }
        }

        private void cleanup() {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (socket != null && !socket.isClosed()) socket.close();
                if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
                System.out.println("[Network] Connection closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
