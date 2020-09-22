
import java.io.*;
import java.net.*;

public class ProxyThread extends Thread {
    private Socket ClientSocket;
    private BufferedReader ClientToProxy;
    final private int bufferSize = 4096;

    ProxyThread(Socket socket) {
//        super();
        this.ClientSocket = socket;
        try {
            ClientToProxy = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Redirects bytes from one socket to another

    private void redirectBytes(Socket inSock, Socket outSock) {
        byte[] buffer = new byte[bufferSize];
        try {
            int counter;
            do {
                counter = inSock.getInputStream().read(buffer);
                if (counter > 0) {
                    outSock.getOutputStream().write(buffer, 0, counter);
                    if (inSock.getInputStream().available() < 1) {
                        outSock.getOutputStream().flush();
                    }
                }
            } while (counter >= 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        try {

            StringBuilder request = new StringBuilder();
            StringBuilder tmp = new StringBuilder();
            Socket ServerSocket;

            boolean isHttps = false;
            String webAdr = "",
                   urlString = "",
                   fileExtension = "";

            int port = 0;
            tmp.append(ClientToProxy.readLine());
            String[] adress;

            //Reading the client's request and checking it's type (HTTP or HTTPS)

            while (!tmp.toString().isEmpty()) {
                System.out.println(tmp.toString());
                if(tmp.toString().equals("null")){
                    ClientSocket.getOutputStream().flush();
                    ClientSocket.close();
                    return;
                }
//                if (tmp.toString().startsWith("GET")){
//                    urlString = tmp.toString().substring(tmp.toString().indexOf(" ")+1);
//                    urlString = urlString.substring(0, urlString.indexOf(' '));
//                    fileExtension = urlString.substring(urlString.lastIndexOf("."));
//                }
                if (tmp.toString().startsWith("CONNECT"))
                    isHttps = true;
                else if (tmp.toString().startsWith("Host:")) {
                    adress = tmp.toString().split(":");
                    webAdr = adress[1].trim();
                    port = adress.length == 3 ? Integer.decode(adress[2].trim()) : 80;
                } else if (tmp.toString().startsWith("Proxy-Connection:")) {
                    tmp.replace(0, 6, "");
                }
                request.append(tmp.toString()).append("\n");
                tmp.setLength(0);
                tmp.append(ClientToProxy.readLine());
            }
            request.append("\n");

            //Sending the Client's request using the proxy server

            if (!isHttps) {
                System.out.println(request.toString());
                ServerSocket = new Socket(webAdr, port);
                ServerSocket.getOutputStream().write(request.toString().getBytes());
                ServerSocket.getOutputStream().flush();
                ServerSocket.setSoTimeout(5000);
                Thread sendingClientBytes = new Thread(() -> {
                    redirectBytes(ClientSocket, ServerSocket);
                });
                sendingClientBytes.start();
                redirectBytes(ServerSocket, ClientSocket);
                ClientSocket.getOutputStream().flush();
                ClientSocket.close();
                ServerSocket.close();
            } else {
                ServerSocket = new Socket(webAdr, port);
                System.out.println(ClientSocket + ": " + ServerSocket);
                ClientSocket.getOutputStream().write("HTTP/1.0 200 Connection established\r\n Proxy-Agent: ProxyServer/1.0\r\n\r\n".getBytes());
                ClientSocket.getOutputStream().flush();
//                ServerSocket.setSoTimeout(1000);
                new Thread(() -> redirectBytes(ClientSocket, ServerSocket)).start();
                redirectBytes(ServerSocket, ClientSocket);
                ClientSocket.close();
                ServerSocket.close();
            }
            ClientToProxy.close();
        } catch (IOException ignored) {
        }
    }
}
