/*
 * @(#)Protocol.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

/**
 * Supported protocols.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public enum Protocol {
    HTTP_1_0("HTTP/1.0"),  HTTP_1_1("HTTP/1.1"), UNKNOWN("");

    private final String protocolString;

    Protocol(final String protocolString) {
        this.protocolString = protocolString;
    }

    public String getProtocolString() {
        return protocolString;
    }

    /**
     * Returns the protocol.
     * @param protocolString The protocol string.
     * @ return The protocol.
     */
    public static Protocol getProtocol(final String protocolString) {
        for (Protocol protocol : Protocol.values()) {
            if (protocol.protocolString.equals(protocolString)) {
                return protocol;
            }
        }
        return UNKNOWN;
    }
}
