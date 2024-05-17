import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class FileServerGUI extends JFrame {
    private static final int PORT = 1234;
    private static final String BASE_DIRECTORY = "/home/salma/Desktop/shakespeare"; // Change to your directory
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found.";
    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private JTextArea logArea;
    private ServerSocket serverSocket;
    private boolean isRunning;

    public FileServerGUI() {
        super("File Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("Start Server");
        JButton stopButton = new JButton("Stop Server");
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        add(controlPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        setVisible(true);
    }

    private void startServer() {
        if (isRunning) {
            log("Server is already running.");
            return;
        }

        isRunning = true;
        log("Starting server...");
        new Thread(this::runServer).start();
    }

    private void stopServer() {
        if (!isRunning) {
            log("Server is not running.");
            return;
        }

        isRunning = false;
        log("Stopping server...");
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error closing server socket: " + e.getMessage());
        }
    }

    private void runServer() {
        try (ServerSocket server = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"))) {
            server.setReuseAddress(true);
            serverSocket = server;
            log("Server started on port " + PORT);

            while (isRunning) {
                try {
                    Socket client = server.accept();
                    log("New client connected: " + client.getInetAddress().getHostAddress());
                    new Thread(new FileHandler(client)).start();
                } catch (IOException e) {
                    if (!isRunning) {
                        log("Server stopped.");
                        break;
                    }
                    log("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private class FileHandler implements Runnable {
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
                log("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void handleReadRequest(String fileName, DataOutputStream out) throws IOException {
            rwLock.readLock().lock();
            try {
                File file = new File(BASE_DIRECTORY, fileName);
                if (file.exists() && file.isFile() && file.getCanonicalPath().startsWith(new File(BASE_DIRECTORY).getCanonicalPath())) {
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
                out.writeUTF("Please start entering lines to be added to the file. Send 'END' on a new line to finish.");
                out.flush();

                File file = new File(BASE_DIRECTORY, fileName);
                try (FileWriter writer = new FileWriter(file, true);
                     BufferedWriter bufferedWriter = new BufferedWriter(writer)) {

                    String line;
                    while ((line = in.readLine()) != null && !line.equals("END")) {
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                }
                out.writeUTF("File write complete.");
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileServerGUI::new);
    }
}
