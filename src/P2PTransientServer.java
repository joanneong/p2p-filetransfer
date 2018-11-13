import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class P2PTransientServer implements Runnable {

    static String directoryServerIP;
    static int directoryServerPort;
    String uniqueName;

    Socket uniqueSocket;

    PrintWriter pw;
    Scanner sc;

    // For thread to send data to directory server
    String requestFilename;
    int requestChunkNum;
    String requestUniqueName;

    // Constructor function for P2P transient server
    public P2PTransientServer(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public P2PTransientServer(String requestFilename, int requestChunkNum, String requestUniqueName,
                              Socket uniqueSocket, String uniqueName) {
        this.requestFilename = requestFilename;
        this.requestChunkNum = requestChunkNum;
        this.requestUniqueName = requestUniqueName;
        this.uniqueSocket = uniqueSocket;
        this.uniqueName = uniqueName;
    }

    // Send a P2P transient server's unique name to the directory server during the initial connection
    // This unique name is the same unique name as the P2P client which it is associated with
    private void sendNameMessage() {
        String toServer = Constant.COMMAND_NAME + Constant.MESSAGE_DELIMITER
                + uniqueName + Constant.MESSAGE_DELIMITER
                + Constant.TYPE_TRANSIENT_SOCKET + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        String[] message = getMessageFromClientSocket(uniqueSocket);

        if (message[0].equals(Constant.MESSAGE_ACK)) {
            System.out.println("Unique name " + uniqueName + " successfully sent to directory server!");
        } else {
            System.err.println("Error: could not send unique name " + uniqueName + " to directory server!");
        }
    }

    public void start() throws IOException {

        // Create a transient server socket and connect to the directory server
        uniqueSocket = new Socket(directoryServerIP, directoryServerPort);
        System.out.println("P2P transient server connected to directory server: " + directoryServerIP
                + " at port " + directoryServerPort + Constant.MESSAGE_DELIMITER);

        // Open writer and scanner between p2p transient server and directory server
        pw = new PrintWriter(uniqueSocket.getOutputStream(), true);
        sc = new Scanner(uniqueSocket.getInputStream());

        // Inform the directory server that this connection is to a P2P transient server
        sendNameMessage();

        // Listen to incoming
        while(true) {
            handleClientSocket(uniqueSocket);
        }
    }

    private void handleClientSocket(Socket client) {
        String messageFromClient = getMsgFromClient(client);

        if (!messageFromClient.isEmpty()) {
            System.out.println("Parsing directory server message...");
            String[] parsedClientMsg = parse(messageFromClient);

            System.out.println("Preparing reply...");
            handleClientMsg(client, parsedClientMsg);
        }
    }

    private void handleClientMsg(Socket client, String[] parsedClientMsg) {

        String type = parsedClientMsg[0];
        System.out.println("Client message has type: " + type);

        switch(type) {
            case Constant.COMMAND_DOWNLOAD:
                String filename = parsedClientMsg[1];
                int chunkNumber = Integer.parseInt(parsedClientMsg[2]);
                String clientUniqueName = parsedClientMsg[3];

                // Create new thread to handel download command
                P2PTransientServer newServer = new P2PTransientServer(filename, chunkNumber,
                                                                        clientUniqueName, client, uniqueName);
                new Thread(newServer).start();
                break;

            case Constant.COMMAND_EXIT:
                System.exit(0);
                break;

            default:
                System.out.println(Constant.ERROR_INVALID_COMMAND);
        }
    }

    private void handleDownloadMsg(String filename, int chunkNumber, String clientUniqueName) {

        // Send ACK to acknowledge receiving DOWNLOAD command
        send(uniqueSocket, getAckMessage() + uniqueName + Constant.MESSAGE_DELIMITER);

        String messageToSend = Constant.COMMAND_UPLOAD + Constant.MESSAGE_DELIMITER
                                + clientUniqueName + Constant.MESSAGE_DELIMITER;
        try {
            // Create new socket to directory server
            Socket newSocketToDirectory = new Socket(directoryServerIP, directoryServerPort);

            // Send UPLOAD command
            send(newSocketToDirectory, messageToSend);

            // Get ACK from directory server
            String[] message = getMessageFromClientSocket(newSocketToDirectory);
            if (message[0].equals(Constant.MESSAGE_ACK)) {
                System.out.println("UPLOAD command requested by " + clientUniqueName
                        + " is successfully acknowledged by directory server!");
            } else {
                System.err.println("Error receiving ACK of UPLOAD command requested by " + clientUniqueName);
            }

            // Send data
            byte[] messageRequested = getRequestMessage(filename, chunkNumber);
            sendP2PResponse(newSocketToDirectory, messageRequested);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getRequestMessage(String fileName, int chunkNum) {

        int bytesRead; // the number of bytes read by buffer

        try{
            RandomAccessFile file = new RandomAccessFile(Constant.DEFAULT_DIRECTORY + fileName, "r");
            file.seek(Constant.CHUNK_SIZE * (chunkNum - 1)); // move the pointer to the position where we start reading
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            bytesRead = file.read(buffer);

            System.out.println("Requested file: " + fileName + "; chunk No." + chunkNum + "; length: " + bytesRead);

            file.close();

            if (bytesRead == Constant.CHUNK_SIZE) {
                return buffer;
            } else { // it is the case for the last packet
                return Arrays.copyOfRange(buffer, 0, bytesRead);
            }

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return new byte[Constant.CHUNK_SIZE];
        }
    }

    @Override
    public void run() {
        System.out.println("New thread created to send " + requestFilename + " chunk "
                + requestChunkNum + " requested by " + requestUniqueName);
        handleDownloadMsg(requestFilename, requestChunkNum, requestUniqueName);
        System.out.println("Closing socket which sent data requested by " + requestUniqueName + "\n");
    }

    public static void main(String[] args) {

        // Check if the number of command line argument is 3
        if (args.length != 3) {
            System.err.println("Usage: java P2PTransientServer directoryServerIP directoryServerPort uniqueName");
            System.exit(1);
        }

        P2PTransientServer.directoryServerIP = args[0];
        P2PTransientServer.directoryServerPort = Integer.parseInt(args[1]);
        String uniqueName = args[2];

        try {
            P2PTransientServer transientServer = new P2PTransientServer(uniqueName);
            transientServer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void sendP2PResponse(Socket serverSocket, byte[] response) {
        try {
            DataOutputStream toClient = new DataOutputStream(serverSocket.getOutputStream());
            toClient.write(response);
            toClient.flush();

            System.out.println("Data requested has been sent");
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    // Send TCP message to client
    private void send(Socket client, String messageToSend) {
        try {
            System.out.println("Sending: " + messageToSend);
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
            writer.println(messageToSend);
            writer.flush();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private String getMsgFromClient(Socket client) {
        String messageFromClient = "";

        try {
            BufferedReader scanner = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String nextLine = scanner.readLine();

            while (nextLine != null) {
                System.out.println("Current line read: " + nextLine);
                messageFromClient += nextLine;
                messageFromClient += Constant.MESSAGE_DELIMITER;

                if (scanner.ready()) {
                    nextLine = scanner.readLine();
                } else {
                    nextLine = null;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return messageFromClient;
    }

    private String[] parse(String message) {
        return message.split(Constant.MESSAGE_DELIMITER);
    }

    private String[] getMessageFromClientSocket(Socket client) {
        String messageFromClient = getMsgFromClient(client);

        String[] parsedClientMsg = parse(messageFromClient);

        return parsedClientMsg;
    }

    private String getAckMessage() {
        return Constant.MESSAGE_ACK + Constant.MESSAGE_DELIMITER;
    }

}
