public class Constant {
    public static final String COMMAND_DOWNLOAD = "DOWNLOAD";
    public static final String COMMAND_EXIT = "EXIT";
    public static final String COMMAND_INFORM = "INFORM";
    public static final String COMMAND_LIST = "LIST";
    public static final String COMMAND_QUERY = "QUERY";
    public static final String COMMAND_NAME = "NAME";
    public static final String COMMAND_UPLOAD = "UPLOAD";
    public static final String COMMAND_INFORM_FILESIZE = "FILESIZE";
    public static final String COMMAND_QUERY_FILESIZE = "QUERYFILESIZE";

    public static final String TYPE_CLIENT_SOCKET = "CLIENT";
    public static final String TYPE_TRANSIENT_SOCKET = "TRANSIENT";

    public static final int DIR_SERVER_PORT = 9090;

    public static final int CHUNK_SIZE = 1024;

    public static final String DEFAULT_DIRECTORY = "resource/";

    public static final String MESSAGE_DELIMITER = "\r\n";
    public static final String MESSAGE_ACK = "ACK";
    public static final String MESSAGE_REPLY = "REPLY";
    public static final String MESSAGE_CHUNK_NOT_EXIST = "CHUNK NOT EXIST";
    public static final String MESSAGE_FILE_LIST_EMPTY = "FILE LIST EMPTY";
    public static final String MESSAGE_GOODBYE = "GOODBYE";

    public static final String ERROR_CLIENT_INFORM_FAILED = "Inform failed";
    public static final String ERROR_INFORM_FILE_NOT_EXIST = "File informed does not exist" + MESSAGE_DELIMITER;
    public static final String ERROR_QUERY_FILE_NOT_EXIST = "File queried does not exist" + MESSAGE_DELIMITER;
    public static final String ERROR_DOWNLOAD_FILE_EXIST = "File requested already exists in directory!" + MESSAGE_DELIMITER;
    public static final String ERROR_DOWNLOAD_FILE_NOT_EXIST = "File requested does not exist" + MESSAGE_DELIMITER;
    public static final String ERROR_INVALID_COMMAND = "Invalid command" + MESSAGE_DELIMITER;
}
