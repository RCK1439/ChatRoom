
/**
 * @author Ruan C. Keet (26340461)
 * MessageFormatter.java
 */

/**
 * This class formats messages (with colors) according to the message format
 * required by the function-caller.
 */
public class MessageFormatter {

    private static String COLOR_LEAVE = "\033[31m";    // Red
    private static String COLOR_JOIN = "\033[32m";     // Green
    private static String COLOR_WHISPER = "\033[33m";  // Yellow
    private static String COLOR_SERVER = "\033[1;37m"; // Bold white
    private static String COLOR_ESCAPE = "\033[m";     // Escape code

    /**
     * Gets a string in the format of {@code "[userName]: message"}.
     * 
     * @apiNote A newline ({@code '\n'}) is appended to the end of the string.
     * 
     * @param userName The username of the message sender.
     * @param message  The message sent by {@code userName}.
     * 
     * @return A string formatted in the global-message format.
     */
    public static String globalMessage(String userName, String message) {
        final StringBuilder builder = new StringBuilder();

        builder.append('[');
        builder.append(userName);
        builder.append("]: ");
        builder.append(message);
        builder.append('\n');

        return builder.toString();
    }

    /**
     * Gets a string in the format of {@code "[SERVER]: message"}.
     * 
     * @apiNote A newline ({@code '\n'}) is appended to the end of the string.
     * 
     * @param message The message sent by the server.
     * 
     * @return A string formatted in the server-message format.
     */
    public static String serverMessage(String message) {
        final StringBuilder builder = new StringBuilder();

        builder.append(COLOR_SERVER);
        builder.append("[SERVER]: ");
        builder.append(message);
        builder.append(COLOR_ESCAPE);
        builder.append('\n');

        return builder.toString();
    }

    /**
     * Gets a string the format of {@code "userName has joined the chat"}.
     * 
     * @apiNote A newline ({@code '\n'}) is appended to the end of the string.
     * <p>
     * Also, the string is colored green.
     * 
     * @param userName The username that joined the server.
     * 
     * @return A string formatted in the join-message format.
     */
    public static String joinMessage(String userName) {
        final StringBuilder builder = new StringBuilder();

        builder.append(COLOR_JOIN);
        builder.append(userName);
        builder.append(" has joined the chat");
        builder.append(COLOR_ESCAPE);
        builder.append('\n');

        return builder.toString();
    }

    /**
     * Gets a string the format of {@code "userName has left the chat"}.
     * 
     * @apiNote A newline ({@code '\n'}) is appended to the end of the string.
     * <p>
     * Also, the string is colored red.
     * 
     * @param userName The username that left the server.
     * 
     * @return A string formatted in the leave-message format.
     */
    public static String leaveMessage(String userName) {
        final StringBuilder builder = new StringBuilder();

        builder.append(COLOR_LEAVE);
        builder.append(userName);
        builder.append(" has left the chat");
        builder.append(COLOR_ESCAPE);
        builder.append('\n');

        return builder.toString();
    }

    /**
     * Gets a string the format of {@code "userName has whispered: message"}.
     * 
     * @apiNote A newline ({@code '\n'}) is appended to the end of the string.
     * <p>
     * Also, the string is colored yellow.
     * 
     * @param userName The username that whispered the message.
     * @param message  The message to be whispered.
     * 
     * @return A string formatted in the whisper-message format.
     */
    public static String whisperMessage(String userName, String message) {
        final StringBuilder builder = new StringBuilder();

        builder.append(COLOR_WHISPER);
        builder.append(userName);
        builder.append(" has whispered: ");
        builder.append(message);
        builder.append(COLOR_ESCAPE);
        builder.append('\n');

        return builder.toString();
    }
}
