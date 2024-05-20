/*
 * @(#)MIMPServerSocketHandler.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import org.homedns.dpaevd.mimp.api.network.IMIMPIOCallback;
import org.homedns.dpaevd.mimp.api.network.IMIMPServerSocketHandler;
import org.homedns.dpaevd.mimp.api.network.MIMPSocketHandlerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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

    private final IMIMPIOCallback iOCallback;

    private final Socket proxySocket;

    private DataInputStream proxySocketIn;

    private DataOutputStream proxySocketOut;

    private final Socket remoteSocket;

    private DataInputStream remoteSocketIn;

    private DataOutputStream remoteSocketOut;

    private volatile MIMPSocketHandlerStatus status;

    private ExecutorService workerExecutor;

    public MIMPServerSocketHandler(final IMIMPIOCallback iOCallback, final Socket proxySocket, final Socket remoteSocket) {
        this.iOCallback = iOCallback;
        this.proxySocket = proxySocket;
        this.remoteSocket = remoteSocket;
        this.status = MIMPSocketHandlerStatus.CONNECTED;
    }

    @Override public void cleanup() {
        status = MIMPSocketHandlerStatus.DISCONNECTED;
        if (workerExecutor != null) {
            workerExecutor.shutdownNow();
        }
        if (proxySocketIn != null) {
            try {
                proxySocketIn.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket input stream! Cause: {}", ioe.getMessage());
            }
            proxySocketIn = null;
        }
        if (proxySocketOut != null) {
            try {
                proxySocketOut.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket output stream! Cause: {}", ioe.getMessage());
            }
            proxySocketOut = null;
        }
        if (proxySocket != null && !proxySocket.isClosed()) {
            try {
                proxySocket.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close proxy socket! Cause: {}", ioe.getMessage());
            }
        }
        if (remoteSocketIn != null) {
            try {
                remoteSocketIn.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket input stream! Cause: {}", ioe.getMessage());
            }
            remoteSocketIn = null;
        }
        if (remoteSocketOut != null) {
            try {
                remoteSocketOut.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket output stream! Cause: {}", ioe.getMessage());
            }
            remoteSocketOut = null;
        }
        if (remoteSocket != null && !remoteSocket.isClosed()) {
            try {
                remoteSocket.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close remote socket! Cause: {}", ioe.getMessage());
            }
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
        workerExecutor = Executors.newFixedThreadPool(2);
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

        workerExecutor.submit(() -> iOCallback.proxyInRemoteOut(this, proxySocketIn, remoteSocketOut));
        workerExecutor.submit(() -> iOCallback.remoteInProxyOut(this, remoteSocketIn, proxySocketOut));
    }
}
