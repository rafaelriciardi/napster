package project;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

public class Server {

    public static Gson gson = new Gson();
    public static void main (String[] args) throws Exception{
        try{
            Hashtable<String, Set<String>> peersInfo = new Hashtable<>();
            DatagramSocket serverSocket = new DatagramSocket(8000);

            while (true){
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

                if (message.request.equals("JOIN")){
                    threadJoin join = new threadJoin(message, peersInfo, clientId, serverSocket);
				    join.start();
                }

                if (message.request.equals("LEAVE")){
                    threadLeave leave = new threadLeave(message, peersInfo, clientId, serverSocket);
				    leave.start();
                }

                if (message.request.equals("SEARCH")){
                    threadSearch search = new threadSearch(message, peersInfo, clientId, serverSocket);
				    search.start();
                }


            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static class threadJoin extends Thread{

        private Message message;
        private Hashtable<String, Set<String>> peersInfo;
        private String clientId;
        private DatagramSocket serverSocket;

        public threadJoin(Message message, Hashtable<String, Set<String>> peersInfo, String clientId, DatagramSocket serverSocket){
            this.message = message;
            this.peersInfo = peersInfo;
            this.clientId = clientId;
            this.serverSocket = serverSocket;
        }

        public void run(){
            try {
                System.out.println("Client Request - JOIN");

                String[] targetParts = clientId.split(":");
                String targetIP = targetParts[0];
                Integer targetPort = Integer.parseInt(targetParts[1]);
                InetAddress IPAddress = InetAddress.getByName(targetIP.substring(1));

                //Para cada arquivo que o peer tem, verifica se na tabela hash ja existe esse arquivo
                //Caso nao, insere como uma nova chave e com a informacao desse peer
                //Caso sim, edita a lista de peers que têm.
                for(String file: message.files){ 
                    if(!peersInfo.containsKey(file)){
                        Set<String> peersWithfile = new HashSet<String>();
                        peersWithfile.add(clientId);
                        peersInfo.put(file, peersWithfile);
                    }
                    else{
                        Set<String> peersWithfile = peersInfo.get(file);
                        peersWithfile.add(clientId);
                        peersInfo.replace(file, peersWithfile);
                    }
                }

                System.out.println();
                System.out.println("**** Peers Info ****");
                System.out.println(peersInfo);
                System.out.println();

                String text = "Sou peer "+clientId+" com arquivos "+concatFilenames(message.files);

                Message join_response = new Message();
                join_response.request = "JOIN_OK";
                join_response.text = text;
                
                System.out.println(gson.toJson(join_response));
                byte[] sendBuf = new byte[1024];
                sendBuf = gson.toJson(join_response).getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, targetPort);

                serverSocket.send(sendPacket);
                System.out.println("Server Response - JOIN_OK");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadLeave extends Thread{

        private Message message;
        private Hashtable<String, Set<String>> peersInfo;
        private String clientId;
        private DatagramSocket serverSocket;

        public threadLeave(Message message, Hashtable<String, Set<String>> peersInfo, String clientId, DatagramSocket serverSocket){
            this.message = message;
            this.peersInfo = peersInfo;
            this.clientId = clientId;
            this.serverSocket = serverSocket;
        }

        public void run(){
            try {
            System.out.println("Client Request - LEAVE");

            String[] targetParts = clientId.split(":");
            String targetIP = targetParts[0];
            Integer targetPort = Integer.parseInt(targetParts[1]);
            InetAddress IPAddress = InetAddress.getByName(targetIP.substring(1));

            //Remove o peer de cada set na hashtable
            //Se o set ficar vazio, remove a chave tambem.
            Hashtable<String, Set<String>> HashtablepeersInfoClone = (Hashtable<String, Set<String>>)peersInfo.clone();
            Set<String> keys = HashtablepeersInfoClone.keySet();

            for(String key: keys){ 
                Set<String> peersWithfile = peersInfo.get(key);
                if(peersWithfile.contains(clientId)){
                    peersWithfile.remove(clientId);
                    if(peersWithfile.isEmpty()){
                        peersInfo.remove(key);
                    } 
                    else{
                        peersInfo.replace(key, peersWithfile);     
                    }                           
                }
            }

            System.out.println();
            System.out.println("**** Peers Info ****");
            System.out.println(peersInfo);
            System.out.println();

            Message leave_response = new Message();
            leave_response.request = "LEAVE_OK";
            
            System.out.println(gson.toJson(leave_response));
            byte[] sendBuf = new byte[1024];
            sendBuf = gson.toJson(leave_response).getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, targetPort);

            serverSocket.send(sendPacket);
            System.out.println("Server Response - LEAVE_OK");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static class threadSearch extends Thread{

        private Message message;
        private Hashtable<String, Set<String>> peersInfo;
        private String clientId;
        private DatagramSocket serverSocket;

        public threadSearch(Message message, Hashtable<String, Set<String>> peersInfo, String clientId, DatagramSocket serverSocket){
            this.message = message;
            this.peersInfo = peersInfo;
            this.clientId = clientId;
            this.serverSocket = serverSocket;
        }

        public void run(){
            try {
                System.out.println("Client Request - SEARCH");

            String[] targetParts = clientId.split(":");
            String targetIP = targetParts[0];
            Integer targetPort = Integer.parseInt(targetParts[1]);
            InetAddress IPAddress = InetAddress.getByName(targetIP.substring(1));
 
            String targetFile = message.text;

            Set<String> peersSet = peersInfo.get(targetFile);

            List<String> peersWithfile = new ArrayList<String>();
            if (peersSet != null)
                peersWithfile.addAll(peersSet);

            System.out.println(peersWithfile);

            Message search_response = new Message();
            search_response.request = "SEARCH_OK";
            search_response.text = "Peers com arquivo solicitado:" + peersWithfile;

            
            System.out.println(gson.toJson(search_response));
            byte[] sendBuf = new byte[1024];
            sendBuf = gson.toJson(search_response).getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, targetPort);

            serverSocket.send(sendPacket);
            System.out.println("Server Response - SEARCH_OK");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static String concatFilenames (String[] files){
        String concatNames ="";

        for(String str: files)
            concatNames = concatNames+str+" ";

        return concatNames;
    }
}


