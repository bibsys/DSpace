package org.dspace.uclouvain.exceptions.mail;

public class EmailGenerationException extends Exception {
    public EmailGenerationException(String message) {
        super(message);
    }

    public EmailGenerationException(String message, Exception e) {
        super(message,e);
    }
}
