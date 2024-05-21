/*
 * @(#)HTTPFunctions.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Functions for HTTP.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface HTTPFunctions {

    static byte[] addOrReplaceHeaders(final byte[] buffer, final List<HTTPHeader> additionalHeaders) {
        int bodyStart = 0;
        StringBuilder requestHeadBuffer = new StringBuilder();
        for(int i=0; i < (buffer.length - 4); i++) {
            if (buffer[i] == '\r' && buffer[i+1] == '\n' && buffer[i+2] == '\r' && buffer[i+3] == '\n') {
                bodyStart = i + 4;
                break;
            } else {
                requestHeadBuffer.append((char)buffer[i]);
            }
        }
        HTTPRequest request = createRequest(requestHeadBuffer.toString());
        if (!additionalHeaders.isEmpty()) {
            List<HTTPHeader> mergedHeaders = new ArrayList<>(additionalHeaders);
            request.headers().forEach(h -> {
                boolean found = false;
                for( HTTPHeader additionalHeader : additionalHeaders ) {
                    if(h.name().equals(additionalHeader.name()) ) {
                        found = true;
                        break;
                    }
                }
                if( !found ) {
                    mergedHeaders.add(h);
                }
            });
            request = new HTTPRequest(request.method(), request.requestURI(), request.protocol(), mergedHeaders);
        }
        String newRequestHeadString = request.toString();
        byte[] newRequestHead = new byte[newRequestHeadString.length()];
        for( int i = 0; i < newRequestHeadString.length(); i++ ) {
            newRequestHead[i] = (byte)newRequestHeadString.charAt(i);
        }

        // case 1: request without body; the request may be changed or not.
        if (newRequestHead.length >= buffer.length) {
            return newRequestHead;

        // case 2: request with body; the request may be changed or not.
        } else {
            byte[] newBuffer = new byte[newRequestHead.length + buffer.length - bodyStart];
            System.arraycopy(newRequestHead, 0, newBuffer, 0, newRequestHead.length);
            System.arraycopy(buffer, bodyStart, newBuffer, newRequestHead.length, buffer.length - bodyStart);
            return newBuffer;
        }
    }

    static String getHead(final byte[] buffer) {
        StringBuilder requestHeadBuffer = new StringBuilder();
        for(int i=0; i < (buffer.length - 4); i++) {
            if (buffer[i] == '\r' && buffer[i+1] == '\n' && buffer[i+2] == '\r' && buffer[i+3] == '\n') {
                break;
            } else {
                requestHeadBuffer.append((char)buffer[i]);
            }
        }
        return requestHeadBuffer.toString();
    }

    static List<HTTPHeader> getHeaders(final String headersString) {
        List<HTTPHeader> headers = new ArrayList<>();
        String[] headerParts = headersString.split(":::");
        for( String headerPart : headerParts ) {
            String[] header = headerPart.split(":");
            if( header.length == 2 ) {
                String headerName = header[0];
                String[] headerValues = header[1].split(",");
                HTTPHeader httpHeader = new HTTPHeader(headerName, Arrays.asList(headerValues));
                headers.add(httpHeader);
            }
        }
        return headers;
    }

    static HTTPRequest createRequest(final String buffer) {
        String[] lines = buffer.split("\r\n");
        if( lines.length < 1 ) {
            throw new IllegalArgumentException("Invalid HTTP request, at least 3 lines are required");
        }
        String[] requestLine = lines[0].split(" ");
        if( requestLine.length != 3 ) {
            throw new IllegalArgumentException("Invalid HTTP request line");
        }
        HTTPRequest request = new HTTPRequest(HTTPMethod.valueOf(requestLine[0]), requestLine[1], "HTTP/1.1", new ArrayList<>());

        for( int i = 1; i < lines.length; i++ ) {
            String[] header = lines[i].split(": ");
            if( header.length == 2 ) {
                String headerName = header[0];
                String[] headerValues = header[1].split(",");
                HTTPHeader httpHeader = new HTTPHeader(headerName, List.of(headerValues));
                request.headers().add(httpHeader);
            }
        }
        return request;
    }
}
