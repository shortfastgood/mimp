/*
 * @(#)MIMPServerSocketHandler.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import org.homedns.dpaevd.mimp.api.MIMPConstants;
import org.homedns.dpaevd.mimp.api.config.IMIMPProperties;
import org.homedns.dpaevd.mimp.api.network.IMIMPIOCallback;
import org.homedns.dpaevd.mimp.api.network.IMIMPServerSocketHandler;
import org.homedns.dpaevd.mimp.api.network.MIMPSocketHandlerStatus;
import org.homedns.dpaevd.mimp.api.network.Protocol;
import org.homedns.dpaevd.mimp.api.util.Alphabet;
import org.homedns.dpaevd.mimp.impl.http.HTTPFunctions;
import org.homedns.dpaevd.mimp.impl.http.HTTPHeader;
import org.homedns.dpaevd.mimp.impl.http.HTTPMethod;
import org.homedns.dpaevd.mimp.impl.http.HTTPRequest;
import org.homedns.dpaevd.mimp.impl.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MIMP server socket handler.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPServerSocketHandler implements IMIMPServerSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIMPServerSocketHandler.class);

    private ExecutorService inBoundWorkerExecutor;

    private final IMIMPIOCallback iOCallback;

    private ExecutorService outBoundWorkerExecutor;

    private final IMIMPProperties properties;

    private final Socket proxySocket;

    private DataInputStream proxySocketIn;

    private DataOutputStream proxySocketOut;

    private final Socket remoteSocket;

    private DataInputStream remoteSocketIn;

    private DataOutputStream remoteSocketOut;

    private final String remoteInfo;

    private volatile MIMPSocketHandlerStatus status;

    private final boolean traceHeaders;

    public MIMPServerSocketHandler(final IMIMPIOCallback iOCallback, IMIMPProperties properties, final Socket proxySocket, final Socket remoteSocket) {
        this.iOCallback = iOCallback;
        this.properties = properties;
        this.proxySocket = proxySocket;
        this.remoteSocket = remoteSocket;
        this.status = MIMPSocketHandlerStatus.CONNECTED;
        this.remoteInfo = remoteSocket.getInetAddress().getHostAddress() + ":" + remoteSocket.getPort();
        this.traceHeaders = Boolean.parseBoolean(properties.getProperty(MIMPConstants.PROXY_TRACE_HEADERS_KEY, "false"));
    }


    /**
     * Checks the protocol.
     * @param proxySocketIn The input stream of the proxy socket.
     * @param remoteSocketOut The output stream of the remote socket.
     * @param remoteSocketIn The input stream of the remote socket.
     * @param proxySocketOut The output stream of the proxy socket.
     * @param recursive The flag indicating if the method should call itself.
     */
    void checkProtocol(
            final DataInputStream proxySocketIn,
            final DataOutputStream remoteSocketOut,
            final DataInputStream remoteSocketIn,
            final DataOutputStream proxySocketOut,
            boolean recursive) {
        byte[] buffer = iOCallback.in(this, proxySocketIn);
        if (isNotConnectedOrOpen()) {
            return;
        }

        boolean isRequest = true;
        for (int i=0; i < buffer.length && i < 3; i++) {
            if (!Alphabet.UPPERCASE.contains((char)buffer[i]) && !Alphabet.LOWERCASE.contains((char)buffer[i])) {
                isRequest = false;
                i = 4;
            }
        }

        if (isRequest) {
            HTTPRequest request = HTTPFunctions.createRequest(HTTPFunctions.getHead(buffer));
            if (Protocol.HTTP_1_0.equals(request.protocol()) || Protocol.HTTP_1_1.equals(request.protocol())) {
                handleHTTP1_1(request, HTTPFunctions.getBody(buffer), proxySocketIn, remoteSocketOut, remoteSocketIn, proxySocketOut, recursive);
            } else {
                iOCallback.out(this, remoteSocketOut, buffer);
            }
        } else {
            iOCallback.out(this, remoteSocketOut, buffer);
        }
    }

    @Override public void cleanup() {
        status = MIMPSocketHandlerStatus.DISCONNECTED;
        if (remoteSocketIn != null) {
            try {
                remoteSocketIn.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket input stream! Cause: {}", ioe.getMessage());
            }
            remoteSocketIn = null;
            LOGGER.debug("Closed remote socket input stream");
        }
        if (remoteSocketOut != null) {
            try {
                remoteSocketOut.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket output stream! Cause: {}", ioe.getMessage());
            }
            remoteSocketOut = null;
            LOGGER.debug("Closed remote socket output stream");
        }
        if (remoteSocket != null && !remoteSocket.isClosed()) {
            try {
                remoteSocket.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket! Cause: {}", ioe.getMessage());
            }
            LOGGER.debug("Closed remote socket");
        }
        if (proxySocketIn != null) {
            try {
                proxySocketIn.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket input stream! Cause: {}", ioe.getMessage());
            }
            proxySocketIn = null;
            LOGGER.debug("Closed proxy socket input stream");
        }
        if (proxySocketOut != null) {
            try {
                proxySocketOut.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket output stream! Cause: {}", ioe.getMessage());
            }
            proxySocketOut = null;
            LOGGER.debug("Closed proxy socket output stream");
        }
        if (proxySocket != null && !proxySocket.isClosed()) {
            try {
                proxySocket.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket! Cause: {}", ioe.getMessage());
            }
            LOGGER.debug("Closed proxy socket");
        }
        if (inBoundWorkerExecutor != null) {
            inBoundWorkerExecutor.shutdown();
            LOGGER.info("Shutdown worker executor for inbound traffic {}", remoteInfo);
        }
        if (outBoundWorkerExecutor != null) {
            outBoundWorkerExecutor.shutdown();
            LOGGER.info("Shutdown worker executor for outbound traffic {}", remoteInfo);
        }
    }

    @Override public Socket getProxySocket() {
        return proxySocket;
    }

    @Override public Socket getRemoteSocket() {
        return remoteSocket;
    }

    @Override public MIMPSocketHandlerStatus getStatus() {
        return this.status;
    }

    @Override public boolean isNotConnectedOrOpen() {
        return proxySocket.isClosed() || remoteSocket.isClosed() || !MIMPSocketHandlerStatus.CONNECTED.equals(status);
    }

    @Override public void execute() {

        try {
            proxySocketIn = new DataInputStream(proxySocket.getInputStream());
            proxySocketOut = new DataOutputStream((proxySocket.getOutputStream()));
            remoteSocketIn = new DataInputStream(remoteSocket.getInputStream());
            remoteSocketOut = new DataOutputStream((remoteSocket.getOutputStream()));
        } catch (IOException ioe) {
            LOGGER.error("Cannot initialize socket IO! Cause: {}", ioe.getMessage());
            status = MIMPSocketHandlerStatus.ERROR;
            cleanup();
            return;
        }

        LOGGER.info("Establish IO with remote {}", remoteInfo);

        checkProtocol(proxySocketIn, remoteSocketOut, remoteSocketIn, proxySocketOut, true);

        if (isNotConnectedOrOpen()) {
            return;
        }

        inBoundWorkerExecutor = Executors.newFixedThreadPool(1);
        outBoundWorkerExecutor = Executors.newFixedThreadPool(1);

        outBoundWorkerExecutor.submit(() -> iOCallback.proxyInRemoteOut(this, proxySocketIn, remoteSocketOut));
        inBoundWorkerExecutor.submit(() -> iOCallback.remoteInProxyOut(this, remoteSocketIn, proxySocketOut));
    }

    void handleHTTP1_1(
            HTTPRequest request,
            byte[] body,
            DataInputStream proxySocketIn,
            DataOutputStream remoteSocketOut,
            DataInputStream remoteSocketIn,
            DataOutputStream proxySocketOut,
            boolean recursive) {

        // patch headers
        List<HTTPHeader> additionalHeaders = HTTPFunctions.getHeaders(properties.getProperty(MIMPConstants.PROXY_HEADERS_KEY, ""));
        if (!additionalHeaders.isEmpty()) {
            HTTPFunctions.addOrReplaceHeaders(request, additionalHeaders);
        }
        if (traceHeaders) {
            StringBuilder buf = new StringBuilder();
            buf.append("\n>> ").append(request.method().name()).append(" ").append(request.requestURI()).append(" ").append(request.protocol().getProtocolString());
            request.headers().forEach(h -> buf.append(String.format("\n>> %s: %s", h.name(), String.join(",", h.values()))));
            buf.append('\n');
            LOGGER.info(buf.toString());
        }

        byte[] requestBuffer = HTTPFunctions.requestToBytes(request);
        byte[] outBuffer = new byte[requestBuffer.length + body.length];
        System.arraycopy(requestBuffer, 0, outBuffer, 0, requestBuffer.length);
        System.arraycopy(body, 0, outBuffer, requestBuffer.length, body.length);

        iOCallback.out(this, remoteSocketOut, outBuffer);
        if (isNotConnectedOrOpen()) {
            return;
        }

        // handle large posts
        try {
            while ((HTTPMethod.POST.equals(request.method()) || HTTPMethod.PUT.equals(request.method())) && proxySocketIn.available() > 0) {
                byte[] buffer = iOCallback.in(this, proxySocketIn);
                if (buffer.length > 0) {
                    iOCallback.out(this, remoteSocketOut, buffer);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Error reading extended body from proxy socket {}", ioe.getMessage());
            cleanup();
            return;
        }

        byte[] buffer = iOCallback.remoteIn(this, remoteSocketIn);
        if (isNotConnectedOrOpen()) {
            return;
        }
        HTTPResponse response = HTTPFunctions.createResponse(HTTPFunctions.getHead(buffer));
        if (traceHeaders) {
            StringBuilder buf = new StringBuilder();
            buf.append("\n<< ").append(response.protocol().getProtocolString()).append(" ").append(response.statusCode()).append(" ").append(response.reasonPhrase());
            response.headers().forEach(h -> buf.append(String.format("\n<< %s: %s", h.name(), String.join(",", h.values()))));
            buf.append('\n');
            LOGGER.info(buf.toString());
        }

        iOCallback.out(this, proxySocketOut, buffer);
        if (isNotConnectedOrOpen()) {
            return;
        }

        try{
            while (remoteSocketIn.available() > 0) {
                buffer = iOCallback.remoteIn(this, remoteSocketIn);
                if (buffer.length > 0) {
                    iOCallback.out(this, proxySocketOut, buffer);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Error reading from remote socket {}", ioe.getMessage());
            cleanup();
            return;
        }

        while (recursive && !isNotConnectedOrOpen()) {
            checkProtocol(proxySocketIn, remoteSocketOut, remoteSocketIn, proxySocketOut, false);
        }

        if (recursive) {
            cleanup();
        }
    }
}
