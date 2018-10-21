import java.io.*;
import java.net.*;
import java.util.*;

public class P2PClient {

    private String getInformMessage(String fileName) {
        return "Reply: " + fileName + " informed to server.";
    }

    private String getQueryMessage(String fileName) {
        return "Reply: " + fileName + " queried to server.";
    }

    private String getDownloadMessage(String hostList) {
        return "Reply: file downloaded from peer.";
    }

    private String getListMessage() {
        return "Reply: files listed.";
    }

    private String getExitMessage() {
        return "Reply: goodbye.";
    }

    private void send(String messageToSend, String IPAddress, int port) {

    }

    private void start(String serverIP, int serverPort) throws IOException {

        // Create a client socket and connect to the server
        Socket clientSocket = new Socket(serverIP, serverPort);
        System.out.println("Connected to directory server: " + serverIP + " at port " + serverPort + ".");

        // Read user input from keyboard
        Scanner scanner = new Scanner(System.in);
        String fromClient = scanner.next();

        PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
        Scanner sc = new Scanner(clientSocket.getInputStream());

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
                replyMessage = getQueryMessage(fileName);
                System.out.println(replyMessage);
                break;
            case "download":
                fileName = scanner.next();
                replyMessage = getQueryMessage(fileName);
                replyMessage = getDownloadMessage(replyMessage);
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
