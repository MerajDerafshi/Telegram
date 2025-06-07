package ToolBox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.ConnectException;
import java.util.function.Consumer;


public class NetworkConnection {
    private ConnectionThread connectionThread;
    public Consumer<Serializable> receiveCallBack;
    public String ip;
    public boolean isServer;
    public int port;


    public static final String STATUS_CONNECTION_SUCCESS = "SYSTEM_STATUS:CONNECTION_SUCCESS";
    public static final String STATUS_CONNECTION_FAILED_CLIENT = "SYSTEM_STATUS:CONNECTION_FAILED_CLIENT";
    public static final String STATUS_CONNECTION_CLOSED_BY_PEER = "SYSTEM_STATUS:CONNECTION_CLOSED_BY_PEER";
    public static final String STATUS_CONNECTION_TERMINATED = "SYSTEM_STATUS:CONNECTION_TERMINATED";
    public static final String STATUS_NETWORK_ERROR = "SYSTEM_STATUS:NETWORK_ERROR";
    public static final String STATUS_DATA_ERROR_CLASSNOTFOUND = "SYSTEM_STATUS:DATA_ERROR_CLASSNOTFOUND";


    public NetworkConnection(Consumer<Serializable> receiveCallBack, String ip, boolean isServer, int port) {
        this.receiveCallBack = receiveCallBack;
        this.ip = ip;
        this.isServer = isServer;
        this.port = port;
        this.connectionThread = new ConnectionThread(); // Initialize here
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
            throw new IOException("Connection not established, output stream not available, or socket closed.");
        }
        connectionThread.outputStream.writeObject(data);
        connectionThread.outputStream.flush();
    }


    public void sendImage(javafx.scene.image.Image image) throws IOException {
        throw new UnsupportedOperationException(
                "Convert the image to a serializable format (e.g., byte array) and use sendData().");
    }

    public void closeConnection() throws IOException {
        System.out.println("Attempting to close connection...");
        if (connectionThread.socket != null && !connectionThread.socket.isClosed()) {
            try {
                connectionThread.socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket in closeConnection: " + e.getMessage());
                if (connectionThread.isAlive()) {
                    connectionThread.interrupt();
                }
                throw e;
            }
        } else if (connectionThread.isAlive()) {
            connectionThread.interrupt();
            if (connectionThread.serverSocket != null && !connectionThread.serverSocket.isClosed()) {
                try {
                    connectionThread.serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing serverSocket in closeConnection: " + e.getMessage());
                }
            }
        }
        System.out.println("closeConnection call completed.");
    }

    private class ConnectionThread extends Thread {
        volatile Socket socket;
        volatile ObjectOutputStream outputStream;
        volatile ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();
            try {
                if (isServer) {
                    serverSocket = new ServerSocket(port);
                    System.out.println("Server: Started on port " + port + ". Waiting for client connection...");
                    socket = serverSocket.accept();
                    System.out.println("Server: Client connected: " + socket.getInetAddress());
                } else {
                    System.out.println("Client: Attempting to connect to " + ip + ":" + port + "...");
                    socket = new Socket(ip, port);
                    System.out.println("Client: Connected to server: " + socket.getInetAddress());
                }

                socket.setTcpNoDelay(true);

                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());


                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_CONNECTION_SUCCESS);
                }

                while (socket.isConnected() && !socket.isClosed()) {
                    Serializable data = (Serializable) inputStream.readObject(); // Blocks here
                    if (receiveCallBack != null) {
                        receiveCallBack.accept(data);
                    }
                }

            } catch (ConnectException e) {
                System.err.println("Client: Connection failed/refused: " + e.getMessage());
                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_CONNECTION_FAILED_CLIENT + ": " + e.getMessage());
                }
            } catch (SocketException e) {
                System.err.println("NetworkConnection SocketException: " + e.getMessage());
                if (receiveCallBack != null) {

                    if (e.getMessage().toLowerCase().contains("connection reset")) {
                        receiveCallBack.accept(STATUS_CONNECTION_CLOSED_BY_PEER + ": " + e.getMessage());
                    } else {
                        receiveCallBack.accept(STATUS_CONNECTION_TERMINATED + ": " + e.getMessage());
                    }
                }
            } catch (java.io.EOFException e) {
                System.out.println("NetworkConnection: Connection closed by peer (EOFException).");
                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_CONNECTION_CLOSED_BY_PEER);
                }
            } catch (IOException e) {
                System.err.println("NetworkConnection IOException: " + e.getClass().getName() + " - " + e.getMessage());
                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_NETWORK_ERROR + ": " + e.getMessage());
                }
            } catch (ClassNotFoundException e) {
                System.err.println("NetworkConnection ClassNotFoundException: " + e.getMessage());
                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_DATA_ERROR_CLASSNOTFOUND + ": " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("NetworkConnection Unexpected Exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
                if (receiveCallBack != null) {
                    receiveCallBack.accept(STATUS_NETWORK_ERROR + ": Unexpected error - " + e.getMessage());
                }
            }
            finally {
                System.out.println("ConnectionThread: Entering finally block for cleanup.");
                try {
                    if (outputStream != null) {
                        outputStream.close();
                        System.out.println("ConnectionThread: ObjectOutputStream closed.");
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        System.out.println("ConnectionThread: Socket closed.");
                    }
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        System.out.println("ConnectionThread: ServerSocket closed.");
                    }
                } catch (IOException ex) {
                    System.err.println("ConnectionThread: Error during cleanup: " + ex.getMessage());
                }
                System.out.println("ConnectionThread: Terminated.");
            }
        }
    }
}
