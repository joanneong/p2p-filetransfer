import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class DirectoryServer implements Runnable {

    private HashMap<Chunk, List<Host>> firstTable;
    private HashMap<Host, List<Chunk>> secondTable;
    private Socket acceptedSocket;
    private HashMap<String, Host> hosts;
    private String uniqueName;
    private HashMap<String, Boolean> semaphores; // Mapping: unique name & semaphore

    /**
     * Constructors
     */
    public DirectoryServer() {
        this.firstTable = new HashMap<>();
        this.secondTable = new HashMap<>();
        this.hosts = new HashMap<>();
        this.semaphores = new HashMap<>();
    }

    public DirectoryServer(Socket acceptedSocket, HashMap<Chunk, List<Host>> firstTable,
                           HashMap<Host, List<Chunk>> secondTable, HashMap<String, Host> hosts,
                           HashMap<String, Boolean> semaphores) {
        this.acceptedSocket = acceptedSocket;
        this.firstTable = firstTable;
        this.secondTable = secondTable;
        this.hosts = hosts;
        this.semaphores = semaphores;
    }

//======================================================================================================================
//=============================== Functions for handling various COMMANDs ==============================================

    private String getQueryReplyMessage(String filename) {

        String message = Constant.MESSAGE_REPLY + Constant.MESSAGE_DELIMITER;

        Host randomlySelectedHost = getRandomHost(filename, 1);

        if (randomlySelectedHost == null) {

            // Chunk requested does not exist
            return message + Constant.MESSAGE_CHUNK_NOT_EXIST + Constant.MESSAGE_DELIMITER;

        } else {
            return message
                    + randomlySelectedHost.getTransientServerSocket().getInetAddress()
                    + Constant.MESSAGE_DELIMITER
                    + randomlySelectedHost.getTransientServerSocket().getPort()
                    + Constant.MESSAGE_DELIMITER;
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

    private String getAckMessage() {
        return Constant.MESSAGE_ACK + Constant.MESSAGE_DELIMITER;
    }

    // Store a connection request from a P2PClient / P2P Transient Server to the directory server
    private void handleNameMsg(Socket socket, String uniqueName, String type) {
        // Set unique name of this thread
        if(this.uniqueName == null) {
            this.uniqueName = uniqueName;
        } else {
            // This should not happen
            System.err.println("The unique name of this thread already exists");
        }

        // Check whether host exists in the host list
        Host host = hosts.get(uniqueName);
        if (host == null) {
            host = new Host();
            host.setUniqueName(uniqueName);
        }

        // Set sockets
        switch (type) {
            case Constant.TYPE_CLIENT_SOCKET:
                host.setClientSocket(socket);
                break;
            case Constant.TYPE_TRANSIENT_SOCKET:
                host.setTransientServerSocket(socket);
                break;
            default:
                System.err.println("Invalid socket type");
        }

        hosts.put(uniqueName, host);
    }

    private void handleInformMsg(String filename, int chunkNumber) {

        Host host = getHostOfCurrentThread();
        Chunk chunk = new Chunk(filename, chunkNumber);

        // Add to the first table
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

    private void handleDownloadFile(String filename) {
        // Send number of chunk to client
        int numOfChunk = getNumOfChunk(filename);
        send(acceptedSocket, "" + numOfChunk);

        for(int i = 1; i <= numOfChunk; i++) {
            // Pick a host that have the chunk of the file
            Host randomlySelectedHost = getRandomHost(filename, i);
            Socket transientSocket = randomlySelectedHost.getTransientServerSocket();

            // Send DOWNLOAD command to the transient server
            String command = Constant.COMMAND_DOWNLOAD + Constant.MESSAGE_DELIMITER
                             + filename + Constant.MESSAGE_DELIMITER
                             + i + Constant.MESSAGE_DELIMITER
                             + getUniqueNameOfThread() + Constant.MESSAGE_DELIMITER;
            send(transientSocket, command);

            // Set up semaphore
            setSemaphore(getUniqueNameOfThread());

            // Wait for semaphore
            while(!isSemaphoreReleased(getUniqueNameOfThread())) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleUploadFile(String clientName, Socket transientServerSocket) {
        // Get the target client
        Host clientHost = getHost(clientName);
        Socket clientSocket = clientHost.getClientSocket();

        byte[] buffer = new byte[Constant.CHUNK_SIZE];
        try {
            // Receive data from transient server
            int bytesRead = transientServerSocket.getInputStream().read(buffer);
            if (bytesRead <= 0) { // Error when reading
                System.err.println("Bytes read from transient server to " + clientName + " is " + bytesRead);
            }
            if (bytesRead != Constant.CHUNK_SIZE) {
                // This is the last packet
                buffer = Arrays.copyOfRange(buffer, 0, bytesRead);
            }

            // Send data back to client
            DataOutputStream toClient = new DataOutputStream(clientSocket.getOutputStream());
            toClient.write(buffer);
            toClient.flush();
            toClient.close();

            releaseSemaphore(getUniqueNameOfThread());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleExitMsg() {

        Host clientHost = getHostOfCurrentThread();

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

        // Close sockets of client
        try {
            clientHost.getTransientServerSocket().close();
            clientHost.getClientSocket().close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        // Remove host from hosts table
        hosts.remove(getUniqueNameOfThread());
    }

//======================================================================================================================
//======================================= Functions for handling socket ================================================

    private String handleClientMsg(Socket client, String[] parsedClientMsg) {

        String type = parsedClientMsg[0];
        System.out.println("Client message has type: " + type);
        String returnMessage;

        switch(type) {
            case Constant.COMMAND_NAME:
                String uniqueName = parsedClientMsg[1];
                String socketType = parsedClientMsg[2];
                handleNameMsg(client, uniqueName, socketType);
                returnMessage = getAckMessage();
                break;

            case Constant.COMMAND_INFORM:
                String filename = parsedClientMsg[1];
                int chunkNumber = Integer.parseInt(parsedClientMsg[2]);

                handleInformMsg(filename, chunkNumber);
                returnMessage = getAckMessage();
                break;

            case Constant.COMMAND_QUERY:
                String filename1 = parsedClientMsg[1];

                returnMessage = getQueryReplyMessage(filename1);
                break;

            case Constant.COMMAND_DOWNLOAD:
                String filename2 = parsedClientMsg[1];

                handleDownloadFile(filename2);

                returnMessage = ""; // Empty message won't be sent
                break;

            case Constant.COMMAND_UPLOAD:
                String uniqueName2 = parsedClientMsg[1];

                handleUploadFile(uniqueName2, client);

                returnMessage = ""; // Empty message won't be sent
                break;

            case Constant.COMMAND_LIST:
                returnMessage = getListReplyMessage();
                break;

            case Constant.COMMAND_EXIT:
                // Send good bye message first
                returnMessage = getGoodbyeMessage();

                // Then close the socket
                handleExitMsg();
                break;

            default:
                returnMessage = Constant.ERROR_INVALID_COMMAND;
        }

        return returnMessage;

    }

    private void handleClientSocket(Socket client) {
        String messageFromClient = getMsgFromClient(client);

        if (!messageFromClient.isEmpty()) {
            System.out.println("Parsing client message...");
            String[] parsedClientMsg = parse(messageFromClient);
            
            System.out.println("Preparing directory server reply...");
            String reply = handleClientMsg(client, parsedClientMsg);

            System.out.println("Sending directory server reply to " + clientIp(client));
            send(client, reply);

            printAllTables();
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

                DirectoryServer newServer = new DirectoryServer(clientSocket, firstTable,
                                                                secondTable, hosts, semaphores);
                new Thread(newServer).start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("New thread created to entertain the client " + clientIp(acceptedSocket) + "\n");
        while (!acceptedSocket.isClosed()) {
            handleClientSocket(acceptedSocket);
        }
        System.out.println("Thread for " + uniqueName + " " + clientIp(acceptedSocket) + " is close...\n");
    }

    public static void main(String[] args) {
        DirectoryServer directoryServer = new DirectoryServer();
        directoryServer.startWelcomeSocket();
    }

//======================================================================================================================
//======================================= Helper classes and functions =================================================

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

            scanner.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return messageFromClient;
    }

    // Send TCP message to client
    private void send(Socket client, String messageToSend) {

        // Empty message won't be sent
        if(messageToSend.isEmpty()) {
            return;
        }

        try {
            System.out.println("Server is sending client: " + messageToSend);
            PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
            writer.println(messageToSend);
            writer.close();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    // Find the total number of chunks for a file with the given filename
    private int getNumOfChunk(String fileName) {
        int chunk = 0;
        while(true) {
            if(!getHostsOfChunk(fileName, chunk + 1).isEmpty()) {
                chunk++;
            } else {
                return chunk;
            }
        }
    }

    private List<Host> getHostsOfChunk(String filename, int chunkNum) {
        Chunk chunk = new Chunk(filename, chunkNum);
        return firstTable.get(chunk);
    }

    private Host getRandomHost(String filename, int chunk) {
        List<Host> listOfHosts = getHostsOfChunk(filename, chunk);
        int randomNumber = (int) (Math.random() * listOfHosts.size());
        return listOfHosts.get(randomNumber);
    }

    private Host getHostOfCurrentThread() {
        Host host = hosts.get(getUniqueNameOfThread());
        if(host == null) {
            System.err.println("Host does not exist");
        }
        return host;
    }

    private String getUniqueNameOfThread() {
        if(uniqueName == null) {
            System.err.println("Unique name is null!");
        }
        return uniqueName;
    }

    private void setSemaphore(String uniqueName) {
        System.out.println("Set semaphore of " + getUniqueNameOfThread());
        semaphores.put(uniqueName, true);
    }

    private Boolean isSemaphoreReleased(String uniqueName) {
        Boolean isReleased = semaphores.get(getUniqueNameOfThread());
        if(isReleased == null) {
            System.err.println("Semaphore of " + uniqueName + " is null!");
        }
        return isReleased;
    }

    private void releaseSemaphore(String uniqueName) {
        System.out.println("Release semaphore of " + getUniqueNameOfThread());
        semaphores.put(uniqueName, false);
    }

    private void printAllTables() {
        printAllHosts();
        printFirstTableContent();
        printSecondTableContent();
    }

    private void printAllHosts() {
        System.out.println("\n All hosts:\n");
        System.out.println(Arrays.toString(hosts.values().toArray()));
    }

    private void printFirstTableContent() {
        HashMap<Chunk, List<Host>> table = firstTable;
        String s = "";
        for (Chunk chunk: table.keySet()) {
            s += chunk.filename + " " + chunk.chunkNumber + "is at: \n";
            for(Host host: table.get(chunk)) {
                s += host.getUniqueName();
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
            s += host.getUniqueName() + " has:\n";
            for(Chunk chunk: table.get(host)) {
                s += chunk.filename + " " + chunk.chunkNumber + " ";
            }
            s += "\n";
        }
        System.out.println("Second table content:");
        System.out.println(s);
    }

    private String clientIp(Socket socket) {
        return socket.getInetAddress().toString() + ":" + socket.getPort();
    }

    private Host getHost(String name) {
        Host host = hosts.get(name);
        if(host == null) {
            System.err.println("Host " + name + " does not exist");
        }
        return host;
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

        private String uniqueName;
        private Socket clientSocket;
        private Socket transientServerSocket;

        public Host() { }

        public String getUniqueName() {
            return uniqueName;
        }

        public void setUniqueName(String uniqueName) {
            if(this.uniqueName == null) {
                this.uniqueName = uniqueName;
                System.out.println("Unique name is set: " + uniqueName);
            } else {
                System.err.println("Unique name already set!");
            }
        }

        public Socket getClientSocket() {
            return clientSocket;
        }

        public void setClientSocket(Socket clientSocket) {
            if(this.clientSocket == null) {
                this.clientSocket = clientSocket;
                System.out.println("Set client socket of " + uniqueName);
            } else {
                System.err.println("Client socket already exists!");
            }
        }

        public Socket getTransientServerSocket() {
            return transientServerSocket;
        }

        public void setTransientServerSocket(Socket transientServerSocket) {
            if(this.transientServerSocket == null) {
                this.transientServerSocket = transientServerSocket;
                System.out.println("Set transient socket of " + uniqueName);
            } else {
                System.err.println("Transient socket already exists!");
            }
        }

        @Override
        public boolean equals(Object other) {
            return other == this
                    || (other instanceof Host
                    && this.uniqueName.equals(((Host) other).uniqueName));
        }

        @Override
        public int hashCode() {
            return this.uniqueName.hashCode();
        }

        @Override
        public String toString() {
            return "Name: " + uniqueName
                    + "Client: " + clientIp(clientSocket)
                    + "Transient: " + clientIp(transientServerSocket);
        }
    }

}
