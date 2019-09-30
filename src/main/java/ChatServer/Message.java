/*
 * Copyright (c) 2019. -- Luiz Artur Boing Imhof
 */

package ChatServer;

public class Message {

    private String user;
    private String message;

    /**
     * Constructor.
     *
     * @param user is the user of the message
     * @param message is the message to show
     */
    public Message(String user, String message) {

        this.user = user.replace('+', ' ').trim();
        this.message = message.replace('+', ' ').trim();
    }

    /**
     *
     * @return the user of the message
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user The user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param message the message to be shown
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        String result;
        if (user.isEmpty()) {
            result = "";
        } else {
            result = (user + ": " + message);
        }
        return result;
    }
}
