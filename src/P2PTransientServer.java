import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class P2PTransientServer {

    public static void main(String[] args) {
        new File(Constant.DEFAULT_DIRECTORY).mkdirs();

        int port = Constant.P2P_SERVER_PORT; // fixed port number

        P2PTransientServer serverInstance = new P2PTransientServer();
        serverInstance.start(port);
        System.out.println("P2P transient server closed. Goodbye!");
    }

    private void start(int port) {

        boolean flag = true;

        try{
            ServerSocket welcomeSocket = new ServerSocket (port);
            System.out.println("P2P transient server running on port 9019...");

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
        String msgType;
        String fileName;
        String chunkNumString;
        int chunkNum;
        int flag = 0;

        try {
            isr = new InputStreamReader(client.getInputStream());
            br = new BufferedReader(isr);
            msgType = br.readLine();

            if (msgType.equals(Constant.COMMAND_EXIT)) { // it is an EXIT command
                byte[] buffer = Constant.MESSAGE_ACK.getBytes();
                sendP2PResponse(client, buffer);
                flag = 1;

            }
            // This command is disabled due to symmetric network
            // else if (msgType.equals(Constant.COMMAND_IPCONFIG)) {
            //     try {
            //         Ipconfig ipconfig = STUNClient.getPubIpconfig();
            //         String ip = ipconfig.getIp();
            //         String port = ipconfig.getPort();
            //         String ipReply = ip + ":" + port;
            //         byte[] buffer = ipReply.getBytes();
            //         sendP2PResponse(client, buffer);
            //     } catch (Exception e) {
            //         System.out.println("cannot contact STUN server");
            //     }
            // }
            else if (msgType.equals(Constant.COMMAND_QUERY)) { // it is a download request
                fileName = br.readLine();
                chunkNumString = br.readLine();
                chunkNum = Integer.parseInt(chunkNumString);

                sendP2PResponse(client, formP2PResponse(fileName, chunkNum));

            } else { // it is an invalid query

            }

        } catch (IOException ioe) {
            if (flag==1) {
                return false;
            } else {
                return true;
            }
        }

        try {
            client.close();
            if (flag==1) {
                return false;
            } else {
                return true;
            }

        } catch (IOException ioe1) {
            if (flag==1) {
                return false;
            } else {
                return true;
            }
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
        int bytesRead; //number of bytes read

        try{
            String directoryPath = this.getClass().getResource(Constant.DEFAULT_DIRECTORY).getPath();

            RandomAccessFile file = new RandomAccessFile(directoryPath + fileName, "r");
            file.seek(Constant.CHUNK_SIZE*(chunkNum-1)); //move the pointer to the position where we start reading
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            bytesRead = file.read(buffer);
            file.close();

            if (bytesRead == Constant.CHUNK_SIZE) {
                return buffer;
            } else { //it is the case for the last packet
                byte[] subBuffer = Arrays.copyOfRange(buffer, 0, bytesRead);
                return subBuffer;
            }

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
