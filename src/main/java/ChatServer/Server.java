/*
 * Copyright (c) 2019. -- Luiz Artur Boing Imhof
 */

package ChatServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static final int PORT = 9000;

    private static List<Message> messages = new LinkedList<Message>();

    public static void main(final String[] args) throws IOException {

        log.debug("Starting the Main ..");

        // The Server Socket
        final ServerSocket serverSocket = new ServerSocket(PORT);

        log.debug("Server started in port {}, waiting for connections ..", PORT);

        // Forever serve.
        while (true) {

            // One socket by request (try with resources).
            try (final Socket socket = serverSocket.accept()) {

                // The remote connection address.
                final InetAddress address = socket.getInetAddress();

                log.debug("========================================================================================");
                log.debug("Connection from {} in port {}.", address.getHostAddress(), socket.getPort());
                processConnection(socket);

            } catch (IOException e) {
                log.error("Error", e);
                throw e;
            }

        }

    }

    /**
     * Process the connection.
     *
     * @param socket to use as source of data.
     */
    private static void processConnection(final Socket socket) throws IOException {

        // Reading the inputstream
        final List<String> lines = readSocketInput(socket);   // FUNCIONA
        //final List<String> lines = readInputStreamByLines(socket); // NAO FUNCIONA, ENTENDER O MOTIVO
        final String request = lines.get(0);
        log.debug("Request: {}", request);

        final PrintWriter pw = new PrintWriter(socket.getOutputStream());

        if (request.contains("GET")) {

            log.debug("GET REQUEST");
            //final PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.println("HTTP/1.1 200 OK");
            pw.println("Server: DSM v0.0.1");
            pw.println("Date: " + new Date());
            pw.println("Content-Type: text/html; charset=UTF-8");
            pw.println();
            pw.println(generateHtml());
            pw.println();
            pw.flush();

        } else if (request.contains("POST")) {
            log.debug("POST REQUEST");

            log.debug("Input body read");

            for (String line : lines) {
                log.debug("***** " + line);
            }

            if (addMessage(lines)) {

                pw.println("HTTP/1.1 200 OK");
                pw.println("Server: DSM v0.0.1");
                pw.println("Date: " + new Date());
                pw.println("Content-Type: text/html; charset=UTF-8");
                pw.println();
                pw.println(generateHtml());
                pw.println();
                pw.flush();

                log.debug("MESSAGE ADDED SUCCESFULLY");

            } else {

                pw.println("HTTP/1.1 400 ERROR");
                pw.println("Server: DSM v0.0.1");
                pw.println();
                pw.flush();
            }
        } else {
            log.debug("ERROR REQUEST");
            //final PrintWriter pw = new PrintWriter(socket.getOutputStream());
            pw.println("HTTP/1.1 400 ERROR");
            pw.println("Server: DSM v0.0.1");
            pw.println();
            pw.flush();
        }
        log.debug("Process ended.");
    }

    /**
     * Read all the input stream.
     *
     * @param socket to use to read.
     * @return all the string readed.
     */
    private static List<String> readInputStreamByLines(final Socket socket) throws IOException {

        final InputStream is = socket.getInputStream();

        // The list of string read from inputstream.
        final List<String> lines = new ArrayList<>();

        // The Scanner
        final Scanner s = new Scanner(is).useDelimiter("\\A");
        log.debug("Reading the Inputstream ..");
        while (true) {
            final String line = s.nextLine();
            if (line.length() == 0) {
                break;
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String generateHtml() throws IOException {

        String html = "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Chat</title>" +
                "</head>" +
                "<body>" +
                "<div>" +
                "<textarea readonly style=\"width:100%; height:50%\">";
        for (Message i : messages) {
            if (i.toString().isEmpty()){

            }else {
                html += i.toString() + "\n";
            }
        }

        html += " </textarea>\n"+
                "    </div>\n" +
                "    <div id=\"chat-input\">\n" +
                "        <form action=\"/\" method=\"post\" >\n" +
                "            <input type=\"text\" name=\"username\">\n" +
                "            <input type=\"text\" name=\"message\">\n" +
                "            <input type=\"submit\" value=\"Send\">\n" +
                "        </form>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";


        return html;


    }

    /**
     * This method read the input stream from a socket.
     *
     * @param socket : The socket to be readed.
     * @return : A List<String> with the lines readed.
     * @throws IOException .
     */
    public static List<String> readSocketInput(Socket socket) throws IOException {

        List<String> input = new ArrayList<String>();
        InputStream is = socket.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        String line = "";

        while (true) {

            line = bf.readLine();

            boolean isOpen = true;

            try {
                isOpen = bf.ready();
            } catch (Exception e) {
                isOpen = false;
            }

            if ((line == null || line.isEmpty()) && !isOpen) {

                log.debug(" * LINE:" + line + " BF STATUS" + bf.ready());
                break;

            } else if (line.isEmpty() && isOpen) {

                int contentLength = 0;

                for (String s : input) {
                    if (s.contains("Content-Length:")) {
                        contentLength = Integer.parseInt(s.substring(16));
                    }
                }


                // int contentLength = Integer.parseInt(input.get(3).substring(16));

                log.debug("CONTENT LENGTH: " + contentLength);


                char[] chars = new char[contentLength];

                for (int i = 0; i < contentLength; i++) {
                    chars[i] = (char) bf.read();

                }

                input.add(new String(chars));


                log.debug("CLOSING CONNECTION");
                break;

            } else {
                log.debug("LINE: " + line + " BF STATUS " + bf.ready());
                input.add(line);
            }

        }

        if (input.isEmpty()) {
            input.add("ERROR");
        }
        return input;
    }

    public static boolean addMessage(List<String> input) {

        if (input.isEmpty()) {
            return false;
        }

        String bodyContent = input.get(input.size() - 1);
        bodyContent = bodyContent.replace("username=", "");
        bodyContent = bodyContent.replace("message=", "");
        log.debug("BODYCONTENCT COM ERRO = {}", bodyContent);
        String username = bodyContent.substring(0, bodyContent.indexOf('&'));
        String message = bodyContent.substring(bodyContent.indexOf('&') + 1, bodyContent.length());

        message = message.replace('+', ' ');

        log.debug("USERNAME: " + username + " MESSAGE: " + message);

        Message newMessage = new Message(username, message);
        messages.add(newMessage);

        return true;
    }


}
