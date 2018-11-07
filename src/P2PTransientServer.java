import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class P2PTransientServer {

    Socket serverSocket;

    public P2PTransientServer(Socket socket) {
        serverSocket = socket;
    }

    public void start() {

        try{
            Scanner fromClient = new Scanner(serverSocket.getInputStream());

            String messageReceived = fromClient.nextLine();

            while (true) {
                // print public IP address of the client requesting for data
                System.out.println("Data requested by " + messageReceived);

                String[] temp = fromClient.nextLine().split(":");
                String fileName = temp[0];
                int chunkNumber = Integer.parseInt(temp[1]);

                fromClient.nextLine();

                byte[] messageRequested = getRequestMessage(fileName, chunkNumber);
                sendP2PResponse(serverSocket, messageRequested);

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

}
