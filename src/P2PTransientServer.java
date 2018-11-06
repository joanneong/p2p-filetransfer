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

public class P2PTransientServer implements Runnable {

    private Socket acceptedClientSocket = null;

    private boolean running = true;

    public P2PTransientServer(Socket acceptedClientSocket) {
        this.acceptedClientSocket = acceptedClientSocket;
    }

    public P2PTransientServer() {}

    public void start(int port) {

        try{
            ServerSocket welcomeSocket = new ServerSocket (port);
            System.out.println("P2P transient server running on port 9019...");

            while (running) {
                Socket connectionSocket = welcomeSocket.accept();
                P2PTransientServer newServer = new P2PTransientServer(connectionSocket);
                new Thread(newServer).start();
            }

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public void exit() {
        running = false;
    }

    @Override
    public void run() {
        System.out.println("New thread created to entertain the client...\n");
        while (!acceptedClientSocket.isClosed()) {
            handleClientSocket(acceptedClientSocket);
        }
        System.out.println("Client exits...");
        return;
    }

    /**
     * Handles requests sent by a client
     * @param  client Socket that handles the client connection
     */
    private void handleClientSocket(Socket client) {
        InputStreamReader isr;
        BufferedReader br;
        String msgType;
        String fileName;
        String chunkNumString;
        int chunkNum;

        try {
            isr = new InputStreamReader(client.getInputStream());
            br = new BufferedReader(isr);
            msgType = br.readLine();

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
            if (msgType.equals(Constant.COMMAND_QUERY)) { // it is a download request
                fileName = br.readLine();
                chunkNumString = br.readLine();
                chunkNum = Integer.parseInt(chunkNumString);

                sendP2PResponse(client, formP2PResponse(fileName, chunkNum));
                System.out.println("Sending " + fileName + " chunk No." + chunkNum + " to " + clientIp(client));

            } else { // it is an invalid query
                System.out.println("Invalid query");
            }

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        try {
            client.close();

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
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
        int bytesRead; //number of bytes read

        try{
            String directoryPath = Constant.DEFAULT_DIRECTORY;

            RandomAccessFile file = new RandomAccessFile(directoryPath + fileName, "r");
            file.seek(Constant.CHUNK_SIZE*(chunkNum-1)); //move the pointer to the position where we start reading
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            bytesRead = file.read(buffer);

//            To print value inside buffer
//            String value = new String(buffer, "UTF-8");
//            System.out.println("value:" + value+ "\n");
            
            file.close();

            if (bytesRead == Constant.CHUNK_SIZE) {
                return buffer;
            } else { //it is the case for the last packet
                byte[] subBuffer = Arrays.copyOfRange(buffer, 0, bytesRead);
                System.out.println("length:"+subBuffer.length + "\n");
                return subBuffer;
            }

        } catch (FileNotFoundException fnfe) { // we assume this will not happen
            System.out.println(fnfe.getMessage());
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            return buffer;
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            byte[] buffer = new byte[Constant.CHUNK_SIZE];
            return buffer;
        }
    }

    private String clientIp(Socket socket) {
        return socket.getInetAddress().toString() + ":" + socket.getPort();
    }
}
