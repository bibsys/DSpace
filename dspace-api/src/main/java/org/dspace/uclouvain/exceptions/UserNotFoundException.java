package org.dspace.uclouvain.exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String user) {
        super(user);
    }
}
