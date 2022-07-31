package project;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

import com.google.gson.Gson;


public class Peer {
    public static void main (String[] args) throws Exception{

        String filesPath = "/home/rafael-ubuntu/napster/peer_data";
        Gson gson = new Gson();

        Scanner input = new Scanner(System.in);
        int selected_option = 0;
        int[] valid_options = {1};
        do{
            System.out.println("Bem vindo ao menu do Napster. Selecione o numero abaixo correspondente a opcao desejada:");
            System.out.println("1 - JOIN");
            System.out.println("2 - LEAVE");
            System.out.println("3 - SEARCH");
            System.out.println("4 - DOWNLOAD");

            selected_option = input.nextInt();
            
            //Selecionou JOIN
            if(selected_option == 1){
                String[] files = listFiles(filesPath);

                Message join_request = new Message();
                join_request.request = "JOIN";
                join_request.files = files;

                sendServer(gson.toJson(join_request));
            }

            //Selecionou LEAVE
            if(selected_option == 2){

                Message leave_request = new Message();
                leave_request.request = "LEAVE";

                sendServer(gson.toJson(leave_request));
            }

            if (Arrays.asList(valid_options).contains(selected_option)) {
                System.out.println("Opcao invalida. Por favor, seleciona uma das opcoes disponiveis no menu");
            }

        } while (selected_option != 0);
    }

    public static String[] listFiles (String path){

        String[] files;
        File f = new File(path);
        files = f.list();

        return files;   
    }

    public static void sendServer(String message) throws IOException{
        int peerPort = 8765;
        DatagramSocket client = new DatagramSocket(peerPort);
        InetAddress IPAddress = InetAddress.getByName("127.0.0.1");
        client.setSoTimeout(1000);

        byte[] sendData = new byte[1024];
        sendData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8000);
        client.send(sendPacket);

        byte[] recBuffer = new byte[1024];
        DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
        client.receive(recPacket); //BLOCKING

        String jsonResponse = new String(recPacket.getData(), recPacket.getOffset(), recPacket.getLength());
        Gson gson = new Gson();
        Message serverResponse = gson.fromJson(jsonResponse, Message.class);
        System.out.println("From server: " + serverResponse.request);
        System.out.println("From server: " + serverResponse.text);

        client.close();
    }
    
}
