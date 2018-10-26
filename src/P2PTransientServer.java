import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

public class P2PTransientServer {

    public static void main(String[] args) {
        int port = Constant.P2P_SERVER_PORT; // fixed port number

        P2PTransientServer serverInstance = new P2PTransientServer();
        serverInstance.start(port);
    }

    private void start(int port) {

        boolean flag = true;

        try{
            ServerSocket welcomeSocket = new ServerSocket (port);

            while (flag) {
                Socket connectionSocket = welcomeSocket.accept();
                flag = handleClientSocket(connectionSocket);
            }

        } catch (IOException ioe) {

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
        String fileName;
        int chunkNum;

        try {
            isr = new InputStreamReader(client.getInputStream());
            br = new BufferedReader(isr);
            fileName = br.readLine();
            if (fileName.equals(Constant.COMMAND_EXIT)) { // we assume file name cannot be "EXIT"
                byte[] buffer = Constant.MESSAGE_ACK.getBytes();
                sendP2PResponse(client, buffer);
                return false;
            }
            chunkNum = Integer.parseInt(br.readLine());

            sendP2PResponse(client, formP2PResponse(fileName, chunkNum));
        } catch (IOException ioe) {
            return true;
        }

        try {
            client.close();
            return true;
        } catch (IOException ioe1) {
            return true;
        }
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
      } catch (IOException ioe) {
      }
    }

    /**
     * Form a response to a P2PRequest
     * @param  fileName The name of file being requested
     * @param  chunkNum The chunk number of the file
     * @return a byte[] that contains the data to be sent to the client
     */
    private byte[] formP2PResponse(String fileName, int chunkNum) {
        try{
            String directoryPath = this.getClass().getResource(Constant.PATH_RESOURCE).getPath();
            RandomAccessFile file = new RandomAccessFile(directoryPath + fileName, "r");
            file.seek(Constant.CHUNK_SIZE*(chunkNum-1));
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            file.read(buffer);
            file.close();
            return buffer;
        } catch (FileNotFoundException fnfe) { // we assume this will not happen
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            return buffer;
        } catch (IOException ioe) {
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            return buffer;
        }
    }


    /**
     * Concatenates 2 byte[] into a single byte[]
     * This is a function provided for your convenience.
     * @param  buffer1 a byte array
     * @param  buffer2 another byte array
     * @return concatenation of the 2 buffers
     */
    private byte[] concatenate(byte[] buffer1, byte[] buffer2) {
        byte[] returnBuffer = new byte[buffer1.length + buffer2.length];
        System.arraycopy(buffer1, 0, returnBuffer, 0, buffer1.length);
        System.arraycopy(buffer2, 0, returnBuffer, buffer1.length, buffer2.length);
        return returnBuffer;
    }

}
