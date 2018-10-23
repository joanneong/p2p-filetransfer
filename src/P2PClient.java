import java.io.*;
import java.net.*;
import java.util.*;

public class P2PClient {

    private BufferedReader br;
    private char[] buffer;

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    private String getInformMessage(String fileName) throws IOException {

        br = new BufferedReader(new FileReader(fileName));
        buffer = new char[1024];

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

    private String getDownloadMessage(String hostList) {
        return "Reply: file downloaded from peer.";
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
        Socket socketToOwnServer = new Socket("localhost", 9019);
        PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
        writerToOwnServer.println("EXIT");

        Scanner scFromOwnServer = new Scanner(socketToOwnServer.getInputStream());
        if (!scFromOwnServer.nextLine().equals("ACK")) {
            System.out.println("Own host server is not closed!");
        }

        scFromOwnServer.close();
        socketToOwnServer.close();
    }

    private void send(String messageToSend, String IPAddress, int port) {

    }

    private void start(String serverIP, int serverPort) throws IOException {

        // Create a client socket and connect to the server
        clientSocket = new Socket(serverIP, serverPort);
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
