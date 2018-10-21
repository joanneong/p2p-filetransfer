import java.util.ArrayList;
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

    private String getQueryReplyMessage(Chunk chunk) {

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
    private void send(String messageToSend, String IPAddress, int port) {

    }

    private void start() {
        while(true) {

        }
    }

    private String[] parse(String message) {
        List<String> parsedMessages = new ArrayList<>();
        String mainContent = message.split(Constant.DELIMITER + Constant.DELIMITER)[0];
        return mainContent.split(Constant.DELIMITER);
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
