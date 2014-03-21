package org.fluxtream.connectors.mymee;

import java.security.Provider;

public class MymeeCrypto extends Provider {
    private static final long serialVersionUID = -2599394895077747264L;

    public MymeeCrypto() {
        super("MymeeCrypto", 0.1, "MyCrypto v0.1, implementing SHA-224");
        put("MessageDigest.SHA-224", "org.fluxtream.connectors.mymee.SHA224");
    }

}