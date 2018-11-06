import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class P2PTransientServer implements Runnable {

    private Socket acceptedConnectionSocket;

    /**
     * Constructors
     */
    public P2PTransientServer() {

    }

    public P2PTransientServer(Socket connectionSocket) {
        acceptedConnectionSocket = connectionSocket;
    }

    public static void main(String[] args) {
        new File(Constant.DEFAULT_DIRECTORY).mkdirs();

        P2PTransientServer serverInstance = new P2PTransientServer();
        serverInstance.start(Constant.P2P_SERVER_PORT);
    }

    private void start(int port) {

        try{
            ServerSocket welcomeSocket = new ServerSocket (port);
            System.out.println("P2P transient server running on port 9019...\n");

            while (true) {
                Socket connectionSocket = welcomeSocket.accept();

                P2PTransientServer newServerInstance = new P2PTransientServer(connectionSocket);
                new Thread(newServerInstance).start();
            }

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("New thread created to entertain the client " + clientIp(acceptedConnectionSocket));
        boolean isExit = handleClientSocket(acceptedConnectionSocket);
        System.out.println(clientIp(acceptedConnectionSocket) + " exits...\n");
        if (isExit) {

            // Print GoodBye message and exit the program with client
            System.out.println("P2P transient server closed. Goodbye!");
            System.exit(0);
        }
    }

    /**
     * Handles requests sent by a client
     * @param  client Socket that handles the client connection
     * @return a boolean that shows whether we want to close this server.
     */
    private boolean handleClientSocket(Socket client) {

        InputStreamReader isr;
        BufferedReader br;

        String msgType;
        String fileName;
        String chunkNumString;
        int chunkNum;

        boolean isExit = false;

        try {
            isr = new InputStreamReader(client.getInputStream());
            br = new BufferedReader(isr);
            msgType = br.readLine();

            if (msgType.equals(Constant.COMMAND_EXIT)) {

                byte[] buffer = Constant.MESSAGE_ACK.getBytes();
                System.out.println("Requested to exit by own client");
                sendP2PResponse(client, buffer);
                isExit = true;

            } else if (msgType.equals(Constant.COMMAND_QUERY)) {

                fileName = br.readLine();
                chunkNumString = br.readLine();
                chunkNum = Integer.parseInt(chunkNumString);

                sendP2PResponse(client, formP2PResponse(fileName, chunkNum));
            }

            client.close();

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        return isExit;
    }

    /**
     * Sends a response back to the client
     * @param  client Socket that handles the client connection
     * @param  response The response to be sent to the client
     */
    private void sendP2PResponse(Socket client, byte[] response) {
	  try {
          DataOutputStream output = new DataOutputStream(client.getOutputStream());
          output.write(response);
          output.flush();

          output.close();
      } catch (IOException ioe) {
          System.out.println(ioe.getMessage());
      }
    }

    /**
     * Form a response to a P2PRequest
     * @param  fileName The name of file being requested
     * @param  chunkNum The chunk number of the file
     * @return a byte[] that contains the data to be sent to the client
     */
    private byte[] formP2PResponse(String fileName, int chunkNum) {

        int bytesRead; // the number of bytes read by buffer

        try{
            String directoryPath = Constant.DEFAULT_DIRECTORY;

            RandomAccessFile file = new RandomAccessFile(directoryPath + fileName, "r");
            file.seek(Constant.CHUNK_SIZE * (chunkNum - 1)); // move the pointer to the position where we start reading
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            bytesRead = file.read(buffer);

            System.out.println("Requested file: " + fileName + "; chunk No." + chunkNum + "; length: " + bytesRead);

            file.close();

            if (bytesRead == Constant.CHUNK_SIZE) {
                return buffer;
            } else { // it is the case for the last packet
                return Arrays.copyOfRange(buffer, 0, bytesRead);
            }

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return new byte[Constant.CHUNK_SIZE];
        }
    }

    private String clientIp(Socket socket) {
        return socket.getInetAddress().toString() + ":" + socket.getPort();
    }

}
