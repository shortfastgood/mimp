/*
 * @(#)MIMPIOCallback.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import org.homedns.dpaevd.mimp.api.MIMPConstants;
import org.homedns.dpaevd.mimp.api.network.IMIMPIOCallback;
import org.homedns.dpaevd.mimp.api.network.IMIMPServerSocketHandler;
import org.homedns.dpaevd.mimp.impl.config.MIMPProperties;
import org.homedns.dpaevd.mimp.impl.http.HTTPFunctions;
import org.homedns.dpaevd.mimp.impl.http.HTTPHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

/**
 * Handles the data exchange between the proxy and the remote.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPIOCallback implements IMIMPIOCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIMPIOCallback.class);

    private final MIMPProperties properties;

    boolean traceHeaders;

    public MIMPIOCallback(MIMPProperties properties) {
        this.properties = properties;
        this.traceHeaders = Boolean.parseBoolean(properties.getProperty(MIMPConstants.PROXY_TRACE_HEADERS_KEY, "false"));
    }

    @Override public void proxyInRemoteOut(IMIMPServerSocketHandler handler, DataInputStream proxyIn, DataOutputStream remoteOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            try {
                bytesRead = proxyIn.read(buffer);
            } catch (Exception e) {
                LOGGER.warn("Error in proxyIn {}", e.getMessage());
                handler.cleanup();
                return;
            }
            if (bytesRead == -1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (handler.isNotConnectedOrOpen()) {
                    return;
                }
            } else {
                byte[] outBuffer = new byte[bytesRead];
                System.arraycopy(buffer, 0, outBuffer, 0, bytesRead);

                // check if the buffer contains an HTTP request
                String bufferString = NetworkFunctions.toString(outBuffer, 0, bytesRead);
                if (Protocol.HTTP_1_1.equals(NetworkFunctions.getProtocol(bufferString))) {
                    List<HTTPHeader> headers = HTTPFunctions.getHeaders(properties.getProperty(MIMPConstants.PROXY_HEADERS_KEY));
                    if (!headers.isEmpty()) {
                        outBuffer = HTTPFunctions.addOrReplaceHeaders(outBuffer, headers);
                        bytesRead = outBuffer.length;
                    }
                    if (traceHeaders) {
                        LOGGER.info("\n" + NetworkFunctions.toString(outBuffer, 0, bytesRead));
                    }
                }
                try {
                    remoteOut.write(outBuffer, 0, bytesRead);
                    remoteOut.flush();
                }
                catch (Exception e) {
                    LOGGER.error("Error in proxyRemoteOut {}", e.getMessage());
                    handler.cleanup();
                }
            }
        }
    }

    @Override public void remoteInProxyOut(IMIMPServerSocketHandler handler, DataInputStream remoteIn, DataOutputStream proxyOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            try {
                bytesRead = remoteIn.read(buffer);
            } catch (Exception e) {
                LOGGER.warn("Error in remoteIn {}", e.getMessage());
                bytesRead = -1;
            }
            if (bytesRead == -1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (handler.isNotConnectedOrOpen()) {
                    return;
                }
            } else {
                byte[] outBuffer = new byte[bytesRead];
                System.arraycopy(buffer, 0, outBuffer, 0, bytesRead);

                // check if the buffer contains an HTTP request
                String bufferString = NetworkFunctions.toString(outBuffer, 0, bytesRead);
                if (Protocol.HTTP_1_1.equals(NetworkFunctions.getProtocol(bufferString))) {
                    if (traceHeaders) {
                        String headString = HTTPFunctions.getHead(outBuffer);
                        LOGGER.info("\n" + headString + "\n");
                    }
                }
                try {
                    proxyOut.write(buffer, 0, bytesRead);
                    proxyOut.flush();
                }
                catch (Exception e) {
                    LOGGER.error("Error in remoteProxyOut {}", e.getMessage());
                    handler.cleanup();
                }
            }
        }
    }
}
