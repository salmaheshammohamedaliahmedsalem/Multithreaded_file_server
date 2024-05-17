import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class FileClientGUI extends JFrame {
    private JTextField ipField, portField, fileField;
    private JTextArea statusArea;
    private JComboBox<String> actionBox;

    public FileClientGUI() {
        super("File Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        ipField = new JTextField("localhost");
        portField = new JTextField("1234");
        fileField = new JTextField();
        actionBox = new JComboBox<>(new String[]{"READ", "WRITE"});

        inputPanel.add(new JLabel("Server IP:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Port:"));
        inputPanel.add(portField);
        inputPanel.add(new JLabel("Filename:"));
        inputPanel.add(fileField);
        inputPanel.add(new JLabel("Action:"));
        inputPanel.add(actionBox);

        add(inputPanel, BorderLayout.NORTH);

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        add(new JScrollPane(statusArea), BorderLayout.CENTER);

        JButton requestButton = new JButton("Request File");
        requestButton.addActionListener(e -> requestFile());
        add(requestButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void requestFile() {
        String serverAddress = ipField.getText();
        int port = Integer.parseInt(portField.getText());
        String fileName = fileField.getText();
        String action = (String) actionBox.getSelectedItem();

        statusArea.append("Connecting to server...\n");

        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            statusArea.append("Connected to server at " + serverAddress + ":" + port + "\n");
            out.println(action + ":" + fileName);
            out.flush();

            statusArea.append("Sent request: " + action + ":" + fileName + "\n");

            if (action.equals("READ")) {
                handleReadResponse(fileName, in);
            } else if (action.equals("WRITE")) {
                handleWriteRequest(fileName, out, in);
            } else {
                statusArea.append("Invalid action.\n");
            }
        } catch (UnknownHostException e) {
            statusArea.append("Server not found: " + e.getMessage() + "\n");
        } catch (IOException e) {
            statusArea.append("I/O error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void handleReadResponse(String fileName, DataInputStream in) throws IOException {
        File file = new File("received_" + fileName);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) > 0) {
                fileOut.write(buffer, 0, count);
            }
            statusArea.append("File received and saved as: " + file.getName() + "\n");
        } catch (EOFException e) {
            statusArea.append("File transfer complete or file is empty.\n");
        } catch (IOException e) {
            statusArea.append("Error saving file.\n");
            e.printStackTrace();
        }
    }

    private void handleWriteRequest(String fileName, PrintWriter out, DataInputStream in) throws IOException {
        statusArea.append("Please enter the content to write to the file. Type 'END' on a new line to finish.\n");

        try (BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (!(line = userIn.readLine()).equals("END")) {
                out.println(line);
                out.flush();
            }
            out.println("END");
            out.flush();

            String response = in.readUTF();
            statusArea.append(response + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileClientGUI::new);
    }
}
