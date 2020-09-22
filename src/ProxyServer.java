
import java.io.*;
import java.net.*;

//http://www.columbia.edu/~fdc/sample.html - site for testing

//This part just starts the server, listening to the port 18917 (my student ID)
//It can be updated by using Executors

public class ProxyServer {
    public void listenSocket() {
        ServerSocket server = null;
        Socket client = null;
        try {
            server = new ServerSocket(18917);
        }
        catch (IOException e) {
            System.out.println("Could not listen");
            System.exit(-1);
        }
        System.out.println("Server listens on port: " + server.getLocalPort());

        while(true) {
            try {
                client = server.accept();
            }
            catch (IOException e) {
                System.out.println("Accept failed");
                System.exit(-1);
            }

            new ProxyThread(client).start();
        }

    }

    public static void main(String[] args) {
        ProxyServer server = new ProxyServer();
        server.listenSocket();
    }
}
