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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Handles the data exchange between the proxy and the remote.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPIOCallback implements IMIMPIOCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIMPIOCallback.class);

    @Override
    public byte[] in(IMIMPServerSocketHandler handler, DataInputStream proxyIn) {
        byte[] buffer = new byte[MIMPConstants.BUFFER_SIZE];
        int bytesRead;
        try {
            bytesRead = proxyIn.read(buffer);
        } catch (Exception e) {
            LOGGER.warn("Error reading data from proxy client {}", e.getMessage());
            handler.cleanup();
            return new byte[0];
        }
        if (bytesRead == -1) {
            // the connection has to be considered as stale. Both channels have to be closed.
            LOGGER.warn("Connection closed by the client");
            handler.cleanup();
            return new byte[0];
        } else if (bytesRead > 0) {
            byte[] inBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, inBuffer, 0, bytesRead);
            return inBuffer;
        } else {
            return new byte[0];
        }
    }

    @Override
    public byte[] remoteIn(IMIMPServerSocketHandler handler, DataInputStream remoteIn) {
        byte[] buffer = new byte[MIMPConstants.BUFFER_SIZE];
        int bytesRead;
        try {
            bytesRead = remoteIn.read(buffer);
        } catch (Exception e) {
            LOGGER.warn("Error reading data from remote {}", e.getMessage());
            handler.cleanup();
            return new byte[0];
        }
        if (bytesRead == -1) {
            // the connection has to be considered as stale. Both channels have to be closed.
            LOGGER.warn("Connection closed by the remote");
            handler.cleanup();
            return new byte[0];
        } else if (bytesRead > 0) {
            byte[] inBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, inBuffer, 0, bytesRead);
            return inBuffer;
        } else {
            return new byte[0];
        }
    }

    @Override
    public void out(IMIMPServerSocketHandler handler, DataOutputStream out, byte[] buffer) {
        try {
            out.write(buffer);
            out.flush();
        } catch (Exception e) {
            LOGGER.error("Error writing data {}", e.getMessage());
            handler.cleanup();
        }
    }

    @Override public void proxyInRemoteOut(IMIMPServerSocketHandler handler, DataInputStream proxyIn, DataOutputStream remoteOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = in(handler, proxyIn);
            if (buffer.length > 0) {
                out(handler, remoteOut, buffer);
            }
        }
    }

    @Override public void remoteInProxyOut(IMIMPServerSocketHandler handler, DataInputStream remoteIn, DataOutputStream proxyOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = in(handler, remoteIn);
            if (buffer.length > 0) {
                out(handler, proxyOut, buffer);
            }
        }
    }
}
