import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class P2PClientPartialTest {
    public static final String TEMP_DIRECTORY = "temp/";

    Socket clientSocket;

    PrintWriter pw;
    Scanner sc;

    String messageReceived;

    private String getDownloadMessage(String fileName) throws IOException {

        FileOutputStream fos = new FileOutputStream(TEMP_DIRECTORY + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        int chunkNumber = 1;

        while (chunkNumber<=13) {

            Socket socketToP2PServer = connectToServer("localhost", Constant.P2P_SERVER_PORT);

            sendQueryToP2PServer(fileName, chunkNumber, socketToP2PServer);

            receiveDataFromP2PServer(bos, socketToP2PServer);

            socketToP2PServer.close();

            chunkNumber++;
        }

        bos.close();

        return "File " + fileName + " downloaded from peer server";
    }

    private void sendExitToOwnServer() throws IOException {
        Socket socketToOwnServer = connectToServer("localhost", Constant.P2P_SERVER_PORT);
        PrintWriter writerToOwnServer = new PrintWriter(socketToOwnServer.getOutputStream(), true);
        writerToOwnServer.println(Constant.COMMAND_EXIT);
        writerToOwnServer.flush();

        socketToOwnServer.close();
    }

    private Socket connectToServer(String p2pServerIP, int p2pServerPort) throws IOException {
        return new Socket(p2pServerIP, p2pServerPort);
    }

    private void receiveDataFromP2PServer(BufferedOutputStream bos, Socket socketToP2PServer) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead = socketToP2PServer.getInputStream().read(buffer);

        if (bytesRead == Constant.CHUNK_SIZE) {
            bos.write(buffer);
        } else { //it is the case for the last packet
            byte[] subBuffer = Arrays.copyOfRange(buffer, 0, bytesRead);
            bos.write(subBuffer);
        }

        bos.flush();
    }

    private void sendQueryToP2PServer(String fileName, int chunkNumber, Socket socketToP2PServer) throws IOException {

        PrintWriter writerToP2PServer = new PrintWriter(socketToP2PServer.getOutputStream(), true);

        String toP2PServer = Constant.COMMAND_QUERY + Constant.MESSAGE_DELIMITER
                + fileName + Constant.MESSAGE_DELIMITER
                + chunkNumber + Constant.MESSAGE_DELIMITER;
        writerToP2PServer.println(toP2PServer);
        writerToP2PServer.flush();
    }

    private int getNumberOfChunks(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        char[] buffer = new char[1024];

        int chunkCount = 0;
        while (br.read(buffer) != -1) {
            //System.out.println("Is counting...");
            chunkCount++;
        }

        br.close();

        return chunkCount;
    }

    private void start(String serverIP, int serverPort) throws IOException {

        String fileName;
        String replyMessage;
        int chunkNumber;

        System.out.println("File name: " + "Sample_Essay_1.docx");
        chunkNumber = getNumberOfChunks("resource/Sample_Essay_1.docx");
        System.out.println("Number of chunks: " + chunkNumber);

        replyMessage = getDownloadMessage("Sample_Essay_1.docx");
        System.out.println(replyMessage);

        sendExitToOwnServer();
        System.out.println("P2P server closed.");

    }

    public static void main(String[] args) {
        new File(TEMP_DIRECTORY).mkdirs();

        try {
            P2PClientPartialTest client = new P2PClientPartialTest();
            client.start("localhost", 9200); //the value here will not be used
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
