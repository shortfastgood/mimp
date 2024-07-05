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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String DATA_BUFFER_KEY = "dataBuffer";

    private static final String PROTOCOL_KEY = "protocol";

    private static final String REQUEST_KEY = "request";

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
     * @return Protocol and data buffer
     */
    Map<String, Object> checkProtocol(final DataInputStream proxySocketIn) {


        byte[] buffer = iOCallback.inAndWait(proxySocketIn);

        boolean hasTextualProlog = true;
        for (int i=0; i < buffer.length && i < 3; i++) {
            if (!Alphabet.UPPERCASE.contains((char)buffer[i]) && !Alphabet.LOWERCASE.contains((char)buffer[i])) {
                hasTextualProlog = false;
                i = 4;
            }
        }

        Map<String, Object> result = new HashMap<>();

        if (hasTextualProlog) {
            HTTPRequest request = HTTPFunctions.createRequest(HTTPFunctions.getHead(buffer));
            result.put(DATA_BUFFER_KEY, buffer);
            result.put(REQUEST_KEY, request);
            result.put(PROTOCOL_KEY, request.protocol());
        } else {
            result.put(DATA_BUFFER_KEY, buffer);
            result.put(PROTOCOL_KEY, Protocol.UNKNOWN);
        }

        return result;
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

        while (!isNotConnectedOrOpen()) {
            Map<String, Object> protocolAndData;
            try {
                protocolAndData = checkProtocol(proxySocketIn);
            } catch (Exception e) {
                LOGGER.error("Error checking protocol {}", e.getMessage());
                cleanup();
                return;
            }
            Protocol protocol = (Protocol) protocolAndData.get(PROTOCOL_KEY);
            if(Protocol.HTTP_1_0.equals(protocol) || Protocol.HTTP_1_1.equals(protocol)) {
                byte[] buffer = (byte[]) protocolAndData.get(DATA_BUFFER_KEY);
                HTTPRequest request = (HTTPRequest) protocolAndData.get(REQUEST_KEY);
                byte[] body = HTTPFunctions.getBody(buffer);
                handleHTTP1_1(request, body, proxySocketIn, remoteSocketOut, remoteSocketIn, proxySocketOut);
            } else {
                LOGGER.warn("Unknown protocol. Switching to binary asynchronous I/O");
                break;
            }
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
            DataOutputStream proxySocketOut) {

        // patch headers
        List<HTTPHeader> additionalHeaders = HTTPFunctions.getHeaders(properties.getProperty(MIMPConstants.PROXY_HEADERS_KEY, ""));
        if (!additionalHeaders.isEmpty()) {
            HTTPFunctions.addOrReplaceHeaders(request, additionalHeaders);
        }
        if (traceHeaders) {
            StringBuilder buf = new StringBuilder();
            buf.append("\n>> ").append("port ").append(proxySocket.getLocalPort()).append(" --> port ").append(remoteSocket.getPort());
            buf.append("\n>> ").append(request.method().name()).append(" ").append(request.requestURI()).append(" ").append(request.protocol().getProtocolString());
            request.headers().forEach(h -> buf.append(String.format("\n>> %s: %s", h.name(), String.join(",", h.values()))));
            buf.append('\n');
            LOGGER.info(buf.toString());
        }

        byte[] requestBuffer = HTTPFunctions.requestToBytes(request);
        byte[] outBuffer = new byte[requestBuffer.length + body.length];
        System.arraycopy(requestBuffer, 0, outBuffer, 0, requestBuffer.length);
        System.arraycopy(body, 0, outBuffer, requestBuffer.length, body.length);

        try {
            iOCallback.out(remoteSocketOut, outBuffer);
        } catch (RuntimeException re) {
            LOGGER.error("Error forwarding request to remote {}", re.getMessage());
            cleanup();
            return;
        }

        // handle large posts
        try {
            while ((HTTPMethod.POST.equals(request.method()) || HTTPMethod.PUT.equals(request.method())) && proxySocketIn.available() > 0) {
                byte[] buffer = iOCallback.in(proxySocketIn);
                if (buffer.length > 0) {
                    iOCallback.out(remoteSocketOut, buffer);
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Error reading extended body from proxy socket {}", ioe.getMessage());
            cleanup();
            return;
        }

        byte[] buffer;
        try {
            buffer = iOCallback.inAndWait(remoteSocketIn);
        } catch (Exception e) {
            LOGGER.error("Error reading response from remote {}", e.getMessage());
            cleanup();
            return;
        }

        HTTPResponse response = HTTPFunctions.createResponse(HTTPFunctions.getHead(buffer));
        if (traceHeaders) {
            StringBuilder buf = new StringBuilder();
            buf.append("\n<< ").append("port ").append(proxySocket.getLocalPort()).append(" <-- port ").append(remoteSocket.getPort());
            buf.append("\n<< ").append(response.protocol().getProtocolString()).append(" ").append(response.statusCode()).append(" ").append(response.reasonPhrase());
            response.headers().forEach(h -> buf.append(String.format("\n<< %s: %s", h.name(), String.join(",", h.values()))));
            buf.append('\n');
            LOGGER.info(buf.toString());
        }

        try {
            iOCallback.out(proxySocketOut, buffer);
        } catch (RuntimeException re) {
            LOGGER.error("Error forwarding response to proxy {}", re.getMessage());
            cleanup();
            return;
        }

            try {
                while (remoteSocketIn.available() > 0) {
                    buffer = iOCallback.in(remoteSocketIn);
                    if (buffer.length > 0) {
                        iOCallback.out(proxySocketOut, buffer);
                    }
                }
            } catch (IOException ioe) {
                LOGGER.error("Error forwarding extended body from remote socket {}", ioe.getMessage());
                cleanup();
                return;
            }
    }

}
