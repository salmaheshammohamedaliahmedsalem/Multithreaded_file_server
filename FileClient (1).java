import java.io.*;
import java.net.*;
import java.util.*;

public class FileClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                Scanner sc = new Scanner(System.in)) {

            System.out.println("Enter action (READ/WRITE):");
            String action = sc.nextLine().toUpperCase();

            System.out.println("Enter file name:");
            String fileName = sc.nextLine();

            out.println(action + ":" + fileName);
            out.flush();

            if (action.equals("READ")) {
                handleReadResponse(fileName, in);
            } else if (action.equals("WRITE")) {
                handleWriteRequest(fileName, socket, in, sc);
            } else {
                System.out.println("Invalid action.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleReadResponse(String fileName, DataInputStream in) throws IOException {
        File file = new File("received_" + fileName);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) > 0) {
                fileOut.write(buffer, 0, count);
            }
            System.out.println("File received and saved as: " + file.getName());
        }
    }

    private static void handleWriteRequest(String fileName, Socket socket, DataInputStream in, Scanner sc)
            throws IOException {
        System.out.println("Please start entering lines to be added to the file. Send 'END' on a new line to finish.");
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String line;
        while (!(line = sc.nextLine()).equals("END")) {
            out.println(line);
            out.flush();
        }
        out.println("END"); // Signal end of input
        out.flush();
        String response = in.readUTF();
        System.out.println(response);
    }
}
