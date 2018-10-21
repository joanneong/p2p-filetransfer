import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectoryServer {

    private HashMap<Chunk, List<Host>> firstTable;
    private HashMap<Host, List<Chunk>> secondTable;

    /**
     * Constructor
     */
    public DirectoryServer() {
        this.firstTable = new HashMap<>();
        this.secondTable = new HashMap<>();
    }

    private String getAckMessage() {
        return Constant.ACK + Constant.DELIMITER + Constant.DELIMITER;
    }

    private String getQueryReplyMessage(String filename, int chunkNumber) {

        Chunk chunk = new Chunk(filename, chunkNumber);
        String message = Constant.REPLY + Constant.DELIMITER;

        List<Host> listOfHosts = firstTable.get(chunk);

        if (listOfHosts.size() == 0) {

            // Chunk not exists
            return message + Constant.CHUNK_NOT_EXIST + Constant.DELIMITER + Constant.DELIMITER;

        } else {

            int randomNumber = (int) Math.random() * listOfHosts.size();

            Host randomlySelectedHost = listOfHosts.get(randomNumber);

            return message + randomlySelectedHost.getIPAddress() + Constant.DELIMITER
                    + randomlySelectedHost.getPortNumber() + Constant.DELIMITER
                    + Constant.DELIMITER;
        }

    }

    private String getListReplyMessage() {
        String listReplyMessage = Constant.REPLY + Constant.DELIMITER;

        Set<Chunk> chunksSet = firstTable.keySet();
        Set<String> filenames = chunksSet.stream()
                .map(chunk -> chunk.getFilename())
                .collect(Collectors.toSet());

        for (String filename : filenames) {
            listReplyMessage += filename;
            listReplyMessage += Constant.DELIMITER;
        }
        listReplyMessage += Constant.DELIMITER;

        return listReplyMessage;
    }

    private String getGoodbyeMessage() {
        return Constant.GOODBYE + Constant.DELIMITER + Constant.DELIMITER;
    }



    /**
     * Send TCP message to client
     */
    private void send(Socket client, String messageToSend) {

    }

    private void handleInformMsg(Socket client, String filename, int chunkNumber) {
        // TODO
    }

    private void handleExitMsg(Socket client) {
        String clientIpAddress = client.getInetAddress().toString();
        int clientPort = client.getPort();
        Host clientHost = new Host(clientIpAddress, clientPort);

        List<Chunk> clientChunks = secondTable.get(clientHost);

        // Remove client information from the first table
        for (Chunk chunk : clientChunks) {
            List<Host> hostsForChunk = firstTable.get(chunk);
            hostsForChunk.remove(clientHost);

            if (hostsForChunk.isEmpty()) {
                firstTable.remove(chunk);
            }
        }

        // Remove client information from the second table
        secondTable.remove(clientHost);
    }

    private String handleClientMsg(Socket client, String[] parsedClientMsg) {

        String type = parsedClientMsg[0];

        switch(type) {
            case Constant.INFORM:

                String filename = parsedClientMsg[1];
                int chunkNumber = Integer.parseInt(parsedClientMsg[2]);
                handleInformMsg(client, filename, chunkNumber);
                return getAckMessage();

            case Constant.QUERY:

                String filename2 = parsedClientMsg[1];
                int chunkNumber2 = Integer.parseInt(parsedClientMsg[2]);
                return getQueryReplyMessage(filename2, chunkNumber2);

            case Constant.LIST:

                return getListReplyMessage();

            case Constant.EXIT:

                handleExitMsg(client);
                return getGoodbyeMessage();

            default:
                return "This is not a supported operation.";
        }

    }

    private String getMsgFromClient(Socket client) {
        String messageFromClient = "";

        try {
            BufferedReader scanner = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String nextLine = scanner.readLine();

            while (nextLine != null) {
                messageFromClient += nextLine;
                nextLine = scanner.readLine();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return messageFromClient;
    }

    private void handleClientSocket(Socket client) {
            String messageFromClient = getMsgFromClient(client);

            String[] parsedClientMsg = parse(messageFromClient);
            String reply = handleClientMsg(client, parsedClientMsg);
            send(client, reply);
    }

    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(Constant.DIR_SERVER_PORT);

            while(true) {
                System.out.println("The directory server is up and running...");

                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection request from a client!");

                handleClientSocket(clientSocket);
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private String[] parse(String message) {
        return message.split(Constant.DELIMITER);
    }

    public static void main(String[] args) {
        DirectoryServer directoryServer = new DirectoryServer();
        directoryServer.start();
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
                    || (other instanceof Host
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
