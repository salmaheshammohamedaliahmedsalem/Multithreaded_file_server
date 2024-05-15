import java.io.*;
import java.net.*;
import java.util.*;

public class FileClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1234);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Enter file name:");
            String fileName = sc.nextLine();

            out.println(fileName);
            out.flush();

            File file = new File("received_" + fileName);
            FileOutputStream fileOut = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) > 0) {
                fileOut.write(buffer, 0, count);
            }
            fileOut.close();
            System.out.println("File received and saved as: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

