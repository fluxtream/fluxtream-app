package org.fluxtream.core.auth;

/**
 * User: candide
 * Date: 30/04/14
 * Time: 23:09
 */
public class DeveloperRoleAddedException extends org.springframework.security.core.AuthenticationException {

    public DeveloperRoleAddedException(String msg) {
        super(msg);
    }
}
