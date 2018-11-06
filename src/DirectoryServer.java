import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectoryServer implements Runnable {

    private HashMap<Chunk, List<Host>> firstTable;
    private HashMap<Host, List<Chunk>> secondTable;
    private Socket acceptedClientSocket;

    /**
     * Constructor
     */
    public DirectoryServer() {
        this.firstTable = new HashMap<>();
        this.secondTable = new HashMap<>();
    }

    public DirectoryServer(Socket acceptedClientSocket, HashMap<Chunk, List<Host>> firstTable,
                           HashMap<Host, List<Chunk>> secondTable) {
        this.acceptedClientSocket = acceptedClientSocket;
        this.firstTable = firstTable;
        this.secondTable = secondTable;
    }

    private String getAckMessage() {
        return Constant.MESSAGE_ACK + Constant.MESSAGE_DELIMITER;
    }

    private String getQueryReplyMessage(String filename, int chunkNumber) {

        Chunk chunk = new Chunk(filename, chunkNumber);
        String message = Constant.MESSAGE_REPLY + Constant.MESSAGE_DELIMITER;

        List<Host> listOfHosts = firstTable.get(chunk);

        if (listOfHosts == null || listOfHosts.isEmpty()) {

            // Chunk not exists
            return message + Constant.MESSAGE_CHUNK_NOT_EXIST + Constant.MESSAGE_DELIMITER;

        } else {

            System.out.println("The chunk exists!");

            int randomNumber = (int) (Math.random() * listOfHosts.size());

            Host randomlySelectedHost = listOfHosts.get(randomNumber);

            return message + randomlySelectedHost.getIPAddress() + Constant.MESSAGE_DELIMITER
                    + randomlySelectedHost.getPortNumber() + Constant.MESSAGE_DELIMITER;
        }

    }

    private String getListReplyMessage() {
        String listReplyMessage = Constant.MESSAGE_REPLY + Constant.MESSAGE_DELIMITER;

        Set<Chunk> chunksSet = firstTable.keySet();
        Set<String> filenames = chunksSet.stream()
                .map(chunk -> chunk.getFilename())
                .collect(Collectors.toSet());

        if (filenames.isEmpty()) {
            listReplyMessage += Constant.MESSAGE_FILE_LIST_EMPTY + Constant.MESSAGE_DELIMITER;
        } else {
            listReplyMessage += filenames.size();
            listReplyMessage += Constant.MESSAGE_DELIMITER;
            for (String filename : filenames) {
                listReplyMessage += filename;
                listReplyMessage += Constant.MESSAGE_DELIMITER;
            }
        }

        return listReplyMessage;
    }

    private String getGoodbyeMessage() {
        return Constant.MESSAGE_GOODBYE + Constant.MESSAGE_DELIMITER;
    }

    /**
     * Send TCP message to client
     */
    private void send(Socket client, String messageToSend) {
        try {
            System.out.println("Server is sending client: " + messageToSend);
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
            writer.println(messageToSend);

            if (messageToSend.contains(Constant.MESSAGE_GOODBYE)) {
                System.out.println("Closing the client socket...");
                client.close();
                System.out.println("Client is closed: " + client.isClosed());
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private void handleInformMsg(String filename, int chunkNumber,
                                 String clientPublicIp, int clientPublicPort) {
        String IPAddress = clientPublicIp;
        int portNumber = clientPublicPort;

        Host host = new Host(IPAddress, portNumber);
        Chunk chunk = new Chunk(filename, chunkNumber);

        //Add to the first table
        List<Host> availableHosts = firstTable.get(chunk);
        if (availableHosts == null) {
            availableHosts = new ArrayList<>();
        }
        availableHosts.add(host);
        firstTable.put(chunk, availableHosts);

        // Add to the second table
        List<Chunk> chunksOfTheHost = secondTable.get(host);
        if(chunksOfTheHost == null) {
            chunksOfTheHost = new ArrayList<>();
        }
        chunksOfTheHost.add(chunk);
        secondTable.put(host, chunksOfTheHost);
    }

    private void printFirstTableContent() {
        HashMap<Chunk, List<Host>> table = firstTable;
        String s = "";
        for (Chunk chunk: table.keySet()) {
            s += chunk.filename + " " + chunk.chunkNumber + "is at: \n";
            for(Host host: table.get(chunk)) {
                s += host.getIPAddress() + ":" + host.getPortNumber() + " ";
            }
            s += "\n";
        }
        System.out.println("\nFirst table content:");
        System.out.println(s);
    }

    private void printSecondTableContent() {
        HashMap<Host, List<Chunk>> table = secondTable;
        String s = "";
        for (Host host: table.keySet()) {
            s += host.getIPAddress() + ":" + host.getPortNumber() + " has:\n";
            for(Chunk chunk: table.get(host)) {
                s += chunk.filename + " " + chunk.chunkNumber + " ";
            }
            s += "\n";
        }
        System.out.println("Second table content:");
        System.out.println(s);
    }

    private void handleExitMsg(Socket client) {
        String clientIpAddress = client.getInetAddress().toString();
        int clientPort = Constant.P2P_SERVER_PORT;
        Host clientHost = new Host(clientIpAddress, clientPort);

        List<Chunk> clientChunks = secondTable.get(clientHost);

        // Remove client information from the first table
        if (clientChunks != null) {
            for (Chunk chunk : clientChunks) {
                List<Host> hostsForChunk = firstTable.get(chunk);

                if (hostsForChunk == null) {
                    continue;
                }
                hostsForChunk.remove(clientHost);

                if (hostsForChunk.isEmpty()) {
                    firstTable.remove(chunk);
                }
                // Remove client information from the second table
                secondTable.remove(clientHost);
            }
        }
    }

    private String handleClientMsg(Socket client, String[] parsedClientMsg) {

        String type = parsedClientMsg[0];
        System.out.println("Client message has type: " + type);
        String returnMessage;

        switch(type) {
            case Constant.COMMAND_INFORM:

                String filename = parsedClientMsg[1];
                int chunkNumber = Integer.parseInt(parsedClientMsg[2]);
                String clientPublicIp = client.getInetAddress().toString();
                int clientPublicPort = Constant.P2P_SERVER_PORT;
                handleInformMsg(filename, chunkNumber, clientPublicIp, clientPublicPort);
                returnMessage = getAckMessage();
                break;

            case Constant.COMMAND_QUERY:

                String filename2 = parsedClientMsg[1];
                int chunkNumber2 = Integer.parseInt(parsedClientMsg[2]);
                returnMessage = getQueryReplyMessage(filename2, chunkNumber2);
                break;

            case Constant.COMMAND_LIST:

                returnMessage = getListReplyMessage();
                break;

            case Constant.COMMAND_EXIT:

                handleExitMsg(client);
                returnMessage = getGoodbyeMessage();
                break;

            default:
                returnMessage = "This is not a supported operation.";
        }

        printFirstTableContent();
        printSecondTableContent();

        return returnMessage;

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

    private void handleClientSocket(Socket client) {
        String messageFromClient = getMsgFromClient(client);

        if (!messageFromClient.isEmpty()) {
            System.out.println("Parsing client message...");
            String[] parsedClientMsg = parse(messageFromClient);
            
            System.out.println("Preparing directory server reply...");
            String reply = handleClientMsg(client, parsedClientMsg);

            System.out.println("Sending directory server reply to" + clientIp(client));
            send(client, reply);
        }
    }

    private void startWelcomeSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(Constant.DIR_SERVER_PORT);
            System.out.println("The directory server is up and running...");

            while(true) {
                System.out.println("Waiting for new client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection request from " + clientIp(clientSocket));

                DirectoryServer newServer = new DirectoryServer(clientSocket, firstTable, secondTable);
                new Thread(newServer).start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private String clientIp(Socket socket) {
        return socket.getInetAddress().toString() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        System.out.println("New thread created to entertain the client" + clientIp(acceptedClientSocket) + "\n");
        while (!acceptedClientSocket.isClosed()) {
            handleClientSocket(acceptedClientSocket);
        }
        System.out.println(clientIp(acceptedClientSocket) + " exits...\n");
    }

    private String[] parse(String message) {
        return message.split(Constant.MESSAGE_DELIMITER);
    }

    public static void main(String[] args) {
        DirectoryServer directoryServer = new DirectoryServer();
        directoryServer.startWelcomeSocket();
    }

    private class Chunk {
        private String filename;

        private int chunkNumber;

        public Chunk(String filename, int chunkNumber) {
            this.filename = filename;
            this.chunkNumber = chunkNumber;
        }

        public String getFilename() {
            return filename;
        }

        public int getChunkNumber() {
            return chunkNumber;
        }

        @Override
        public boolean equals(Object other) {
            return other == this
                    || (other instanceof Chunk
                    && this.chunkNumber == ((Chunk) other).chunkNumber
                    && this.filename.equals(((Chunk) other).filename));

        }

        @Override
        public int hashCode() {
            return this.filename.hashCode() + this.chunkNumber * 17;
        }
    }

    private class Host {
        private String IPAddress;

        private int portNumber;

        public Host(String IPAddress, int portNumber) {
            this.IPAddress = IPAddress;
            this.portNumber = portNumber;
        }

        public String getIPAddress() {
            return IPAddress;
        }

        public int getPortNumber() {
            return portNumber;
        }

        @Override
        public boolean equals(Object other) {
            return other == this
                    || (other instanceof Host
                    && this.portNumber == ((Host) other).portNumber
                    && this.IPAddress.equals(((Host) other).IPAddress));
        }

        @Override
        public int hashCode() {
            return this.IPAddress.hashCode() + this.portNumber * 17;
        }
    }

}
