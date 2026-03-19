import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args){
        String hostname = "localhost";
        int port = 8080;
        while(true){
            try (Socket socket = new Socket(hostname, port)) {

                // 1. Setup Output to send data to server
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                // 2. Setup Input to read data from server
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                Scanner keyboard = new Scanner(System.in);
                System.out.println("Type a message to send to the server:");
                String userInput = keyboard.nextLine();

                writer.println(userInput);
                // 3. Send a message (The Request)
                // 4. Read the response (The Response)
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

            } catch (UnknownHostException ex) {
                System.err.println("Server not found: " + ex.getMessage());
            } catch (IOException ex) {
                System.err.println("I/O error: " + ex.getMessage());
            }
        }

    }
}
