package com.fluxtream.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */

/**
 * Message envelope resulting from a Ping operation
 */
@XmlRootElement
public class Status {

    public Status(){}

    Status(final String message) {
        this.message = message;
    }

    /**
     * A kind message that informs you of the Ping prod's success
     */
    public String message;
}
