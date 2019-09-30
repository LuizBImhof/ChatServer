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

    private static final String htmlPart1 = "<!DOCTYPE HTML>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<title>Chat-Server</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div>\n" +
            "<textarea readonly style=\"height:50vh; width:75vh; \">\n";

    private static final String htmlPart2 = " </textarea>\n"+
            "    </div>\n" +
            "    <div>\n" +
            "        <form action=\"/\" method=\"post\" >\n" +
            "            <input type=\"text\" name=\"username\">\n" +
            "            <input type=\"text\" name=\"message\">\n" +
            "            <input type=\"submit\" value=\"Send\">\n" +
            "        </form>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";

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
        final String request = lines.get(0);//Primeira linha = metodo de request (GET ou POST)
        log.debug("Request: {}", request);
        final PrintWriter pw = new PrintWriter(socket.getOutputStream());
        if (request.contains("GET")) {
            log.debug("GET REQUEST");
            printHttp(pw);
        } else if (request.contains("POST")) {
            log.debug("POST REQUEST");
            if (addMessage(lines)) {
                printHttp(pw);
                log.debug("MESSAGE ADDED SUCCESFULLY");
            } else {
                pw.println("HTTP/1.1 400 ERROR");
                pw.println("Server: DSM v0.0.1");
                pw.println();
                pw.flush();
            }
        } else {
            log.debug("ERROR REQUEST");
            pw.println("HTTP/1.1 400 ERROR");
            pw.println("Server: DSM v0.0.1");
            pw.println();
            pw.flush();
        }
        log.debug("Process ended.");
    }


    private static void printHttp(PrintWriter pw) {
        pw.println("HTTP/1.1 200 OK");
        pw.println("Server: DSM v0.0.1");
        pw.println("Date: " + new Date());
        pw.println("Content-Type: text/html; charset=UTF-8");
        pw.println();
        pw.println(completeHtml());
        pw.println();
        pw.flush();
    }

    /**
     * Read all the input stream with a scanner.
     *
     * @param socket to use to read.
     * @return all the string readed.
     */
    private static List<String> readInputStreamByLines(final Socket socket) throws IOException {
        //FIXME Make it work with the scanner

        final InputStream is = socket.getInputStream();
        // The list of string read from inputstream.
        final List<String> lines = new ArrayList<>();
        // The Scanner
        final Scanner s = new Scanner(is).useDelimiter("\\A");

        int contentLength = 0;
        String postData = "";
        log.debug("Reading the Inputstream ..");
        Boolean post = false;
        while (true) {
            final String line = s.nextLine();

            if (line.length() == 0) {
                break;
            } else {
                lines.add(line);
                if(line.contains("Content-Length:")){
                    contentLength = Integer.valueOf(line.substring(16));
                    log.debug("CONTENT lENGTH = {}",contentLength);
                }
                if (line.contains("POST")){
                    post = true;
                }
                log.debug("Info da linha {} ",line);
            }
        }
        return lines;
    }

    /**
     *Completes the HTML with the messages.
     *
     * @return the full HTML.
     */
    private static String completeHtml(){
        String html = htmlPart1;
        for (Message i : messages) {
            if (!i.toString().isEmpty()){
                html += i.toString() + "\n";
            }else {

            }
        }
        html += htmlPart2;
        return html;
    }

    /**
     * Read the input stream from a socket using bufferedReader.
     *
     * @param socket : The socket to be read.
     * @return : A List<String> with the lines that were read.
     * @throws IOException .
     */
    public static List<String> readSocketInput(Socket socket) throws IOException {

        List<String> input = new ArrayList<String>();
        InputStream inputStream = socket.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String postBody = "";
        int contentLength = 0;

        while (true) {
            line = bufferedReader.readLine();
            boolean isOpen = true;
            try {
                isOpen = bufferedReader.ready();
            } catch (Exception e) {
                isOpen = false;
            }
            if ((line == null || line.isEmpty()) && !isOpen) { // linha vazia e bufferedreader fechado (significa que nao tem o que ler)
                log.debug(" * LINE: {}, BUFFEREDREADER STATUS {} ", line ,bufferedReader.ready());
                break;
            } else if (line.isEmpty() && isOpen) { // linha vazia e buferredreader aberto (significa que chegou na ultima linha do header)
                log.debug("CONTENT LENGTH: " + contentLength);
                char[] chars = new char[contentLength];
                for (int i = 0; i < contentLength; i++) { //le char por char (read) pois nao tem endline (por ser o body), limitado por content length, que é o numero de bytes, e nesse caso 1 byte = 1 char
                  chars[i] = (char) bufferedReader.read();
                }
                postBody = new String(chars);
                input.add(postBody); // adiciona o Body
                log.debug("BODY : {}", postBody);
                log.debug("CLOSING CONNECTION");
                break;
            } else { // linha nao vazia e buffered reader aberto (Header do metodo)
                if(line.contains("Content-Length:")){ // enquanto adiciona as lin
                  contentLength = Integer.valueOf(line.substring(16));
                }
                input.add(line);
            }
        }
        if (input.isEmpty()) {
          input.add("ERROR");
        }
        return input;
    }

    /**
     * Adds the last message received to the list of messages.
     *
     * @param input String list representing all the chat messages.
     * @return false if there are no messages and true if there were messages added to the list.
     */
    public static boolean addMessage(List<String> input) {

        if (input.isEmpty()) { // sem mensagens
          return false;
        }

        String bodyContent = input.get(input.size() - 1); // pega a ultima string da lista (Ultima mensagem)
        bodyContent = bodyContent.replace("username=", ""); //Remove as informacoes desnecessarias do corpo
        bodyContent = bodyContent.replace("message=", "");

        String username = bodyContent.substring(0, bodyContent.indexOf('&')); //salva as informacoes necessarias em Strings para o usuario e a msg
        String message = bodyContent.substring(bodyContent.indexOf('&') + 1, bodyContent.length());

        log.debug("USERNAME: " + username + " MESSAGE: " + message);

        Message newMessage = new Message(username, message);
        messages.add(newMessage); //adiciona na lista de mensagens

        return true;
    }

}
