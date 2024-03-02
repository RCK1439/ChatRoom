
/**
 * @author Ruan C. Keet (26340461)
 * Server.java
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class represents the main server of the chatroom.
 * 
 * @see Runnable
 */
public class Server implements Runnable {

    private ServerSocket serverSocket;
    private ServerInput serverInput;
    private HashMap<String, ClientHandler> clients;
    private ExecutorService threadPool;

    /**
     * Constructs an instance of the server and also starts the server.
     * <p>
     * The server is opened on port 6666 always.
     */
    public Server() {
        try {
            this.serverSocket = new ServerSocket(6666);
        } catch (IOException e) {
            System.err.println("server could not be opened");
            System.exit(1);
        }

        this.serverInput = new ServerInput();
        this.clients = new HashMap<String, ClientHandler>();
        this.threadPool = Executors.newCachedThreadPool();

        System.out.printf("server is open on port %d\n", 6666);
        threadPool.execute(serverInput);
    }

    /**
     * Actively listens for any incoming connections from clients to the server
     * <p>
     * This will terminate once the server socket has been closed.
     */
    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                final Socket socket = serverSocket.accept();
                final ClientHandler handler = new ClientHandler(socket);

                try {
                    String username = handler.promptUserName();
                    while (clients.containsKey(username)) {
                        handler.sendMessage(String.format("%s already exists\n", username));
                        username = handler.promptUserName();
                    }

                    threadPool.execute(handler);
                    clients.put(username, handler);

                    final String joinMessage = MessageFormatter.joinMessage(username);
                    broadcast(username, joinMessage, false);
                } catch (IOException e) {
                    System.err.println("error occured whilst prompting for username");
                }
            } catch (IOException e) {
                System.err.println("failed accepting incoming client. server socket closed?");
                System.exit(0);
            }
        }
    }

    /**
     * Closes all sockets from the clients in {@code clients} and also closes
     * the server socket.
     * <p>
     * This should, in theory, terminate the server.
     */
    private void close() {
        for (final ClientHandler client : clients.values()) {
            disconnectClient(client, false);
        }
        clients.clear();

        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("failed to close server socket");
        }

        System.out.println("server terminated");
    }

    /**
     * Broadcasts {@code message} from {@code username} to all the connected
     * clients on the server.
     * 
     * @param username   The username from which the message originates from.
     * @param message    The message to broadcast to all the connected clients.
     * @param fromServer A flag indicating whether the message comes from the
     * server or not.
     */
    private void broadcast(String username, String message, boolean fromServer) {
        if (fromServer) {
            final String serverMessage = MessageFormatter.serverMessage(message);
            for (final ClientHandler client : clients.values()) {
                try {
                    client.sendMessage(serverMessage);
                } catch (IOException e) {
                    System.err.printf("failed to send server message to %s\n", client.getUserName());
                } 
            }

            return;
        }

        final ClientHandler from = clients.get(username);
        for (final ClientHandler client : clients.values()) {
            if (client != from) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.printf("failed to send message (\"%s\") to %s, from %s\n", message, client.getUserName(), username);
                }
            }
        }

        System.out.print(message);
    }

    /**
     * Handles the command sent from {@code from}.
     * 
     * @param command The command to handle.
     * @param from    The client from whom the command comes from.
     */
    private void handleCommand(String command, ClientHandler from) {
        final String[] tokens = command.split(" ");

        if (tokens[0].equals("/quit")) {
            disconnectClient(from, true);
        } else if (tokens[0].equals("/whisper")) {
            if (tokens.length < 3) {
                try {
                    from.sendMessage("whisper with: /whisper <username> <message>\n");
                } catch (IOException e) {
                    System.err.printf("failed to show whisper-usage to %s\n", from.getUserName());
                }
                return;
            } 

            final String to = tokens[1];
            if (!clients.containsKey(to)) {
                try {
                    from.sendMessage(String.format("no user with username: %s\n", to));
                } catch (IOException e) {
                    System.err.printf("failed to send error message to %s\n", from.getUserName());
                }
                return;
            }
            final ClientHandler reciever = clients.get(to);

            final StringBuilder builder = new StringBuilder();
            for (int i = 2; i < tokens.length; i++) {
                builder.append(tokens[i]);
                builder.append(' ');
            }
            final String message = builder.toString();

            try {
                reciever.sendMessage(MessageFormatter.whisperMessage(from.getUserName(), message));
            } catch (IOException e) {
                System.err.printf("failed to whisper to %s from %s\n", reciever.getUserName(), from.getUserName());
            }

        } else if (tokens[0].equals("/list")) {
            final StringBuilder builder = new StringBuilder();

            builder.append("connected users: ");
            for (final String userName : clients.keySet()) {
                builder.append(userName);
                builder.append(' ');
            }
            builder.append('\n');

            final String message = builder.toString();
            try {
                from.sendMessage(message);
            } catch (IOException e) {
                System.err.printf("failed to send list of users to %s\n", from.getUserName());
            }
        } else {
            try {
                from.sendMessage(String.format("unknown command: %s\n", tokens[0]));
            } catch (IOException e) {
                System.err.printf("failed to display \"unknown command\" error to %s\n", from.getUserName());
            }
        }
    }

    /**
     * Disconnects {@code client} from the server.
     * 
     * @param client The client to disconnect from the server.
     * @param remove A flag indicating whether or not the client should be
     * removed from the {@code clients} HashMap.
     */
    private void disconnectClient(ClientHandler client, boolean remove) {
        final String userName = client.getUserName();
        try {
            if (remove) {
                clients.remove(userName);
            }
            client.sendMessage("/close"); // forward disconnect command
            client.close();

            final String disconnectMessage = MessageFormatter.leaveMessage(userName);
            broadcast(userName, disconnectMessage, false);
        } catch (IOException e) {
            System.err.printf("failed to disconnect %s from the server\n", userName);
        }
    }

    /**
     * Class handling server text-input from the terminal.
     * <p>
     * This essentially allows the server to send messages and close itself.
     * 
     * @see Runnable
     */
    private class ServerInput implements Runnable {
        
        /**
         * Constructs a new instance of {@code ServerInput}.
         */
        public ServerInput() {}

        /**
         * Actively listens for input from {@code System.in} from the server.
         * <p>
         * This will terminate once the "/close" has been entered via
         * {@code System.in}. This will also terminate the server.
         */
        @Override
        public void run() {
            final Scanner scanner = new Scanner(System.in);
            
            String message;
            while (!(message = scanner.nextLine()).equals("/close")) {
                if (message.startsWith("/")) {
                    System.out.println("only \"/close\" command is available to the server");
                } else {
                    broadcast(null, message, true);
                }
            }

            scanner.close();
            close();
        }
    }

    /**
     * This class is what the server sees from the connected clients.
     * <p>
     * This allows the server to communicate to the clients aswell as for the
     * clients to communicate with one another.
     * 
     * @see Runnable
     */
    private class ClientHandler implements Runnable {

        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        private String userName;

        /**
         * Constructs a new instance of a {@code ClientHandler} and then opens
         * the input- and outputstreams for the server to communicate with.
         * 
         * @param socket The connected client socket.
         * 
         * @throws IOException - If and I/O error occurs.
         */
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
            this.userName = null;
        }

        /**
         * Prompts the client to enter their username.
         * 
         * @return The username entered by the client.
         * 
         * @throws IOException - If and I/O error occurs.
         */
        public String promptUserName() throws IOException {
            sendMessage("Enter a username: ");
            this.userName = input.readUTF();

            return userName;
        }

        /**
         * Sends {@code message} to this client.
         * 
         * @param message The message to send to the client.
         * 
         * @throws IOException - If and I/O error occurs.
         */
        public void sendMessage(String message) throws IOException {
            if (!socket.isClosed()) {
                output.writeUTF(message);
            }
        }

        /**
         * Closes the socket of this client.
         * 
         * @throws IOException - If an I/O error occurs.
         */
        public void close() throws IOException {
            if (!socket.isClosed()) {
                socket.close();
            }
        }

        /**
         * Gets the username entered by the client.
         * 
         * @return The username of this client.
         */
        public String getUserName() {
            return userName;
        }

        /**
         * Actively listens for any incoming messages from this client.
         * <p>
         * This will run until this client's socket is closed.
         */
        @Override
        public void run() {
            while (!socket.isClosed()) {
                try {
                    if (input.available() > 0) {
                        final String message = input.readUTF();
                        if (message.startsWith("/")) {
                            handleCommand(message, this);
                        } else {
                            final String globalMessage = MessageFormatter.globalMessage(userName, message);
                            broadcast(userName, globalMessage, false);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("failed recieving bytes from client input stream");
                    disconnectClient(this, true);
                }
            }
        }
    }

    /**
     * Main routine for the server. This serves as the entry point of the
     * server program.
     * 
     * @param args Arguments passed via the command-line.
     */
    public static void main(String[] args) {
        final Server server = new Server();
        server.run();
    }
}