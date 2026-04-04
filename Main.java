import src.main.server.HTTPHandler;
import src.main.server.HTTPListenerConfiguration;
import src.main.server.HTTPServer;

public class Main {
    public static void main(String[] args){
        HTTPHandler handler = (request, response) ->{
            String name = request.getURI().substring(1); // e.g., /Vu -> Vu
            if (name.isEmpty()) name = "Guest";

            response.setContentType("text/html; charset=utf-8");
            var writer = response.getWriter();
            writer.write("<h1>Chào " + name + "!</h1>");
            writer.write("<p>Your IP: " + request.getIpAddress() + "</p>");
        };

        try (HTTPServer server = new HTTPServer().withHandler(handler).withListener(new HTTPListenerConfiguration(8080))){
            server.start();
        }


    }
}
