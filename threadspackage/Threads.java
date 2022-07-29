package threadspackage;

public class Threads extends Thread{

    private String threadName;

    public Threads(String nome){
        threadName = nome;
    }

    public void run(){
    
        System.out.println("Thread iniciada");

        for (int i = 1; i <= 4; i++){
            System.out.println("T" + threadName + ": " + i);
            try{
                Thread.sleep(50);
            }
            catch(InterruptedException e){

            }
        }
    }
        
}