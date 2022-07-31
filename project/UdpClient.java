package project;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {
    public static void main(String[] args) throws Exception{
        DatagramSocket client = new DatagramSocket();

        InetAddress IPAddress = InetAddress.getByName("127.0.0.1");

        byte[] sendData = new byte[1024];
        sendData = "Im a client".getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8000);

        client.send(sendPacket);

        byte[] recBuffer = new byte[1024];

        DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);

        client.receive(recPacket); //BLOCKING

        String info = new String(recPacket.getData(), recPacket.getOffset(), recPacket.getLength());

        System.out.println("From server: "+info);

        client.close();

    }
}
