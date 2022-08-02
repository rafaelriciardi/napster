package project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;


public class Peer {

    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static int peerPort = 8765;
    private static int serverPort = 8000;
    private static String serverIP = "127.0.0.1"; 
    private static String filesPath = "/home/rafael-ubuntu/napster/peer_data";
    public static void main (String[] args) throws Exception{

        
        Gson gson = new Gson();

        Scanner input = new Scanner(System.in);
        int selected_option = 0;

        do{
            System.out.println();
            System.out.println("Bem vindo ao menu do Napster. Selecione o numero abaixo correspondente a opcao desejada:");
            System.out.println("1 - JOIN");
            System.out.println("2 - LEAVE");
            System.out.println("3 - SEARCH");
            System.out.println("4 - DOWNLOAD");
            System.out.println("5 - ENVIAR");
            System.out.println("0 - SAIR");

            selected_option = input.nextInt();
            
            //Selecionou JOIN
            if(selected_option == 1){
                String[] files = listFiles(filesPath);

                Message join_request = new Message();
                join_request.request = "JOIN";
                join_request.files = files;

                sendServer(gson.toJson(join_request), serverIP+":"+serverPort);
            }

            //Selecionou LEAVE
            else if(selected_option == 2){

                Message leave_request = new Message();
                leave_request.request = "LEAVE";


                sendServer(gson.toJson(leave_request), serverIP+":"+serverPort);
            }
            
            //Selecionou SEARCH
            else if(selected_option == 3){

                System.out.println("Qual o nome do arquivo que deseja buscar?");

                Message search_request = new Message();
                search_request.request = "SEARCH";
                search_request.text = input.next();

                sendServer(gson.toJson(search_request), serverIP+":"+serverPort);
            }

            //Selecionou DOWNLOAD
            else if(selected_option == 4){
                System.out.println("Qual o nome do arquivo que deseja baixar?");
                String filename = input.next();

                System.out.println("Qual o endereco ip:porta do peer que deseja solicitar?");
                String targetPeer = input.next();

                Message download_request = new Message();
                download_request.request = "DOWNLOAD";
                download_request.text = filename;

                String peerResponse = sendServer(gson.toJson(download_request), targetPeer);

                if(peerResponse.equals("DOWNLOAD_OK")){
                    receiveFileFromPeer(filename);
                }
            }

            //Selecionou ENVIAR
            else if(selected_option == 5){
                DatagramSocket serverSocket = new DatagramSocket(peerPort);
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
                System.out.println("Waiting for any messages");
                serverSocket.receive(recPacket); //BLOCKING

                InetAddress IPAddress = recPacket.getAddress();
                int port = recPacket.getPort();

                String json = new String(recPacket.getData(), 0, recPacket.getLength());
                Message message = gson.fromJson(json, Message.class);

                String clientId = IPAddress+":"+port;

                System.out.println(clientId+" - "+message.request);

                if (message.request.equals("DOWNLOAD")){
                    System.out.println("Peer Request - DOWNLOAD");

                    Message send_response = new Message();
                    send_response.request = "DOWNLOAD_OK";

                    byte[] sendBuf = new byte[1024];
                    sendBuf = gson.toJson(send_response).getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

                    serverSocket.send(sendPacket);
                    System.out.println("My Response - DOWNLOAD_OK");

                    TimeUnit.SECONDS.sleep(5);
                    
                    System.out.println("My Response - Starting Upload");

                    sendFileToPeer(message.text, clientId);

                }
            }

            else{
                System.out.println("Opcao invalida. Por favor, digite o numero correspondente a uma das opcoes disponiveis no menu");
            }


        } while (selected_option != 0);
    }

    public static String[] listFiles (String path){

        String[] files;
        File f = new File(path);
        files = f.list();

        return files;   
    }

    public static String sendServer(String message, String targetAddress) throws IOException{

        String[] targetParts = targetAddress.split(":");
        String targetIP = targetParts[0];
        Integer targetPort = Integer.parseInt(targetParts[1]);
        
        DatagramSocket client = new DatagramSocket(peerPort);
        InetAddress IPAddress = InetAddress.getByName(targetIP);
        client.setSoTimeout(1000);

        byte[] sendData = new byte[1024];
        sendData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
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

        return serverResponse.request;
    }

    public static void sendFileToPeer(String filename, String targetPeer) throws IOException{
        System.out.println("Sending file "+filename+" to peer "+targetPeer);
        try {
            String[] peerParts = targetPeer.split(":");
            String targetPeerIP = peerParts[0].substring(1);
            System.out.println(targetPeerIP);
            System.out.println("11");
            Integer targetPeerPort = Integer.parseInt(peerParts[1]);
            //InetAddress IPAddress = InetAddress.getByName(targetPeerIP);

            System.out.println("1");

            Socket socket = new Socket(targetPeerIP, targetPeerPort);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            System.out.println("2");
            sendFile(filesPath+"/"+filename);
            
            dataInputStream.close();
            socket.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void sendFile(String path) throws Exception{
        System.out.println("3");
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        System.out.println("4");
        // send file size
        dataOutputStream.writeLong(file.length());  
        System.out.println("5");
        // break file into chunks
        byte[] buffer = new byte[4*1024];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        System.out.println("6");
        fileInputStream.close();
    }

    public static void receiveFileFromPeer(String filename){
        try{
            ServerSocket serverSocket = new ServerSocket(peerPort);
            System.out.println("listening to port:"+peerPort);
            Socket clientSocket = serverSocket.accept();
            System.out.println(clientSocket+" connected.");
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            receiveFile(filesPath+"/"+filename);

            dataInputStream.close();
            dataOutputStream.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName) throws Exception{
        int bytes = 0;
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        
        long size = dataInputStream.readLong();     // read file size
        byte[] buffer = new byte[4*1024];
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size
        }
        fileOutputStream.close();
    }
}
