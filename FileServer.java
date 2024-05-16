import java.io.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class FileServer {
    private static final int PORT = 1234;
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found.";
    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());
                FileHandler clientSock = new FileHandler(client);
                new Thread(clientSock).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FileHandler implements Runnable {
        private final Socket clientSocket;

        public FileHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String request;
                while ((request = in.readLine()) != null) {
                    String[] requestParts = request.split(":");
                    if (requestParts.length < 2) {
                        out.writeUTF("Invalid request format.");
                        continue;
                    }

                    String action = requestParts[0];
                    String fileName = requestParts[1];

                    switch (action) {
                        case "READ":
                            handleReadRequest(fileName, out);
                            break;
                        case "WRITE":
                            handleWriteRequest(fileName, in, out);
                            break;
                        default:
                            out.writeUTF("Invalid action.");
                            break;
                    }
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

        private void handleReadRequest(String fileName, DataOutputStream out) throws IOException {
            rwLock.readLock().lock();
            try {
                File file = new File(fileName);
                if (file.exists()) {
                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = fileIn.read(buffer)) > 0) {
                            out.write(buffer, 0, count);
                        }
                    }
                    out.writeUTF("File transfer complete.");
                } else {
                    out.writeUTF(FILE_NOT_FOUND_MESSAGE);
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }

        private void handleWriteRequest(String fileName, BufferedReader in, DataOutputStream out) throws IOException {
            rwLock.writeLock().lock();
            try {
                // Prompt the client to start entering lines
                out.writeUTF(
                        "Please start entering lines to be added to the file. Send 'END' on a new line to finish.");
                out.flush(); // Ensure prompt is sent immediately

                // Open the file for appending
                File file = new File(fileName);
                try (FileWriter writer = new FileWriter(file, true);
                        BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

                    // Receive lines from the client and write them to the file
                    String line;
                    while ((line = in.readLine()) != null && !line.equals("END")) {
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                        bufferedWriter.flush(); // Ensure line is written immediately
                    }
                }
                out.writeUTF("File write complete.");
            } finally {
                rwLock.writeLock().unlock();
            }
        }

    }
}
