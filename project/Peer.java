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
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;


public class Peer {
    
    public static Random r = new Random();
    private static DataOutputStream dataOutputStream = null;
    private static DataInputStream dataInputStream = null;
    private static int peerPort = r.nextInt(9000 - 8001) + 8001;
    private static int serverPort = 8000;
    private static String serverIP = "127.0.0.1";  
    private static String filesPath;
    private static Gson gson = new Gson();
    
    public static void main (String[] args) throws Exception{
        
        Scanner input = new Scanner(System.in);
        int selected_option = 0;
        

        do{
            System.out.println();
            System.out.println("Bem vindo ao menu do Napster. Selecione o numero abaixo correspondente a opcao desejada e pressione ENTER:");
            System.out.println("1 - JOIN");
            System.out.println("2 - LEAVE");
            System.out.println("3 - SEARCH");
            System.out.println("4 - DOWNLOAD");
            System.out.println("5 - SEND");
            System.out.println("0 - SAIR");

            selected_option = input.nextInt();
            
            //Selecionou JOIN
            if(selected_option == 1){
                System.out.println("Qual o caminho para sua pasta de arquivos?");
                filesPath = input.next();

                threadJoin join = new threadJoin(serverIP, serverPort, filesPath);
				join.start();
            }

            //Selecionou LEAVE
            else if(selected_option == 2){
                threadLeave leave = new threadLeave(serverIP, serverPort);
				leave.start();
            }
            
            //Selecionou SEARCH
            else if(selected_option == 3){
                System.out.println("Qual o nome do arquivo que deseja buscar?");
                String filename = input.next();

                threadSearch search = new threadSearch(serverIP, serverPort, filename);
				search.start();
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

                Message peerResponse = sendServer(gson.toJson(download_request), targetPeer);

                if(peerResponse.request.equals("DOWNLOAD_OK")){
                    receiveFileFromPeer(filename);
                }
            }

            //Selecionou SEND
            else if(selected_option == 5){

                DatagramSocket serverSocket = new DatagramSocket(peerPort);
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(recPacket); //BLOCKING

                InetAddress IPAddress = recPacket.getAddress();
                int port = recPacket.getPort();

                String json = new String(recPacket.getData(), 0, recPacket.getLength());
                Message message = gson.fromJson(json, Message.class);

                String clientId = IPAddress+":"+port;

                if (message.request.equals("DOWNLOAD")){

                    Message send_response = new Message();
                    send_response.request = "DOWNLOAD_OK";

                    byte[] sendBuf = new byte[1024];
                    sendBuf = gson.toJson(send_response).getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

                    serverSocket.send(sendPacket);

                    TimeUnit.SECONDS.sleep(3);
                    

                    sendFileToPeer(message.text, clientId);
                }

                
            }

            else{
                System.out.println("Opcao invalida. Por favor, digite o numero correspondente a uma das opcoes disponiveis no menu");
            }


        } while (selected_option != 0);
    }

    public static class threadJoin extends Thread{

        private String serverIP;
        private int serverPort;
        private String filesPath;

        public threadJoin(String serverIP, int serverPort, String filesPath){
            this.serverIP = serverIP;
            this.serverPort = serverPort;
            this.filesPath = filesPath;
        }

        public void run(){
            try {
                String[] files = listFiles(filesPath);

                Message join_request = new Message();
                join_request.request = "JOIN";
                join_request.files = files;

                Message response = sendServer(gson.toJson(join_request), serverIP+":"+serverPort);
                System.out.println(response.text);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadLeave extends Thread{

        private String serverIP;
        private int serverPort;

        public threadLeave(String serverIP, int serverPort){
            this.serverIP = serverIP;
            this.serverPort = serverPort;
        }

        public void run(){
            try {
                Message leave_request = new Message();
                leave_request.request = "LEAVE";

                sendServer(gson.toJson(leave_request), serverIP+":"+serverPort);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadSearch extends Thread{

        private String serverIP;
        private int serverPort;
        private String filename;

        public threadSearch(String serverIP, int serverPort, String filename){
            this.serverIP = serverIP;
            this.serverPort = serverPort;
            this.filename = filename;
        }

        public void run(){
            try {
                

                Message search_request = new Message();
                search_request.request = "SEARCH";
                search_request.text = filename;

                Message response = sendServer(gson.toJson(search_request), serverIP+":"+serverPort);

                System.out.println(response.text);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadDownload extends Thread{

        private String targetPeer;
        private String filename;

        public threadDownload(String filename, String targetPeer){
            this.targetPeer = targetPeer;
            this.filename = filename;
        }

        public void run(){
            try {

                Message download_request = new Message();
                download_request.request = "DOWNLOAD";
                download_request.text = filename;

                Message peerResponse = sendServer(gson.toJson(download_request), targetPeer);

                if(peerResponse.equals("DOWNLOAD_OK")){
                    receiveFileFromPeer(filename);
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadUpload extends Thread{


        public threadUpload(){
        }

        public void run(){
            try {
                DatagramSocket serverSocket = new DatagramSocket(peerPort);
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPacket = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(recPacket); //BLOCKING

                InetAddress IPAddress = recPacket.getAddress();
                int port = recPacket.getPort();

                String json = new String(recPacket.getData(), 0, recPacket.getLength());
                Message message = gson.fromJson(json, Message.class);

                String clientId = IPAddress+":"+port;


                if (message.request.equals("DOWNLOAD")){

                    Message send_response = new Message();
                    send_response.request = "DOWNLOAD_OK";

                    byte[] sendBuf = new byte[1024];
                    sendBuf = gson.toJson(send_response).getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

                    serverSocket.send(sendPacket);

                    TimeUnit.SECONDS.sleep(3);
                    
                    sendFileToPeer(message.text, clientId);
                    serverSocket.close();
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static String[] listFiles (String path){

        String[] files;
        File f = new File(path);
        files = f.list();

        return files;   
    }

    public static Message sendServer(String message, String targetAddress) throws IOException{
        
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

        client.close();

        return serverResponse;
    }

    public static void sendFileToPeer(String filename, String targetPeer) throws IOException{
        try {
            String[] peerParts = targetPeer.split(":");
            String targetPeerIP = peerParts[0].substring(1);
            Integer targetPeerPort = Integer.parseInt(peerParts[1]);


            Socket socket = new Socket(targetPeerIP, targetPeerPort);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            sendFile(filesPath+"/"+filename);
            
            dataInputStream.close();
            socket.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void sendFile(String path) throws Exception{
        int bytes = 0;
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        dataOutputStream.writeLong(file.length());  
        byte[] buffer = new byte[4*1024];

        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();
        }
        fileInputStream.close();
    }

    public static void receiveFileFromPeer(String filename){
        try{
            ServerSocket serverSocket = new ServerSocket(peerPort);
            Socket clientSocket = serverSocket.accept();
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            receiveFile(filesPath+"/"+filename);

            System.out.println("Arquivo "+filename+" baixado com sucesso na pasta "+filesPath);

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
