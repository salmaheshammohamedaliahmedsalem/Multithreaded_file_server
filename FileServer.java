import java.io.*;
import java.net.*;

public class FileServer {
    public static void main(String[] args) {
        ServerSocket server = null;

        try {
            server = new ServerSocket(1234);
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());
                FileHandler clientSock = new FileHandler(client);
                new Thread(clientSock).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class FileHandler implements Runnable {
        private final Socket clientSocket;

        public FileHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                
                String fileName;
                while ((fileName = in.readLine()) != null) {
                    File file = new File(fileName);
                    if (file.exists()) {
                        FileInputStream fileIn = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = fileIn.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                        }
                        fileIn.close();
                    }
                    out.writeUTF("File transfer complete.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

