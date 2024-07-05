/*
 * @(#)HTTPRequest.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.http;

import java.util.List;

import org.homedns.dpaevd.mimp.api.network.Protocol;

/**
 * Simplified HTTP response for proxy purposes.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public record HTTPResponse(Protocol protocol, int statusCode, String reasonPhrase, List<HTTPHeader> headers) {
    public HTTPResponse {
        if(protocol == null) {
            protocol = Protocol.UNKNOWN;
        }
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("Invalid HTTP status statusCode");
        }
        if (reasonPhrase == null || reasonPhrase.isBlank()) {
            throw new IllegalArgumentException("Invalid HTTP status reasonPhrase");
        }
        if (headers == null) {
            headers = List.of();
        }
    }

    public boolean isWebSocketUpgrade() {
        return statusCode == 101 && headers.stream().anyMatch(h -> h.name().equalsIgnoreCase("Upgrade") && h.values().contains("websocket"));
    }

    @Override public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("HTTP/1.1 ").append(statusCode).append(" ").append(reasonPhrase).append("\r\n");
        for (HTTPHeader header : headers) {
            buf.append(header.name()).append(": ").append(String.join(",", header.values())).append("\r\n");
        }
        buf.append("\r\n");
        return buf.toString();
    }
}
