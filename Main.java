import src.main.server.HTTPServer;

public class Main {
    public static void main(String[] args){
        HTTPServer server = new HTTPServer(8080);
        server.start();
    }
}
