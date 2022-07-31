package project;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Server {
    public static void main (String[] args) throws Exception{
        try{
            DatagramSocket serverSocket = new DatagramSocket(8000);

            while (true){
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
                System.out.println("Waiting for any messages");
                serverSocket.receive(recPacket); //BLOCKING

                String sentence = new String(recPacket.getData(), 0, recPacket.getLength());

                System.out.println(sentence);

                InetAddress IPAddress = recPacket.getAddress();
                int port = recPacket.getPort();

                byte[] sendBuf = new byte[1024];
                sendBuf = ("Sou peer "+IPAddress+":"+port+" com arquivos "+sentence).getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

                serverSocket.send(sendPacket);
                System.out.println("Message sent by the server");
            }

        } catch (Exception e) {
            //TODO: handle exception
        }
    }
}


