import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Developed by Margarida Cavadas - 10/2023
 */

/** Chat server.
 * <p>
 * Server side application - accepts connections to several clients and sends the messages sent by each client to all connected clients.
 * </p>
 * <p>
 * This application also allows other commands:
 * </p>
 * <p>
 * The messages can be sent to only one specified client;
 * </p>
 * <p>
 * The message can be sent anonymously;
 * </p>
 * <p>
 * The client can change his username;
 * </p>
 * <p>
 * The client can list all connected clients.
 * </p>
 *
 */
public class Server {

    /** Command to exit chat */
    private static final String QUIT = "/quit";

    /** Command to list all clients */
    private static final String LIST = "/list";

    /** Command to send private message */
    private static final String WHISPER = "/whisper";

    /** Command to send anonymous message */
    private static final String ANON = "/anon";

    /** Command to change username */
    private static final String USER = "/user";


    private ServerSocket serverSocket;

    /** Map with the connected clients */
    private volatile Map<String, ClientSocket> clientMap = new ConcurrentHashMap<String, ClientSocket>();

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please, insert the port number: ");
        int port;
        try {
            port = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            System.out.println("Failure to read port number");
            System.out.println("Default port number: 8099");
            port = 8099;
        }
        Server server = new Server(port);
        System.out.println("Server started at " + server.serverSocket);
        server.start();
    }

    /**
     * Accepts connections with clients and delegates the responsibility of dealing with their messages to a Thread Pool.
     */
    private void start() {
        int i = 0;
        try {

            while (true) {

                Socket clientSocket = serverSocket.accept();
                i++;
                String clientName = "Client-" + i;
                ClientSocket clientConnection = new ClientSocket(clientSocket, clientName);

                //add client to map
                clientMap.put(clientName, clientConnection);

                System.out.println("Connection accepted with " + clientName + ", at " + clientSocket + ".");

                ExecutorService pool = Executors.newFixedThreadPool(4);
                pool.submit(clientConnection);
            }

        } catch (IOException e) {
            System.out.println("ERROR: Could not initiate Server.");
        }
    }

    /**
     * Lists the clients in the client Map
     * @return       String with the clients' names.
     */
    private String listClients() {
        StringBuilder listBuilder = new StringBuilder("List of map of clients: \n");
        for (String n : clientMap.keySet()) {
            listBuilder.append(n + "\n");
        }
        return listBuilder.toString();
    }

    /**
     * Sends a message from one client to one specified client.
     * @param receivedMessage           the message to be sent
     * @param destClient                the final client
     * @param senderClient              the origin client
     */
    private void sendPrivateMessage(String receivedMessage, String destClient, ClientSocket senderClient) {
        if (!clientMap.containsKey(destClient)) {
            try {
                senderClient.send("Message not send. The client does not exist.");
            } catch (IOException e) {
                System.out.println("ERROR: could not send message back to " + senderClient.name);
            }
        } else {
            ClientSocket destinationSocket = clientMap.get(destClient);
            try {
                destinationSocket.send("@" + senderClient.name + ": " + receivedMessage);
            } catch (IOException e) {
                System.out.println("ERROR: could not send the private message from " + senderClient.name + " to " + destinationSocket.name + ".");
            }
        }
    }

    /**
     * Sends a message from one client to all connected clients.
     * It iterates through the client map
     * @param receivedMessage           the message to be sent.
     * @param clientSocket              the origin client.
     * @throws IOException
     */
    private void broadcast(String receivedMessage, ClientSocket clientSocket) throws IOException {

        for (ClientSocket c : clientMap.values()) {
            if (c != null) {
                if (!c.equals(clientSocket)) {
                    c.send(clientSocket.name + ": " + receivedMessage);
                }
            }
        }
    }

    /**
     * Sends a message from one client to all connected clients, anonymously.
     * @param receivedMessage   the message to be sent
     * @throws IOException
     */
    private void broadcastAnonMessage(String receivedMessage) throws IOException {
        for (ClientSocket c : clientMap.values()) {
            if (c != null) {
                c.send("~" + receivedMessage);
            }
        }
    }

    /**
     * Removes one client from the clientMap and prints the remained elements on the list.
     * @param clientSocket              client to be removed
     */
    private void removeMap(ClientSocket clientSocket) {
        if (clientMap.containsKey(clientSocket.name)) {
            //System.out.println("My map contains the client I am trying to remove " + clientSocket.name);
            clientMap.remove(clientSocket.name, clientMap.get(clientSocket.name));
        }
        //System.out.println(listClientMap());
    }

    /**
     * Changes the name of a specific client in the clientMap and prints the updated collection.
     *
     * @param name              client's new name
     * @param clientSocket      the client whose name is to be changed
     */
    private void changeClientName(String name, ClientSocket clientSocket) {
        for (String n : clientMap.keySet()) {
            if (n.equals(clientSocket.name)) {
                removeMap(clientSocket);
                clientMap.put(name, clientSocket);
            }
        }

        System.out.println(listClients());

    }


    /**
     * Inner class that implements Runnable
     * Responsible for dealing with the received messages from each client and managing what actions to take.
     */
    private class ClientSocket implements Runnable {

        private Socket clientSocket;
        private BufferedWriter writer;
        private BufferedReader reader;
        private String name;

        public ClientSocket(Socket clientSocket, String name) {
            this.clientSocket = clientSocket;
            this.name = name;
            try {
                setUpStreams();
            } catch (IOException e) {
                System.out.println("Could not connect the I/O Streams: " + e.getMessage());
            }
        }

        @Override
        public void run() {

            try {
                while (!clientSocket.isClosed()) {

                    String receivedMessage = reader.readLine();

                    if (receivedMessage == null || receivedMessage.equals(QUIT)) {
                        System.out.println("Closing connection with " + name);
                        //removeList(this);
                        removeMap(this);
                        reader.close();
                        writer.close();
                        break;
                    }

                    if (receivedMessage.split(" ")[0].equals(USER)) {
                        String propClientName = receivedMessage.split(" ")[1];
                        if (propClientName != null || (!propClientName.equals(""))) {
                            changeClientName(propClientName, this);
                            this.name = propClientName;
                            continue;
                        } else {
                            continue;
                        }
                    }

                    if (receivedMessage.equals(LIST)) {
                        System.out.println(name + " requested a list of clients.");
                        //send(listClients());
                        send(listClients());
                        continue;
                    }

                    if (receivedMessage.split(" ")[0].equals(WHISPER)) {

                        String destClient = receivedMessage.split(" ")[1];

                        System.out.println(name + " wants to send a private message.");

                        sendPrivateMessage(buildPrivateMessage(receivedMessage), destClient, this);
                        continue;
                    }

                    if (receivedMessage.split(" ")[0].equals(ANON)) {
                        System.out.println(name + "wants to send an anonymous message.");
                        broadcastAnonMessage(buildAnonMessage(receivedMessage));
                        continue;
                    }

                    broadcast(receivedMessage, this);
                    System.out.println(name + ": " + receivedMessage);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        private void setUpStreams() throws IOException {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }

        /**
         * Sends a given message to this client socket.
         *
         * @param message               message to be sent
         * @throws IOException
         */
        private void send(String message) throws IOException {
            if (!clientSocket.isClosed()) {

                writer.write(message);
                writer.newLine();
                writer.flush();
            }

        }


        /**
         * Refactors the message to be sent anonymously, excluding the sender's name.
         *
         * @param message       message to be sent
         * @return              the refactored message
         */
        private String buildAnonMessage(String message) {
            String[] destMessage = message.split(" ");

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 1; i < destMessage.length; i++) {

                stringBuilder.append(destMessage[i] + " ");
            }
            return stringBuilder.toString();
        }


        /**
         * Refactors the message to be sent privately, excluding the sender's name, and the destination client.
         *
         * @param message           message to be sent
         * @return                  the refactored message
         */
        private String buildPrivateMessage(String message) {
            String[] destMessage = message.split(" ");

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 2; i < destMessage.length; i++) {

                stringBuilder.append(destMessage[i] + " ");
            }
            return stringBuilder.toString();
        }
    }
}
