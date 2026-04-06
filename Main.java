import src.main.server.HTTPHandler;
import src.main.server.HTTPListenerConfiguration;
import src.main.server.HTTPServer;

public class Main {
    public static void main(String[] args){
        HTTPHandler handler = (request, response) ->{
            String name = request.getURI().substring(1); // e.g., /Vu -> Vu
            if (name.isEmpty()) name = "Guest";
            String html = "<h1>Chào " + name + "!</h1>";
            response.setContentType("text/html; charset=utf-8");
            response.setHeader("content-length", String.valueOf(html.length()));
            var writer = response.getWriter();
            writer.write(html);
        };

        try (HTTPServer server = new HTTPServer().withHandler(handler).withListener(new HTTPListenerConfiguration(8080))){
            server.start();
        }


    }
}
