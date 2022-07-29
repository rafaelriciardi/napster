package tpc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ThreadAtendimento extends Thread{

    private Socket no = null;

    public ThreadAtendimento(Socket node){
        no = node;
    }
    
    public void run(){

        try {
            InputStreamReader is = new InputStreamReader(no.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            OutputStream os = no.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            String text = reader.readLine();

            writer.writeBytes(text + "\n");

            no.close();

        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
}
