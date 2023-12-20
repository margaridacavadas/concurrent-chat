import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Chat client
 * <p>
 * Client side application that connects to a server. It sends and receives messages to and from other clients.
 * </p>
 * <p>
 * This application accepts other commands:
 * </p>
 * <p>
 * The client can send a private message with the command "/whisper ";
 * </p>
 * <p>
 * The client can send an anonymous message with the command "/anon ";
 * </p>
 * <p>
 * The client can list all client connected to the chat with the command "/list";
 * </p>
 * <p>
 * The client can change his username with the command "/user ";
 * </p>
 * <p>
 * The client can quit the chat with the command "/quit";
 * </p>
 */
public class Client {


    /** Command to quit chat */
    private static final String QUIT = "/quit";

    /** Command to list all clients */
    private static final String LIST = "/list";

    /** Command to send private message to specified client */
    private static final String WHISPER = "/whisper";

    private Socket clientSocket;
    private BufferedReader serverReader;
    private ExecutorService pool;

    public Client(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please insert the server's port number: ");
        int port;
        try {
            port = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            System.out.println("Failure to read port number");
            throw new RuntimeException(e);
        }

        Client client = new Client("localhost", port);
        client.start();

    }

    private void setupStreams() throws IOException {
        serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    /**
     * Listens for the messages sent by other clients.
     * @throws IOException
     */
    private void listen() throws IOException {
        while (!clientSocket.isClosed()) {

            String message = serverReader.readLine();

            if (message == null) {
                break;
            }

            System.out.println(message);
        }

        try {
            serverReader.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void start() {
        try {
            setupStreams();

            pool = Executors.newSingleThreadExecutor();
            Messages messages = new Messages();
            pool.submit(messages);

            listen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private class Messages implements Runnable {

        private BufferedReader inSystem;
        private BufferedWriter serverWriter;

        @Override
        public void run() {
            try {
                streams();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sendMessage();

        }


        private void streams() throws IOException {
            inSystem = new BufferedReader(new InputStreamReader(System.in));
            serverWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }

        /**
         * Sends message to chat.
         */
        private void sendMessage() {

            while (!clientSocket.isClosed()) {

                try {
                    String message = inSystem.readLine();

                    if (message == QUIT) {
                        break;
                    }

                    serverWriter.write(message, 0, message.length());
                    serverWriter.newLine();
                    serverWriter.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {

                serverReader.close();
                serverWriter.close();
                clientSocket.close();
                pool.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }


            System.exit(0);
        }

    }

}
