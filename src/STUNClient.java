import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.String.*;
import java.lang.Byte.*;

class STUNClient {
    public static void main(String args[]) {
    }

    public static Ipconfig getPubIpconfig() throws Exception {
        String stunQuery = "000100082112a442cab99fe99d9695a5fd7f00000003000400000000";// content of STUN query, in hex

        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("stun.l.google.com");// STUN server to query
        System.out.println(IPAddress);
        byte[] sendData = hexStringToByteArray(stunQuery);
        byte[] receiveData = new byte[1024];

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 19302);

        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        //parse the source and destination IP address
        //xor encryption keys: 0x2112a442(IP) 0x2112(port)
        String pubPort = String.valueOf((int)((receiveData[26]+receiveData[27]) & 0xFFFF ^ 0x2112));

        // "& 0xFF" to make it unsigned, "^ 0x$$" to decrypt
        String pubIp = String.valueOf((int)(receiveData[28] & 0xFF ^ 0x21)) + "."
                + String.valueOf((int)(receiveData[29] & 0xFF ^ 0x12)) + "."
                + String.valueOf((int)(receiveData[30] & 0xFF ^ 0xa4)) + "."
                + String.valueOf((int)(receiveData[31] & 0xFF ^ 0x42));

        Ipconfig ipconfig = new Ipconfig(pubIp, pubPort);

        clientSocket.close();

        return ipconfig;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
