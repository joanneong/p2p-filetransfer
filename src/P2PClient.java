import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class P2PClient implements Runnable {

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    String messageReceived;

    P2PTransientServer transientServer;

    public P2PClient(P2PTransientServer serverInstance) {
        this.transientServer = serverInstance;
    }

    // String ownServerPublicIP;
    // String ownServerPublicPort;

    private String getInformMessage(String fileName, int chunkNumber) {

        String toServer = Constant.COMMAND_INFORM + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        messageReceived = sc.nextLine();
        System.out.println("Message from directory server: " + messageReceived);
        sc.nextLine();

        if (messageReceived.equals(Constant.MESSAGE_ACK)) {
            return "File " + fileName + "chunk " + chunkNumber + " informed to directory server";
        } else {
            return Constant.ERROR_CLIENT_INFORM_FAILED;
        }
    }

    private String getQueryMessage(String fileName, int chunkNumber) {

        String toServer = Constant.COMMAND_QUERY + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        sc.nextLine();
        messageReceived = sc.nextLine();

        if (messageReceived.equals(Constant.MESSAGE_CHUNK_NOT_EXIST)) {
            sc.nextLine();
            return Constant.ERROR_QUERY_FILE_NOT_EXIST;
        } else {
            String p2pServerIP = messageReceived;
            String p2pServerPort = sc.nextLine();
            sc.nextLine();

            return "File " + fileName + " found at port " + p2pServerPort + " of P2P server " + p2pServerIP + Constant.MESSAGE_DELIMITER;
        }
    }

    private String getDownloadMessage(String fileName) throws IOException {

        boolean isExist = new File(Constant.DEFAULT_DIRECTORY + fileName).exists();
        if (isExist) {
            return Constant.ERROR_DOWNLOAD_FILE_EXIST;
        }

        FileOutputStream fos = new FileOutputStream(Constant.DEFAULT_DIRECTORY + fileName);

        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int chunkNumber = 1;

        while (true) {
            messageReceived = getQueryMessage(fileName, chunkNumber);

            if (messageReceived.equals(Constant.ERROR_QUERY_FILE_NOT_EXIST)) {
                break;
            }

            // Get IP address of P2P server
            int i;
            for (i = 0; i < messageReceived.length() - 10; i++) {
                if (messageReceived.substring(i, i + 10).equals("P2P server")) {
                    i = i + 12;
                    break;
                }
            }
            String p2pServerIP = messageReceived.substring(i, messageReceived.length() - 2);

            // Get port number of P2P server
            for (i = 0; i < messageReceived.length() - 4; i++) {
                if (messageReceived.substring(i, i + 4).equals("port")) {
                    i = i + 5;
                    break;
                }
            }
            StringBuilder portBuilder = new StringBuilder();
            while (messageReceived.charAt(i) != ' ') {
                portBuilder.append(messageReceived.charAt(i));
                i++;
            }
            int p2pServerPort = Integer.parseInt(portBuilder.toString());

            Socket socketToP2PServer = connectToServer(p2pServerIP, p2pServerPort);

            sendQueryToP2PServer(fileName, chunkNumber, socketToP2PServer);

            receiveDataFromP2PServer(bos, socketToP2PServer);

            socketToP2PServer.close();

            getInformMessage(fileName, chunkNumber);

            chunkNumber++;
        }

        bos.close();

        if (chunkNumber == 1) {
            File file = new File(Constant.DEFAULT_DIRECTORY + fileName);
            file.delete();
            return Constant.ERROR_DOWNLOAD_FILE_NOT_EXIST;
        }
        return "File " + fileName + " downloaded from peer server" + Constant.MESSAGE_DELIMITER;
    }

    private String getListMessage() {

        String toServer = Constant.COMMAND_LIST;
        pw.println(toServer);
        pw.flush();

        StringBuilder replyMessage = new StringBuilder();
        replyMessage.append("File list:").append(Constant.MESSAGE_DELIMITER);

        sc.nextLine();
        messageReceived = sc.nextLine();

        if (messageReceived.equals(Constant.MESSAGE_FILE_LIST_EMPTY)) {
            replyMessage.append("There is no file available").append(Constant.MESSAGE_DELIMITER);
        } else {
            int fileCount = Integer.parseInt(messageReceived);
            for (int i = 0; i < fileCount; i++) {
                messageReceived = sc.nextLine();
                replyMessage.append(messageReceived).append(Constant.MESSAGE_DELIMITER);
            }
        }

        sc.nextLine();

        return replyMessage.toString();
    }

    private String getExitMessage() {

        String toServer = Constant.COMMAND_EXIT;
        pw.println(toServer);
        pw.flush();

        messageReceived =  sc.nextLine();
        sc.nextLine();

        return messageReceived + Constant.MESSAGE_DELIMITER;
    }

    // Ask own P2P server for public IP and port to inform directory server
    // private String askIpconfigToOwnServer() throws IOException {
    //     Socket socketToOwnServer = connectToServer("localhost", Constant.P2P_SERVER_PORT);
    //     PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
    //     writerToOwnServer.println(Constant.COMMAND_IPCONFIG);
    //     writerToOwnServer.flush();

    //     Scanner scFromOwnServer = new Scanner(socketToOwnServer.getInputStream());
    //     String response = scFromOwnServer.nextLine();

    //     scFromOwnServer.close();
    //     socketToOwnServer.close();

    //     return response;
    // }

    private Socket connectToServer(String p2pServerIP, int p2pServerPort) throws IOException {
        return new Socket(p2pServerIP, p2pServerPort);
    }

    private void receiveDataFromP2PServer(BufferedOutputStream bos, Socket socketToP2PServer) throws IOException {
        byte[] buffer = new byte[Constant.CHUNK_SIZE];
        int bytesRead = socketToP2PServer.getInputStream().read(buffer);

        bos.write(buffer, 0, bytesRead);
        bos.flush();
    }

    private void sendQueryToP2PServer(String fileName, int chunkNumber, Socket socketToP2PServer) throws IOException {

        PrintWriter writerToP2PServer = new PrintWriter(socketToP2PServer.getOutputStream(), true);

        String toP2PServer = Constant.COMMAND_QUERY + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        writerToP2PServer.println(toP2PServer);
        writerToP2PServer.flush();
    }

    private int getNumberOfChunks(String fileName) {
        File f = new File(Constant.DEFAULT_DIRECTORY + fileName);
        int fileLength = (int) f.length();

        int chunkCount = fileLength / Constant.CHUNK_SIZE;

        if (fileLength > (chunkCount * Constant.CHUNK_SIZE)) {
            chunkCount = chunkCount + 1;
        }

        return chunkCount;
    }

    private void start(String serverIP, int serverPort) throws IOException {

        // Create a client socket and connect to the server
        clientSocket = connectToServer(serverIP, serverPort);
        System.out.println("Connected to directory server: " + serverIP + " at port " + serverPort);

        // Read user input from keyboard
        Scanner scanner = new Scanner(System.in);
        String fromClient = scanner.next();

        pw = new PrintWriter(clientSocket.getOutputStream(), true);
        sc = new Scanner(clientSocket.getInputStream());

        // Get own transient server's public IP and port, disabled due to symmetric network
        // messageReceived = askIpconfigToOwnServer();
        // String[] temp = messageReceived.split(":");
        // ownServerPublicIP = temp[0];
        // ownServerPublicPort = temp[1];

        String fileName;
        String replyMessage;
        int chunkNumber;

        while (true) {
            switch (fromClient.toUpperCase()) {
            case Constant.COMMAND_INFORM:
                fileName = scanner.next();
                System.out.println("File name: " + fileName);
                chunkNumber = getNumberOfChunks(fileName);
                System.out.println("Number of chunks: " + chunkNumber);

                if (chunkNumber == -1) {
                    System.out.println(Constant.ERROR_INFORM_FILE_NOT_EXIST);
                    break;
                }

                boolean isInformSuccess = true;
                for (int i = 1; i <= chunkNumber; i++) {
                    replyMessage = getInformMessage(fileName, i);
                    if (replyMessage.equals(Constant.ERROR_CLIENT_INFORM_FAILED)) {
                        System.out.println(replyMessage);
                        isInformSuccess = false;
                        break;
                    }
                }
                if (isInformSuccess) {
                    System.out.println("File " + fileName + " informed to directory server" + Constant.MESSAGE_DELIMITER);
                }
                break;
            case Constant.COMMAND_QUERY:
                fileName = scanner.next();
                chunkNumber = 1;
                replyMessage = getQueryMessage(fileName, chunkNumber);
                System.out.println(replyMessage);
                break;
            case Constant.COMMAND_DOWNLOAD:
                fileName = scanner.next();
                replyMessage = getDownloadMessage(fileName);
                System.out.println(replyMessage);
                break;
            case Constant.COMMAND_LIST:
                replyMessage = getListMessage();
                System.out.println(replyMessage);
                break;
            case Constant.COMMAND_EXIT:
                replyMessage = getExitMessage();
                System.out.println(replyMessage);
                break;
            default:
                System.out.println(Constant.ERROR_INVALID_COMMAND);
                scanner.nextLine();
                break;
            }

            if (fromClient.toUpperCase().equals(Constant.COMMAND_EXIT)) {
                scanner.close();
                sc.close();

                clientSocket.close();

                System.out.println("Closing socket");
                break;
            }

            fromClient = scanner.next();
        }
    }

    public static void main(String[] args) {

        // Check if the number of command line argument is 2
        if (args.length != 2) {
            System.err.println("Usage: java TCPEchoClient serverIP serverPort");
            System.exit(1);
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try {
            P2PTransientServer serverInstance = new P2PTransientServer();
            P2PClient newServer = new P2PClient(serverInstance);
            new Thread(newServer).start();

            P2PClient client = new P2PClient(serverInstance);
            client.start(serverIP, serverPort);

            client.transientServer.exit();
            System.out.println("here");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("New thread created to run transient server...\n");
        new File(Constant.DEFAULT_DIRECTORY).mkdirs();

        transientServer.start(Constant.P2P_SERVER_PORT);
        System.out.println("Exiting...");
        return;
    }

}
