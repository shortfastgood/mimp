/*
 * @(#)MIMPServerSocketHandler.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.homedns.dpaevd.mimp.api.MIMPConstants;
import org.homedns.dpaevd.mimp.api.config.IMIMPProperties;
import org.homedns.dpaevd.mimp.api.network.IMIMPIOCallback;
import org.homedns.dpaevd.mimp.api.network.IMIMPServerSocketHandler;
import org.homedns.dpaevd.mimp.api.network.MIMPSocketHandlerStatus;
import org.homedns.dpaevd.mimp.impl.http.HTTPFunctions;
import org.homedns.dpaevd.mimp.impl.http.HTTPHeader;
import org.homedns.dpaevd.mimp.impl.http.HTTPRequest;
import org.homedns.dpaevd.mimp.impl.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public void cleanup() {
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

    @Override
    public void execute() {
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

        inBoundWorkerExecutor = Executors.newFixedThreadPool(1);
        outBoundWorkerExecutor = Executors.newFixedThreadPool(1);

        outBoundWorkerExecutor.submit(() -> handleProxyToRemoteStream(proxySocketIn, remoteSocketOut));
        inBoundWorkerExecutor.submit(() -> handleRemoteToProxyStream(remoteSocketIn, proxySocketOut));

    }

    protected int getHttpProtocolStartIndex(final byte[] buffer) {
        int index = -1;
        for (int i = 0; i < buffer.length - 5; i++) {
            if (buffer[i] == 'H' && buffer[i + 1] == 'T' && buffer[i + 2] == 'T' && buffer[i + 3] == 'P' && buffer[i + 4] == '/' && buffer[i + 5] == '1') {
                index = i;
                break;
            }
        }
        return index;
    }

    protected int getRequestStartIndex(final byte[] buffer) {
        int index = -1;
        for (int i = 0; i < buffer.length - 5; i++) {
            if (buffer[i] == 'G' && buffer[i + 1] == 'E' && buffer[i + 2] == 'T' && buffer[i + 3] == ' ' && buffer[i + 4] == '/') {
                index = i;
                break;
            } else if (buffer[i] == 'H' && buffer[i + 1] == 'E' && buffer[i + 2] == 'A' && buffer[i + 3] == 'D' && buffer[i + 4] == ' ') {
                index = i;
                break;
            } else if (buffer[i] == 'P' && buffer[i + 1] == 'O' && buffer[i + 2] == 'S' && buffer[i + 3] == 'T' && buffer[i + 4] == ' ') {
                index = i;
                break;
            } else if (buffer[i] == 'P' && buffer[i + 1] == 'U' && buffer[i + 2] == 'T' && buffer[i + 3] == ' ') {
                index = i;
                break;
            } else if (buffer[i] == 'D' && buffer[i + 1] == 'E' && buffer[i + 2] == 'L' && buffer[i + 3] == 'E' && buffer[i + 4] == 'T') {
                index = i;
                break;
            }
        }
        return index;
    }

    protected int getHttpHeaderEndIndex(final byte[] buffer) {
        int index = -1;
        for (int i = 0; i < buffer.length - 3; i++) {
            if (buffer[i] == '\r' && buffer[i + 1] == '\n' && buffer[i + 2] == '\r' && buffer[i + 3] == '\n') {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    public Socket getProxySocket() {
        return proxySocket;
    }

    @Override
    public Socket getRemoteSocket() {
        return remoteSocket;
    }

    @Override
    public MIMPSocketHandlerStatus getStatus() {
        return status;
    }

    public void handleProxyToRemoteStream(final DataInputStream in, final DataOutputStream out) {
        try {
            while(!isNotConnectedOrOpen()) {
                byte[] buffer = iOCallback.in(in);
                if (buffer.length > 0) {
                    int httpStartIndex = getRequestStartIndex(buffer);
                    if (httpStartIndex < 0) {
                        iOCallback.out(out, buffer);
                    } else {
                        int httpEndIndex = getHttpHeaderEndIndex(buffer);
                        if (httpEndIndex < 0) {
                            iOCallback.out(out, buffer);
                        } else {
                            String httpHeader = new String(buffer, httpStartIndex, httpEndIndex - httpStartIndex + 4);
                            HTTPRequest request = HTTPFunctions.createRequest(httpHeader);

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
                            byte[] outBuffer = new byte[buffer.length - httpHeader.length() + requestBuffer.length];
                            System.arraycopy(buffer, 0, outBuffer, 0, httpStartIndex);
                            System.arraycopy(requestBuffer, 0, outBuffer, httpStartIndex, requestBuffer.length);
                            System.arraycopy(buffer, httpEndIndex + 4, outBuffer, httpStartIndex + requestBuffer.length, buffer.length - httpEndIndex - 4);

                            iOCallback.out(remoteSocketOut, outBuffer);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling proxy to remote stream! Cause: {}", e.getMessage());
            status = MIMPSocketHandlerStatus.ERROR;
            cleanup();
        }
    }

    public void handleRemoteToProxyStream(final DataInputStream in, final DataOutputStream out) {
        try {
            while(!isNotConnectedOrOpen()) {
                byte[] buffer = iOCallback.in(in);
                if (buffer.length > 0) {
                    int httpStartIndex = getHttpProtocolStartIndex(buffer);
                    if (httpStartIndex < 0) {
                        iOCallback.out(out, buffer);
                    } else {
                        int httpEndIndex = getHttpHeaderEndIndex(buffer);
                        if (httpEndIndex < 0) {
                            iOCallback.out(out, buffer);
                        } else {
                            String httpHeader = new String(buffer, httpStartIndex, httpEndIndex - httpStartIndex + 4);
                            HTTPResponse response = HTTPFunctions.createResponse(httpHeader);
                            if (traceHeaders) {
                                StringBuilder buf = new StringBuilder();
                                buf.append("\n<< ").append("port ").append(proxySocket.getLocalPort()).append(" <-- port ").append(remoteSocket.getPort());
                                buf.append("\n<< ").append(response.protocol().getProtocolString()).append(" ").append(response.statusCode()).append(" ").append(response.reasonPhrase());
                                response.headers().forEach(h -> buf.append(String.format("\n<< %s: %s", h.name(), String.join(",", h.values()))));
                                buf.append('\n');
                                LOGGER.info(buf.toString());
                            }
                            iOCallback.out(out, buffer);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling proxy to remote stream! Cause: {}", e.getMessage());
            status = MIMPSocketHandlerStatus.ERROR;
            cleanup();
        }
    }

    @Override
    public boolean isNotConnectedOrOpen() {
        return proxySocket.isClosed() || remoteSocket.isClosed() || !MIMPSocketHandlerStatus.CONNECTED.equals(status);
    }
}
