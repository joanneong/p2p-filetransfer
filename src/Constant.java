public class Constant {
    public static final String COMMAND_DOWNLOAD = "DOWNLOAD";
    public static final String COMMAND_INFORM = "INFORM";
    public static final String COMMAND_QUERY = "QUERY";
    public static final String COMMAND_LIST = "LIST";
    public static final String COMMAND_EXIT = "EXIT";

    public static final int DIR_SERVER_PORT = 9090;
    public static final int P2P_SERVER_PORT = 9019;
    public static final int CHUNK_SIZE = 1024;

    public static final String DEFAULT_DIRECTORY = "resource/";

    public static final String MESSAGE_DELIMITER = "\r\n";
    public static final String MESSAGE_ACK = "ACK";
    public static final String MESSAGE_REPLY = "REPLY";
    public static final String MESSAGE_CHUNK_NOT_EXIST = "CHUNK NOT EXIST";
    public static final String MESSAGE_GOODBYE = "GOODBYE";

    public static final String ERROR_CLIENT_INFORM_FAILED = "Inform failed";
    public static final String ERROR_FILE_NOT_EXIST = "File queried does not exist";
    public static final String ERROR_INVALID_COMMAND = "Invalid command";
    public static final String ERROR_OWN_SERVER_NOT_CLOSED = "Own host server is not closed!";
}
