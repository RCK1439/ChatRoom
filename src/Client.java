
/**
 * @author Ruan C. Keet (26340461)
 * Client.java
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;

import java.util.Scanner;

/**
 * This class represents the Client of our chatroom.
 * 
 * @see Runnable
 */
public class Client implements Runnable {

    private Socket socket;
    private Scanner scanner;
    private ServerHandler handler;
    private Thread thread;

    /**
     * Constructs a new instance of the client and attempts to connect to the
     * server.
     */
    public Client() {
        this.scanner = new Scanner(System.in);

        try {
            this.socket = new Socket("localhost", 6666);
        } catch (IOException e) {
            System.out.println("server not open");
            close();
            System.exit(0);
        }
        
        this.handler = new ServerHandler(socket);
        this.thread = new Thread(handler);

        thread.start();
    }

    /**
     * Actively listens for any input via {@code System.in} from the client.
     * <p>
     * Upon <enter> is pressed, the message will be sent to the server via
     * the server handler class.
     */
    @Override
    public void run() {
        while (!socket.isClosed()) {
            final String message = scanner.nextLine();
            
            try {
                handler.sendMessage(message);
            } catch (IOException e) {
                System.err.printf("failed to send message to server: \"%s\"\n", message);
            }

            if (message.equals("/quit")) {
                break;
            }
        }

        close();
    }

    /**
     * Closes the socket and scanner used by the client.
     */
    private void close() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("failed to close socket");
                System.exit(1);
            }
        }

        scanner.close();
    }

    /**
     * This class is what the Client sees from the server. This allows the 
     * Client to send messages to the server and recieve messages from the
     * server.
     * 
     * @see Runnable
     */
    private class ServerHandler implements Runnable {
        
        DataInputStream input;
        DataOutputStream output;

        /**
         * Constructs a new instance of the {@code ServerHandler}.
         * 
         * @param socket The socket opened by the client.
         */
        public ServerHandler(Socket socket) {
            try {
                this.input = new DataInputStream(socket.getInputStream());
                this.output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.err.println("failed opening I/O streams from socket");
                close();
            }
        }

        /**
         * Sends {@code message} to the server.
         * 
         * @param message The message to send to the server.
         * 
         * @throws IOException - If and I/O error occurs.
         */
        public void sendMessage(String message) throws IOException {
            if (!socket.isClosed()) {
                output.writeUTF(message);
            }
        }

        /**
         * Actively listens to any messages coming from the server.
         * <p>
         * Upon message recieved, the message is printed to {@code System.out}.
         * <p>
         * This function will terminate if the socket is closed.
         */
        @Override
        public void run() {
            while (!socket.isClosed()) {
                try {
                    if (input.available() > 0) {
                        final String message = input.readUTF();
                        if (message.equals("/close")) {
                            System.out.println("server closed: press [ENTER] to continue");
                            close();
                        } else {
                            System.out.print(message);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("failed to read incoming bytes from server");
                    close();
                }
            }
        }
    }

    /**
     * Main routine for the client. This serves as the entry point of the
     * client program.
     * 
     * @param args Arguments passed via the command-line.
     */
    public static void main(String[] args) {
        final Client client = new Client();
        client.run();
    }
}
