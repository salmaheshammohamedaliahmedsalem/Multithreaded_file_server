import java.io.*;
import java.net.*;
import java.util.*;

public class FileClient {
    public static void main(String[] args) {
        // Allow user to specify server address and port
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter server IP address:");
        String serverAddress = sc.nextLine();

        try (Socket socket = new Socket(serverAddress, 1234);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            System.out.println("Enter file name:");
            String fileName = sc.nextLine();

            out.println(fileName);
            out.flush();

            // Receive feedback from server or file data
            File file = new File("received_" + fileName);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) > 0) {
                    fileOut.write(buffer, 0, count);
                }
                System.out.println("File received and saved as: " + file.getName());
            } catch (EOFException e) {
                System.out.println("File transfer complete or file is empty.");
            } catch (IOException e) {
                System.out.println("Error saving file.");
                e.printStackTrace();
            }

        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}
