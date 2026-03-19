package src.main.server;
import java.net.Socket;

public class HTTPWorkerThread implements Runnable {
    private final Socket clientSocket;
    public HTTPWorkerThread(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run(){
        System.out.println("Running");
    }
}
