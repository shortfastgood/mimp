/*
 * @(#)HTTPRequest.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.homedns.dpaevd.mimp.api.network.Protocol;

/**
 * Simplified HTTP request for proxy purposes.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public record HTTPRequest(HTTPMethod method, String requestURI, Protocol protocol, List<HTTPHeader> headers) {
    public HTTPRequest {
        if (method == null) {
            throw new IllegalArgumentException("Invalid HTTP method");
        }
        if (requestURI == null || requestURI.isBlank()) {
            throw new IllegalArgumentException("Invalid request URI");
        }
        if (protocol == null) {
             protocol = Protocol.UNKNOWN;
        }
        if (headers == null) {
            headers = new ArrayList<>();
        }
    }

    public Optional<HTTPHeader> getHeader(String name) {
        return headers.stream().filter(h -> h.name().equals(name)).findFirst();
    }

    public boolean isKeepAlive() {
        Optional<HTTPHeader> connectionHeader = getHeader("Connection");
        return connectionHeader.map(httpHeader -> httpHeader.values().contains("keep-alive")).orElse(false);
    }

    @Override public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(method.name()).append(" ").append(requestURI).append(" ").append(protocol).append("\r\n");
        for (HTTPHeader header : headers) {
            buf.append(header.name()).append(": ").append(String.join(",", header.values())).append("\r\n");
        }
        buf.append("\r\n");
        return buf.toString();
    }
}
