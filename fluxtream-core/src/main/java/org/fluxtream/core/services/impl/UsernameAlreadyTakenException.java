package org.fluxtream.core.services.impl;

/**
 * User: candide
 * Date: 09/09/13
 * Time: 12:05
 */
public class UsernameAlreadyTakenException extends Throwable {
    public UsernameAlreadyTakenException(final String s) {
        super(s);
    }
}
