import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class P2PClient {

    String directoryServerIP;
    int directoryServerPort;
    String uniqueName;

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    // Constructor function for a new P2P client
    P2PClient(String directoryServerIP, int directoryServerPort, String uniqueName) {
        this.directoryServerIP = directoryServerIP;
        this.directoryServerPort = directoryServerPort;
        this.uniqueName = uniqueName;
    }

    // Send a P2P client's unique name to the directory server during the initial connection
    private void sendNameMessage() {
        String toServer = Constant.COMMAND_NAME + Constant.MESSAGE_DELIMITER
                + uniqueName + Constant.MESSAGE_DELIMITER
                + Constant.TYPE_CLIENT_SOCKET + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        String[] message = getMessageFromClientSocket(clientSocket);

        if (message[0].equals(Constant.MESSAGE_ACK)) {
            System.out.println("Unique name " + uniqueName + " successfully sent from client to directory server!");
        } else {
            System.err.println("Error: could not send unique name " + uniqueName + " to directory server!");
        }
    }

    // Get the total number of chunks in a file
    private int getNumberOfChunks(String fileName) {

        File f = new File(Constant.DEFAULT_DIRECTORY + fileName);
        int fileLength = (int) f.length();
        int chunkCount = fileLength / Constant.CHUNK_SIZE;

        if (fileLength > (chunkCount * Constant.CHUNK_SIZE)) {
            chunkCount = chunkCount + 1;
        }
        return chunkCount;
    }

    // Inform the directory server that the client owns a file and chunk
    private String sendInformMessage(String fileName, int chunkNumber) {

        String toServer = Constant.COMMAND_INFORM + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        String[] message = getMessageFromClientSocket(clientSocket);
        System.out.println("Inform chunk " + chunkNumber + " to directory server: " + message[0]);

        if (message[0].equals(Constant.MESSAGE_ACK)) {
            return "File " + fileName + " chunk " + chunkNumber + " informed to directory server";
        } else {
            return Constant.ERROR_CLIENT_INFORM_FAILED;
        }
    }

    // Ask the directory server for a list of public IP addresses of peers who own the file
    private String sendQueryMessage(String fileName, int chunkNumber) {

        String toServer = Constant.COMMAND_QUERY + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        String[] message = getMessageFromClientSocket(clientSocket);

        if (message[0].equals(Constant.MESSAGE_CHUNK_NOT_EXIST)) {
            return Constant.ERROR_QUERY_FILE_NOT_EXIST;
        } else {
            String p2pServerIP = message[0];
            String p2pServerPort = message[1];

            return "File " + fileName + " found at port " + p2pServerPort + " of P2P server "
                    + p2pServerIP + Constant.MESSAGE_DELIMITER;
        }
    }

    // Download a file from another peer via the directory server
    private String sendDownloadMessage(String fileName) throws IOException {

        // Check if the client already owns the file
        boolean isExist = new File(Constant.DEFAULT_DIRECTORY + fileName).exists();
        if (isExist) {
            return Constant.ERROR_DOWNLOAD_FILE_EXIST;
        }

        String toServer = Constant.COMMAND_DOWNLOAD + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        // Get the total number of chunks to expect from the directory server
        String[] message = getMessageFromClientSocket(clientSocket);
        int totalChunksToReceive = Integer.parseInt(message[0]);

        // Check if the file exists (to the directory server's knowledge)
        if (totalChunksToReceive == 0) {
            File file = new File(Constant.DEFAULT_DIRECTORY + fileName);
            file.delete();
            return Constant.ERROR_DOWNLOAD_FILE_NOT_EXIST;
        }

        // Prepare to write received file contents
        FileOutputStream fos = new FileOutputStream(Constant.DEFAULT_DIRECTORY + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        // Write file contents to new file
        for (int i = 1; i <= totalChunksToReceive; i++) {
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            int bytesRead = clientSocket.getInputStream().read(buffer);

            bos.write(buffer, 0, bytesRead);
            bos.flush();

            System.out.println("Downloaded " + fileName + " chunk " + i);
        }

        bos.close();

        // Only inform the directory server after the whole file has been downloaded
        for (int i = 1; i <= totalChunksToReceive; i++) {
            sendInformMessage(fileName, i);
        }
        return "File " + fileName + " downloaded from peer server" + Constant.MESSAGE_DELIMITER;
    }

    // Get a list of all available files from the directory server
    private String sendListMessage() {

        String toServer = Constant.COMMAND_LIST;
        pw.println(toServer);
        pw.flush();

        StringBuilder replyMessage = new StringBuilder();
        replyMessage.append("File list:").append(Constant.MESSAGE_DELIMITER);

        String[] message = getMessageFromClientSocket(clientSocket);

        if (message[1].equals(Constant.MESSAGE_FILE_LIST_EMPTY)) {
            replyMessage.append("There is no file available").append(Constant.MESSAGE_DELIMITER);
        } else {
            int fileCount = Integer.parseInt(message[1]);
            for (int i = 0; i < fileCount; i++) {
                replyMessage.append(message[i + 2]).append(Constant.MESSAGE_DELIMITER);
            }
        }

        return replyMessage.toString();
    }

    // Send exit message to the directory server
    private void sendExitMessage() {
        String toServer = Constant.COMMAND_EXIT;
        pw.println(toServer);
        pw.flush();
    }

    private void start() throws IOException {

        // Create a client socket and connect to the directory server
        clientSocket = new Socket(directoryServerIP, directoryServerPort);
        System.out.println("P2P client connected to directory server: " + directoryServerIP + " at port " + directoryServerPort);

        // Open writer and scanner between p2p client and directory server
        pw = new PrintWriter(clientSocket.getOutputStream(), true);
        sc = new Scanner(clientSocket.getInputStream());

        // Inform the directory server that this connection is to a P2P client
        sendNameMessage();

        // Read user input from keyboard
        Scanner scanner = new Scanner(System.in);
        String fromClient = scanner.next();

        String fileName;
        String replyMessage;
        int chunkNumber;

        while (true) {
            switch (fromClient.toUpperCase()) {
                case Constant.COMMAND_INFORM:
                    fileName = scanner.next();
                    chunkNumber = getNumberOfChunks(fileName);

                    System.out.println("Attempting to inform " + fileName + " with no. of chunks: " + chunkNumber);

                    if (chunkNumber <= 0) {
                        System.out.println(Constant.ERROR_INFORM_FILE_NOT_EXIST);
                        break;
                    }

                    replyMessage = "File " + fileName + " successfully informed to directory server\n";
                    String tempMessage;
                    for (int i = 1; i <= chunkNumber; i++) {
                        tempMessage = sendInformMessage(fileName, i);
                        if (tempMessage.equals(Constant.ERROR_CLIENT_INFORM_FAILED)) {
                            replyMessage = Constant.ERROR_CLIENT_INFORM_FAILED;
                            break;
                        }
                    }
                    System.out.println(replyMessage);
                    break;

                case Constant.COMMAND_QUERY:
                    fileName = scanner.next();
                    chunkNumber = 1;
                    replyMessage = sendQueryMessage(fileName, chunkNumber);
                    System.out.println(replyMessage);
                    break;

                case Constant.COMMAND_DOWNLOAD:
                    fileName = scanner.next();
                    replyMessage = sendDownloadMessage(fileName);
                    System.out.println(replyMessage);
                    break;

                case Constant.COMMAND_LIST:
                    replyMessage = sendListMessage();
                    System.out.println(replyMessage);
                    break;

                case Constant.COMMAND_EXIT:
                    sendExitMessage();
                    System.out.println(Constant.MESSAGE_GOODBYE);
                    break;

                default:
                    System.out.println(Constant.ERROR_INVALID_COMMAND);
                    scanner.nextLine();
                    break;
            }

            if (fromClient.toUpperCase().equals(Constant.COMMAND_EXIT)) {
                scanner.close();
                sc.close();
                pw.close();
                clientSocket.close();
                break;
            }

            fromClient = scanner.next();
        }
    }

    public static void main(String[] args) {

        // Check if the number of command line argument is 3
        if (args.length != 3) {
            System.err.println("Usage: java P2PClient directoryServerIP directoryServerPort uniqueName");
            System.exit(1);
        }

        String directoryServerIP = args[0];
        int directoryServerPort = Integer.parseInt(args[1]);
        String uniqueName = args[2];

        try {
            P2PClient client = new P2PClient(directoryServerIP, directoryServerPort, uniqueName);
            client.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String[] getMessageFromClientSocket(Socket client) {
        String messageFromClient = getMsgFromClient(client);

        String[] parsedClientMsg = parse(messageFromClient);

        return parsedClientMsg;
    }

    private String[] parse(String message) {
        return message.split(Constant.MESSAGE_DELIMITER);
    }

    // Read in a message from a P2P client
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

}
