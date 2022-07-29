package udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpServer {

    public static void main (String[] args) throws Exception{

        try{
            DatagramSocket serverSocket = new DatagramSocket(9876);

            while (true){
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
                System.out.println("Waiting for any messages");
                serverSocket.receive(recPacket); //BLOCKING

                byte[] sendBuf = new byte[1024];
                sendBuf = "Im the server".getBytes();

                InetAddress IPAddress = recPacket.getAddress();
                int port = recPacket.getPort();

                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

                serverSocket.send(sendPacket);
                System.out.println("Message sent by the server");
            }

        } catch (Exception e) {
            //TODO: handle exception
        }
    }
    
}
