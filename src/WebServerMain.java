import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A simple HTTP webserver.
 *
 * @author 170008773
 */
public class WebServerMain {

    // next line taken from
    // https://stackoverflow.com/questions/2041778/how-to-initialize-hashset-values-by-construction
    private Set<String> implementedRequests = new HashSet<String>(Arrays.asList("GET", "HEAD"));
    private Map<Integer, String> responseMessage;

    private String root = null;
    private ServerSocket ss = null;
    private Socket conn = null;
    private final String protocol = "HTTP/1.1";
    private final String crlf = "\r\n";
    private PrintWriter out = null;
    private BufferedReader in = null;
    private final String logPath = "logs";
    private final boolean verbose = true;
    private static final int HTTP_REQUEST_COMPONENTS = 3;

    private static final int HTTP_200_OK = 200;
    private static final int HTTP_400_BAD_REQUEST = 400;
    private static final int HTTP_505_HTTP_NOT_SUPPORTED = 505;
    private static final int HTTP_501_NOT_IMPLEMENTED = 501;
    private static final int HTTP_403_FORBIDDEN = 403;
    private static final int HTTP_500_INTERNAL_ERROR = 500;
    private static final int HTTP_404_NOT_FOUND = 404;

    /**
     * Constructor.
     *
     * @param port
     *            the port the server should listen on
     * @param root
     *            the folder where all the documents are stored
     * @throws IOException
     *             do you want text here check?
     */

    public WebServerMain(int port, String root) throws IOException {
        super();
        this.root = root;

        responseMessage = new HashMap<Integer, String>();
        responseMessage.put(HTTP_200_OK, "OK");
        responseMessage.put(HTTP_400_BAD_REQUEST, "Bad Request");
        responseMessage.put(HTTP_505_HTTP_NOT_SUPPORTED, "HTTP Version Not Supported");
        responseMessage.put(HTTP_501_NOT_IMPLEMENTED, "Not Implemented");
        responseMessage.put(HTTP_403_FORBIDDEN, "Forbidden ");
        responseMessage.put(HTTP_500_INTERNAL_ERROR, "Internal Server Error");
        responseMessage.put(HTTP_404_NOT_FOUND, "Not Found");

        ss = new ServerSocket(port);
        while (true) {
            conn = ss.accept();

            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            in = new BufferedReader(isr);
            out = new PrintWriter(conn.getOutputStream(), true);
            String line = in.readLine();
            if (isRequestValid(line)) {
                handleRequest(line);
            }
            conn.close();

        }

    }

    /**
     * Sends back an appropriately formated response to the client.
     *
     * @param requestLine
     *            The request that was made, used for logging.
     * @param responseCode
     *            the HTTP response code
     * @param messageBody
     *            the content of the requested file
     * @param serveBody
     *            should the body be sent to the client?
     */
    private void respond(String requestLine, int responseCode, String messageBody, boolean serveBody) {

        logRequest(requestLine, responseCode, messageBody.length());
        if (verbose) {
            System.out.println(getResponseHeader(responseCode, messageBody));
        }

        out.println(getResponseHeader(responseCode, messageBody));
        if (serveBody) {
            out.println(messageBody);
            if (verbose) {
                System.out.println(messageBody);
            }
        }
        out.flush();
    }

    /**
     * Stores the incoming requests in a daily file.
     *
     * @param requestLine
     *            the request that was made
     * @param responseCode
     *            the HTTP response code
     * @param sizeOfFileReturned
     *            The number of bytes to be sent with the response
     */
    private void logRequest(String requestLine, int responseCode, int sizeOfFileReturned) {

        // next line was taken from
        // https://stackoverflow.com/questions/23068676/how-to-get-current-timestamp-in-string-format-in-java-yyyy-mm-dd-hh-mm-ss
        String today = new SimpleDateFormat("YYYYMMdd").format(new Date());
        File currentLog = new File(logPath + File.separator + today + ".log");
        File logDir = new File(logPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }

        // Succeeds iff the file doens't already exists
        try {
            currentLog.createNewFile();
            try (PrintWriter logger = new PrintWriter(new FileWriter(currentLog, true));) {
                // format taken from
                // https://en.wikipedia.org/wiki/Common_Log_Format
                String timeStamp = new SimpleDateFormat("[dd/MM/YYYY:HH:mm:ss:SSSS z]").format(new Date());
                logger.print(timeStamp + " \"" + requestLine + "\" " + Integer.toString(responseCode) + " "
                        + Integer.toString(sizeOfFileReturned) + crlf);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Read contents of text file into a string.
     *
     * @param file
     *            the file to be read
     * @return a string containing the contents of the provided file
     */
    private String readFile(File file) {

        String contents = "";

        try (Scanner s = new Scanner(file);) {
            while (s.hasNextLine()) {
                contents += s.nextLine() + crlf;
            }
        } catch (Exception e) {
            // we checked the file existed so this should never happen
            e.printStackTrace();
            System.exit(1);
        }

        return contents;
    }

    /**
     * parses the request and calls appropriate functions to respond.
     *
     * @param line
     *            the request that was received.
     */
    private void handleRequest(String line) {

        String[] splitRequest = line.split(" ");
        String method = splitRequest[0];
        String path = root + File.separatorChar + splitRequest[1];

        File requestedFile = new File(path);
        if (!requestedFile.exists()) {

            respond(line, HTTP_404_NOT_FOUND, "", true);
            return;
        }
        if (!requestedFile.canRead()) {
            respond(line, HTTP_403_FORBIDDEN, "", true);
            return;
        }

        if (method.equals("GET")) {
            String fileContents = readFile(requestedFile);
            respond(line, HTTP_200_OK, fileContents, true);

            // out.println(getResponseHeader(HTTP_200_OK, fileContents));
            // out.println(fileContents);
            return;
        } else if (method.equals("HEAD")) {
            String fileContents = readFile(requestedFile);
            respond(line, HTTP_200_OK, fileContents, false);
            // out.println(getResponseHeader(HTTP_200_OK, fileContents));
            return;
        }

    }

    /**
     * constructs a appropriately formated HTTP response header.
     *
     * @param responsCode
     *            the HTTP response code
     * @param responseBody
     *            the contents of the message to be returned.
     * @return
     */
    private String getResponseHeader(int responsCode, String responseBody) {

        assert responseMessage.containsKey(responsCode) : "invalid response code" + Integer.toString(responsCode);
        String mimeType = "text/html";
        String header = protocol + " " + Integer.toString(responsCode) + " " + responseMessage.get(responsCode) + crlf;
        header += "Server: Simple Java Http Server" + crlf;
        header += "Content-Type: " + mimeType + crlf;
        header += "Content-Length: " + responseBody.length() + crlf;
        return header;

    }

    /**
     * Checks whether a request is valid.
     *
     * @param request
     *            the request to be validated
     * @return True if the request is valid, false otherwise
     */
    public boolean isRequestValid(String request) {

        String[] splitRequest = request.split(" ");

        if (splitRequest.length != HTTP_REQUEST_COMPONENTS) {
            respond(request, HTTP_400_BAD_REQUEST, "", true);
            // out.println(getResponseHeader( HTTP_400_BAD_REQUEST, ""));
            // System.out.println(getResponseHeader( HTTP_400_BAD_REQUEST, ""));
            return false;
        }

        if (!splitRequest[splitRequest.length - 1].trim().equals(protocol)) {
            respond(request, HTTP_505_HTTP_NOT_SUPPORTED, "", true);
            // out.println(getResponseHeader(HTTP_505_HTTP_NOT_SUPPORTED, ""));
            // System.out.println(getResponseHeader(HTTP_505_HTTP_NOT_SUPPORTED,
            // ""));
            return false;
        }

        if (!implementedRequests.contains(splitRequest[0])) {
            respond(request, HTTP_501_NOT_IMPLEMENTED, "", true);
            // out.println(getResponseHeader(HTTP_501_NOT_IMPLEMENTED, ""));
            // System.out.println(getResponseHeader(HTTP_501_NOT_IMPLEMENTED,
            // ""));
            return false;
        }

        // regex addapted from
        // https://stackoverflow.com/questions/37370301/how-do-make-a-regular-expression-to-match-file-paths
        // File.separator is for platform independence
        if (!Pattern.matches("\\" + File.separator + "?[^\\" + File.separator + "].*", splitRequest[1])) {
            respond(request, HTTP_400_BAD_REQUEST, "", true);
            // out.println(getResponseHeader( HTTP_400_BAD_REQUEST, ""));
            // System.out.println(getResponseHeader( HTTP_400_BAD_REQUEST, ""));
            return false;
        }

        return true;
    }

    /**
     * main function. does some input checking and then starts up the server.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {

        try {
            File inRoot = new File(args[0]);
            int inPort = Integer.parseInt(args[1]);
            assert inRoot.exists() : "inRoot.exists(): " + inRoot.exists();
            assert inRoot.isDirectory() : "inRoot.isDirectory(): " + inRoot.isDirectory();
            assert inRoot.canWrite() : "inRoot.canWrite(): " + inRoot.canWrite();
            assert inRoot.canRead() : "inRoot.canRead(): " + inRoot.canRead();
            assert inPort > 0;

            new WebServerMain(inPort, args[0]);

        } catch (Exception e) {
            // e.printStackTrace();
            System.out.println("Usage: java WebServerMain <document_root> <port>");
        }

    }

}
