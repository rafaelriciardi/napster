package tpc;

import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {

    public static void main (String[] args) throws Exception{

        ServerSocket serverSocket = new ServerSocket(9000);

        while (true){
            System.out.println("Waiting for conection...");
            Socket no = serverSocket.accept(); //BLOCKING
            System.out.println("Conected to client");

            ThreadAtendimento thread = new ThreadAtendimento(no);
            thread.start();
        }

    }
    
}
