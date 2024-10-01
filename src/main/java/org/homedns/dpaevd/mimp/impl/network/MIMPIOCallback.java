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
import org.homedns.dpaevd.mimp.api.network.MIMPServerSocketHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public byte[] in(final DataInputStream in) {

        byte[] buffer = new byte[MIMPConstants.BUFFER_SIZE];
        int bytesRead;
        try {
            bytesRead = in.read(buffer);
        } catch (Exception e) {
            throw new MIMPServerSocketHandlerException("Error reading data from the input stream: " + e.getMessage());
        }
        if (bytesRead == -1) {
            // the connection has to be considered as stale. Both channels have to be closed.
            throw new MIMPServerSocketHandlerException("Connection closed by the client/remote");
        } else if (bytesRead > 0) {
            byte[] inBuffer = new byte[bytesRead];
            System.arraycopy(buffer, 0, inBuffer, 0, bytesRead);
            return inBuffer;
        } else {
            return new byte[0];
        }
    }

    @Override
    public byte[] inAndWait(final DataInputStream in) {

        // wait for data to be available
        try {
            while (in.available() == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } catch (IOException ioe) {
            throw new MIMPServerSocketHandlerException("Error waiting data from the input stream: " + ioe.getMessage());
        }

        return in(in);

    }

    @Override
    public void out(final DataOutputStream out, byte[] buffer) {
        try {
            out.write(buffer);
            out.flush();
        } catch (IOException ioe) {
            throw new MIMPServerSocketHandlerException("Error writing data to the output stream: " + ioe.getMessage());
        }
    }

    @Override public void proxyInRemoteOut(IMIMPServerSocketHandler handler, DataInputStream proxyIn, DataOutputStream remoteOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = in(proxyIn);
            if (buffer.length > 0) {
                out(remoteOut, buffer);
            }
        }
    }

    @Override public void remoteInProxyOut(IMIMPServerSocketHandler handler, DataInputStream remoteIn, DataOutputStream proxyOut) {
        while(!handler.isNotConnectedOrOpen()) {
            byte[] buffer = in(remoteIn);
            if (buffer.length > 0) {
                out(proxyOut, buffer);
            }
        }
    }

    @Override public void wsProxyInRemoteOut(IMIMPServerSocketHandler handler, DataInputStream proxyIn, DataOutputStream remoteOut, AtomicBoolean isWebSocketClosed) {
    }

    @Override public void wsRemoteInProxyOut(IMIMPServerSocketHandler handler, DataInputStream remoteIn, DataOutputStream proxyOut, AtomicBoolean isWebSocketClosed) {
    }
}
