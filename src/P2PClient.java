import java.io.*;
import java.net.*;
import java.util.*;

public class P2PClient {

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    private String getInformMessage(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        char[] buffer = new char[1024];

        int chunkCount = 0;
        while (br.read(buffer) != 0) {
            chunkCount++;
        }

        String toServer = "INFORM\r\n" + fileName + "\r\n" + chunkCount;
        pw.println(toServer);

        return sc.nextLine();
    }

    private String getQueryMessage(String fileName, int chunkNumber) {

        String toServer = "QUERY\r\n" + fileName + "\r\n" + chunkNumber;
        pw.println(toServer);

        return sc.nextLine();
    }

    private String getDownloadMessage(String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int chunkNumber = 1;

        while (true) {
            String messageReceived = getQueryMessage(fileName, chunkNumber);

            if (messageReceived.contains("CHUNK NOT EXIST")) {
                break;
            }

            String p2pServerIP = messageReceived;
            int p2pServerPort = 9019;
            Socket socketToP2PServer = connectToServer(p2pServerIP, p2pServerPort);

            sendQueryToP2PServer(fileName, chunkNumber, socketToP2PServer);

            receiveDataFromP2PServer(bos, socketToP2PServer);

            socketToP2PServer.close();

            chunkNumber++;
        }

        bos.close();

        return "Reply: file " + fileName + " downloaded from peer server.";
    }

    private String getListMessage() {

        String toServer = "LIST";
        pw.println(toServer);

        return sc.nextLine();
    }

    private String getExitMessage() throws IOException {
        String toServer = "EXIT";
        pw.println(toServer);

        String messageReceived =  sc.nextLine();

        sendExitToOwnServer();

        return messageReceived;
    }

    private void sendExitToOwnServer() throws IOException {
        Socket socketToOwnServer = connectToServer("localhost", 9019);
        PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
        writerToOwnServer.println("EXIT");

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
        String messageReceived;

        while (true) {
            switch (fromClient.toLowerCase()) {
            case "inform":
                fileName = scanner.next();
                messageReceived = getInformMessage(fileName);
                System.out.println(messageReceived);
                break;
            case "query":
                fileName = scanner.next();
                int chunkNumber = 1;
                messageReceived = getQueryMessage(fileName, chunkNumber);
                System.out.println(messageReceived);
                break;
            case "download":
                fileName = scanner.next();
                messageReceived = getDownloadMessage(fileName);
                System.out.println(messageReceived);
                break;
            case "list":
                messageReceived = getListMessage();
                System.out.println(messageReceived);
                break;
            case "exit":
                messageReceived = getExitMessage();
                System.out.println(messageReceived);
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
