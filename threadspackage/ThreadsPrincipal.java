package threadspackage;

public class ThreadsPrincipal{
    public static void main(String[] args) {
        
        Threads t1 = new Threads("1");
        Threads t2 = new Threads("2");

        t1.start();
        t2.start();
    
    }
}