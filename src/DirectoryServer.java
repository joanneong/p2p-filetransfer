public class DirectoryServer {

    // TODO: tables in server

    private String getAckMessage() {
        return "";
    }

    private String getQueryReplyMessage(String fileName, int chunkNumber) {
        return "";
    }

    private String getListReplyMessage() {
        return "";
    }

    private String getGoodbyeMessage() {
        return "";
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

    public static void main(String[] args) {
        DirectoryServer directoryServer = new DirectoryServer();
        directoryServer.start();
    }

}
