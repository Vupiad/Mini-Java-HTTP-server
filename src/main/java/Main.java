package src.main.java;

import src.main.java.server.HTTPHandler;
import src.main.java.server.HTTPListenerConfiguration;
import src.main.java.server.HTTPServer;

public class Main {
    public static void main(String[] args){
        HTTPHandler handler = (request, response) ->{
            String name = request.getURI().substring(1);
            if (name.isEmpty()) name = "Guest";
            String html = "<h1>hello " + name + "!</h1>";
            response.setContentType("text/html; charset=utf-8");
            response.setHeader("content-length", String.valueOf(html.length()));
            var writer = response.getWriter();
            writer.write(html);
        };

        try (HTTPServer server = new HTTPServer().withHandler(handler).withListener(new HTTPListenerConfiguration(8080)).withMaximumPendingSocketConnection(250)){
            server.start();
        }


    }
}
