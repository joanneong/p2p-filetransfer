import java.io.*;
import java.net.*;
import java.util.*;

public class P2PClient {

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    String messageReceived;

    private String getInformMessage(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        char[] buffer = new char[1024];

        int chunkCount = 0;
        while (br.read(buffer) != 0) {
            chunkCount++;
        }

        String toServer = "INFORM\r\n" + fileName + "\r\n" + chunkCount;
        pw.println(toServer);
        pw.flush();

        messageReceived = sc.nextLine();
        if (messageReceived.equals("ACK")) {
            return "File " + fileName + " informed to directory server";
        } else {
            return "Inform failed";
        }
    }

    private String getQueryMessage(String fileName, int chunkNumber) {

        String toServer = "QUERY\r\n" + fileName + "\r\n" + chunkNumber;
        pw.println(toServer);
        pw.flush();

        sc.nextLine();
        messageReceived = sc.nextLine();

        if (messageReceived.equals("CHUNK NOT EXIST")) {
            return "File queried does not exist";
        } else {
            String p2pServerIP = messageReceived;
            int p2pServerPort = Integer.parseInt(sc.nextLine());

            return "File " + fileName + " found at port " + p2pServerPort + " of P2P server " + p2pServerIP;
        }
    }

    private String getDownloadMessage(String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int chunkNumber = 1;

        while (true) {
            messageReceived = getQueryMessage(fileName, chunkNumber);

            if (messageReceived.equals("File queried does not exist")) {
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
            int p2pServerPort = 9019;

            Socket socketToP2PServer = connectToServer(p2pServerIP, p2pServerPort);

            sendQueryToP2PServer(fileName, chunkNumber, socketToP2PServer);

            receiveDataFromP2PServer(bos, socketToP2PServer);

            socketToP2PServer.close();

            chunkNumber++;
        }

        bos.close();

        return "File " + fileName + " downloaded from peer server";
    }

    private String getListMessage() {

        String toServer = "LIST";
        pw.println(toServer);
        pw.flush();

        StringBuilder replyMessage = new StringBuilder();
        replyMessage.append("File list:\r\n");

        sc.nextLine();
        messageReceived = sc.nextLine();
        while (!messageReceived.equals("")) {
            replyMessage.append(messageReceived).append("\r\n");
            messageReceived = sc.nextLine();
        }

        return replyMessage.toString();
    }

    private String getExitMessage() throws IOException {

        String toServer = "EXIT";
        pw.println(toServer);
        pw.flush();

        messageReceived =  sc.nextLine();

        sendExitToOwnServer();

        return messageReceived;
    }

    private void sendExitToOwnServer() throws IOException {
        Socket socketToOwnServer = connectToServer("localhost", 9019);
        PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
        writerToOwnServer.println("EXIT");
        writerToOwnServer.flush();

        Scanner scFromOwnServer = new Scanner(socketToOwnServer.getInputStream());
        if (!scFromOwnServer.nextLine().equals("ACK")) {
            System.out.println("Own host server is not closed!");
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

        String toP2PServer = "QUERY\r\n" + fileName + "\r\n" + chunkNumber;
        writerToP2PServer.println(toP2PServer);
        writerToP2PServer.flush();
    }

    private void start(String serverIP, int serverPort) throws IOException {

        // Create a client socket and connect to the server
        clientSocket = connectToServer(serverIP, serverPort);
        System.out.println("Connected to directory server: " + serverIP + " at port " + serverPort + ".");

        // Read user input from keyboard
        Scanner scanner = new Scanner(System.in);
        String fromClient = scanner.next();

        pw = new PrintWriter(clientSocket.getOutputStream(), true);
        sc = new Scanner(clientSocket.getInputStream());

        String fileName;
        String replyMessage;

        while (true) {
            switch (fromClient.toLowerCase()) {
            case "inform":
                fileName = scanner.next();
                replyMessage = getInformMessage(fileName);
                System.out.println(replyMessage);
                break;
            case "query":
                fileName = scanner.next();
                int chunkNumber = 1;
                replyMessage = getQueryMessage(fileName, chunkNumber);
                System.out.println(replyMessage);
                break;
            case "download":
                fileName = scanner.next();
                replyMessage = getDownloadMessage(fileName);
                System.out.println(replyMessage);
                break;
            case "list":
                replyMessage = getListMessage();
                System.out.println(replyMessage);
                break;
            case "exit":
                replyMessage = getExitMessage();
                System.out.println(replyMessage);
                break;
            default:
                System.out.println("Invalid command.");
                break;
            }

            if (fromClient.toLowerCase().equals("exit")) {
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
