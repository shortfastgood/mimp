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

import org.homedns.dpaevd.mimp.api.network.Protocol;

/**
 * Functions for HTTP.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface HTTPFunctions {

    static void addOrReplaceHeaders(HTTPRequest request,  final List<HTTPHeader> additionalHeaders) {
        List<HTTPHeader> newHeaders = new ArrayList<>();
        additionalHeaders.forEach(h -> {
            boolean found = false;
            for( HTTPHeader header : request.headers() ) {
                if(h.name().equals(header.name()) ) {
                    found = true;
                    break;
                }
            }
            if( !found ) {
                newHeaders.add(h);
            }
        });
        request.headers().addAll(newHeaders);
    }

    static HTTPRequest createRequest(final String buffer) {
        String[] lines = buffer.split("\r\n");
        if( lines.length < 1 ) {
            throw new IllegalArgumentException("Invalid HTTP request, at least 1 line is required");
        }
        String[] requestLine = lines[0].split(" ");
        if( requestLine.length != 3 ) {
            throw new IllegalArgumentException("Invalid HTTP request line, 3 parts are required");
        }
        HTTPRequest request = new HTTPRequest(HTTPMethod.valueOf(requestLine[0]), requestLine[1], Protocol.getProtocol(requestLine[2]), new ArrayList<>());

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

    static HTTPResponse createResponse(final String buffer) {
        String[] lines = buffer.split("\r\n");
        if( lines.length < 1 ) {
            throw new IllegalArgumentException("Invalid HTTP response, at least 1 line is required");
        }
        String[] responseLine = lines[0].split(" ");
        if( responseLine.length < 2 ) {
            throw new IllegalArgumentException("Invalid HTTP response line, 2 parts are required");
        }
        String reasonPhrase = responseLine.length > 2 ? responseLine[2] : "N/A";
        HTTPResponse response = new HTTPResponse(Protocol.getProtocol(responseLine[0]), Integer.parseInt(responseLine[1]), reasonPhrase, new ArrayList<>());

        for( int i = 1; i < lines.length; i++ ) {
            String[] header = lines[i].split(": ");
            if( header.length == 2 ) {
                String headerName = header[0];
                String[] headerValues = header[1].split(",");
                HTTPHeader httpHeader = new HTTPHeader(headerName, List.of(headerValues));
                response.headers().add(httpHeader);
            }
        }
        return response;
    }

    static byte[] getBody(final byte[] buffer) {
        boolean found = false;
        int i = 0;
        for(; i < (buffer.length - 4) && !found; i++) {
            if (buffer[i] == '\r' && buffer[i+1] == '\n' && buffer[i+2] == '\r' && buffer[i+3] == '\n') {
                found = true;
                i += 3; // +1 is added by the loop !
            }
        }
        byte[] body = new byte[buffer.length - i];
        if (i > 0) {
            System.arraycopy(buffer, i, body, 0, buffer.length - i);
        }
        return body;
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

    static byte[] requestToBytes(final HTTPRequest request) {
        StringBuilder buf = new StringBuilder();
        buf.append(request.method().name()).append(" ").append(request.requestURI()).append(" ").append(request.protocol().getProtocolString()).append("\r\n");
        for( HTTPHeader header : request.headers() ) {
            buf.append(header.name()).append(": ").append(String.join(",", header.values())).append("\r\n");
        }
        buf.append("\r\n");
        return buf.toString().getBytes();
    }
}
