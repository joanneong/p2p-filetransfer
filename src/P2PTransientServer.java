import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class P2PTransientServer implements Runnable {

    String directoryServerIP;
    int directoryServerPort;
    String uniqueName;

    Socket transientServerSocket;

    PrintWriter pw;
    Scanner sc;

    String messageReceived;

    // Constructor function for P2P transient server
    public P2PTransientServer(String directoryServerIP, int directoryServerPort, String uniqueName) {
        this.directoryServerIP = directoryServerIP;
        this.directoryServerPort = directoryServerPort;
        this.uniqueName = uniqueName;
    }

    // Send a P2P transient server's unique name to the directory server during the initial connection
    // This unique name is the same unique name as the P2P client which it is associated with
    private void sendNameMessage() {
        String toServer = Constant.COMMAND_NAME + Constant.MESSAGE_DELIMITER
                + uniqueName + Constant.MESSAGE_DELIMITER
                + Constant.TYPE_TRANSIENT_SOCKET + Constant.MESSAGE_DELIMITER;
        pw.println(toServer);
        pw.flush();

        sc.nextLine();
        messageReceived =  sc.nextLine();

        if (messageReceived.equals(Constant.MESSAGE_ACK)) {
            System.out.println("Unique name " + uniqueName + " successfully sent from transient server to directory server!");
        } else {
            System.err.println("Error: could not send unique name " + uniqueName + " to directory server!");
        }
    }

    public void start() throws IOException {

        // Create a transient server socket and connect to the directory server
        transientServerSocket = new Socket(directoryServerIP, directoryServerPort);
        System.out.println("P2P transient server connected to directory server: " + directoryServerIP
                + " at port " + directoryServerPort + Constant.MESSAGE_DELIMITER);

        // Open writer and scanner between p2p transient server and directory server
        pw = new PrintWriter(transientServerSocket.getOutputStream(), true);
        sc = new Scanner(transientServerSocket.getInputStream());

        // Inform the directory server that this connection is to a P2P transient server
        sendNameMessage();

        try{
            Scanner fromClient = new Scanner(transientServerSocket.getInputStream());

            // Open writer and scanner between p2p transient server and directory server
            pw = new PrintWriter(transientServerSocket.getOutputStream(), true);
            sc = new Scanner(transientServerSocket.getInputStream());

            while (true) {
                // Print public IP address of the client requesting for data
                System.out.println("Data requested by " + messageReceived);

                String[] temp = fromClient.nextLine().split(":");
                String fileName = temp[0];
                int chunkNumber = Integer.parseInt(temp[1]);

                fromClient.nextLine();

                byte[] messageRequested = getRequestMessage(fileName, chunkNumber);
                sendP2PResponse(transientServerSocket, messageRequested);

                System.out.println("Sent " + fileName + " chunk " + chunkNumber);

                messageReceived = fromClient.nextLine();
            }

            // System.out.println("P2P transient server closed. Goodbye!");

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private byte[] getRequestMessage(String fileName, int chunkNum) {

        int bytesRead; // the number of bytes read by buffer

        try{
            RandomAccessFile file = new RandomAccessFile(Constant.DEFAULT_DIRECTORY + fileName, "r");
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

    private void sendP2PResponse(Socket serverSocket, byte[] response) {
	  try {
	      DataOutputStream toClient = new DataOutputStream(serverSocket.getOutputStream());
          toClient.write(response);
          toClient.flush();

          System.out.println("Data requested has been sent" + Constant.MESSAGE_DELIMITER);

          toClient.close();
      } catch (IOException ioe) {
          System.out.println(ioe.getMessage());
      }
    }

    @Override
    public void run() {
        System.out.println("New thread created to entertain the client " + clientIp(acceptedSocket) + "\n");
        while (!acceptedSocket.isClosed()) {
            handleClientSocket(acceptedSocket);
        }
        System.out.println(uniqueName + " " + clientIp(acceptedSocket) + " exits...\n");
    }

    public static void main(String[] args) {

        // Check if the number of command line argument is 3
        if (args.length != 3) {
            System.err.println("Usage: java P2PTransientServer directoryServerIP directoryServerPort uniqueName");
            System.exit(1);
        }

        String directoryServerIP = args[0];
        int directoryServerPort = Integer.parseInt(args[1]);
        String uniqueName = args[2];

        try {
            P2PTransientServer transientServer = new P2PTransientServer(directoryServerIP, directoryServerPort, uniqueName);
            transientServer.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
