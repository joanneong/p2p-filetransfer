import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class P2PClient {

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    String messageReceived;

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
        sc.nextLine();

        if (messageReceived.equals(Constant.MESSAGE_CHUNK_NOT_EXIST)) {
            return Constant.ERROR_FILE_NOT_EXIST;
        } else {
            String p2pServerIP = messageReceived;

            return "File " + fileName + " found at port " + Constant.P2P_SERVER_PORT + " of P2P server " + p2pServerIP;
        }
    }

    private String getDownloadMessage(String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(Constant.DEFAULT_DIRECTORY + fileName);

        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int chunkNumber = 1;

        while (true) {
            messageReceived = getQueryMessage(fileName, chunkNumber);

            if (messageReceived.equals(Constant.ERROR_FILE_NOT_EXIST)) {
                break;
            }

            int i;
            for (i = 0; i < messageReceived.length() - 10; i++) {
                if (messageReceived.substring(i, i + 10).equals("P2P server")) {
                    i = i + 11;
                    break;
                }
            }
            String p2pServerIP = messageReceived.substring(i);

            Socket socketToP2PServer = connectToServer(p2pServerIP, Constant.P2P_SERVER_PORT);

            sendQueryToP2PServer(fileName, chunkNumber, socketToP2PServer);

            receiveDataFromP2PServer(bos, socketToP2PServer);

            socketToP2PServer.close();

            getInformMessage(fileName, chunkNumber);

            chunkNumber++;
        }

        bos.close();

        return "File " + fileName + " downloaded from peer server";
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

    private String getExitMessage() throws IOException {

        String toServer = Constant.COMMAND_EXIT;
        pw.println(toServer);
        pw.flush();

        messageReceived =  sc.nextLine();
        sc.nextLine();

        sendExitToOwnServer();

        return messageReceived;
    }

    private void sendExitToOwnServer() throws IOException {
        Socket socketToOwnServer = connectToServer("localhost", Constant.P2P_SERVER_PORT);
        PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
        writerToOwnServer.println(Constant.COMMAND_EXIT);
        writerToOwnServer.flush();

        Scanner scFromOwnServer = new Scanner(socketToOwnServer.getInputStream());
        if (!scFromOwnServer.nextLine().equals(Constant.MESSAGE_ACK)) {
            System.out.println(Constant.ERROR_OWN_SERVER_NOT_CLOSED);
        }

        scFromOwnServer.close();
        socketToOwnServer.close();
    }

    private Socket connectToServer(String p2pServerIP, int p2pServerPort) throws IOException {
        return new Socket(p2pServerIP, p2pServerPort);
    }

    private void receiveDataFromP2PServer(BufferedOutputStream bos, Socket socketToP2PServer) throws IOException {
        byte[] buffer = new byte[1024];
        socketToP2PServer.getInputStream().read(buffer);
        bos.write(buffer);
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

    private int getNumberOfChunks(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(Constant.DEFAULT_DIRECTORY + fileName));

        char[] buffer = new char[1024];

        int chunkCount = 0;
        while (br.read(buffer) != -1) {
            //System.out.println("Is counting...");
            chunkCount++;
        }

        br.close();

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
                    System.out.println("File " + fileName + " informed to directory server");
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
            P2PClient client = new P2PClient();
            client.start(serverIP, serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
