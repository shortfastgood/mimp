/*
 * @(#)MIMPSocketServer.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import org.homedns.dpaevd.mimp.api.MIMPConstants;
import org.homedns.dpaevd.mimp.api.config.IMIMPProperties;
import org.homedns.dpaevd.mimp.api.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The MIMP socket server.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPSocketServer implements IMIMPSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIMPSocketServer.class);

    private IMIMPSocketServerStatusCallback callback;

    private ExecutorService clientExecutorService;

    private final int proxyIpPort;

    private final IMIMPIOCallback iOCallback;

    private final IMIMPProperties properties;

    private final String remoteHostName;

    private final int remoteIpPort;

    private ExecutorService serverExecutorService;

    private final List<IMIMPServerSocketHandler> serverSocketHandlers;

    private ServerSocket serverSocket;

    private volatile MIMPSocketServerStatus serviceStatus;

    /**
     *
     * @param iOCallback Callback interface to handle the data exchange.
     * @param properties Properties.
     * @param proxyIpPort Proxy port.
     * @param remoteHostName Remote host name.
     * @param remoteIpPort Remote port.
     */
    public MIMPSocketServer(
            final IMIMPIOCallback iOCallback,
            final IMIMPProperties properties,
            final int proxyIpPort,
            final String remoteHostName,
            final int remoteIpPort) {
        this.iOCallback = iOCallback;
        this.properties = properties;
        this.proxyIpPort = proxyIpPort;
        this.remoteHostName = remoteHostName;
        this.remoteIpPort = remoteIpPort;
        this.serverSocketHandlers = new CopyOnWriteArrayList<>();
    }

    public void cleanup() {
        LOGGER.info("Cleaning up MIMP proxy server on port {} -> {}:{}", proxyIpPort, remoteHostName, remoteIpPort);
        serverSocketHandlers.forEach(IMIMPServerSocketHandler::cleanup);
        if (serverExecutorService != null && !serverExecutorService.isShutdown()) {
            serverExecutorService.shutdownNow();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ioe) {
                LOGGER.error("Cannot close the server channel on port {}. Reason: {}", proxyIpPort, ioe.getMessage());
            }
            LOGGER.info("Closed the server channel on port {}", proxyIpPort);
        }
    }

    @Override
    public int getProxyIpPort() {
        return proxyIpPort;
    }

    @Override
    public String getRemoteHostName() {
        return remoteHostName;
    }

    @Override
    public int getRemoteIpPort() {
        return remoteIpPort;
    }

    public void initialize() {
        LOGGER.info("Initializing MIMP proxy server on port {} -> {}:{}", proxyIpPort, remoteHostName, remoteIpPort);
        final int byteBufferSize = properties.getIntValue(MIMPConstants.LOCAL_SERVER_BUFFER_SIZE_KEY, 2048);
        final InetSocketAddress serverAddress = new InetSocketAddress(proxyIpPort);
        clientExecutorService = Executors.newWorkStealingPool(8);
        serverExecutorService = Executors.newSingleThreadExecutor();
        serverExecutorService.submit(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.bind(serverAddress);
                serviceStatus = MIMPSocketServerStatus.UP;
                if (callback != null) {
                    callback.onServiceStatusChange(serviceStatus);
                }
            } catch (IOException ioe) {
                LOGGER.error("Cannot open the server channel on port {}. Reason: {}", proxyIpPort, ioe.getMessage());
                serviceStatus = MIMPSocketServerStatus.ERROR;
                if (callback != null) {
                    callback.onServiceStatusChange(serviceStatus);
                }
            }
            Timer watchdogTimer = new Timer();
            try {
                watchdogTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override public void run() {
                        if (MIMPSocketServerStatus.UP.equals(serviceStatus) || MIMPSocketServerStatus.CONNECTED.equals(serviceStatus)) {
                            if (serverSocketHandlers.stream().anyMatch(h -> MIMPSocketHandlerStatus.CONNECTED.equals(h.getStatus()))) {
                                serviceStatus = MIMPSocketServerStatus.CONNECTED;
                                if (callback != null) {
                                    callback.onServiceStatusChange(serviceStatus);
                                }
                            } else {
                                serviceStatus = MIMPSocketServerStatus.UP;
                                if (callback != null) {
                                    callback.onServiceStatusChange(serviceStatus);
                                }
                            }
                        }
                    }
                }, 0, 5000);

                while (!serverSocket.isClosed()) {
                    Socket proxySocket = serverSocket.accept();

                    clientExecutorService.execute(() -> {
                        LOGGER.info("Accepted connection from {}", proxySocket.getRemoteSocketAddress());

                        try {
                            proxySocket.setReceiveBufferSize(byteBufferSize);
                            proxySocket.setSendBufferSize(byteBufferSize);
                            proxySocket.setKeepAlive(true);
                            proxySocket.setSoLinger(true, 0);

                            Socket remoteSocket = new Socket();
                            remoteSocket.connect(new InetSocketAddress(remoteHostName, remoteIpPort));
                            remoteSocket.setReceiveBufferSize(byteBufferSize);
                            remoteSocket.setSendBufferSize(byteBufferSize);
                            remoteSocket.setSoLinger(true, 0);
                            remoteSocket.setKeepAlive(true);

                            IMIMPServerSocketHandler handler = new MIMPServerSocketHandler(iOCallback, properties, proxySocket, remoteSocket);
                            serverSocketHandlers.add(handler);
                            handler.execute();

                        } catch (IOException ioe) {
                            LOGGER.error("Cannot establish connection with remote {}. Reason: {}", remoteHostName, ioe.getMessage());
                        }

                    });

                    List<IMIMPServerSocketHandler> zombies = serverSocketHandlers.stream().filter(IMIMPServerSocketHandler::isNotConnectedOrOpen).toList();
                    zombies.forEach(IMIMPServerSocketHandler::cleanup);
                    zombies.forEach(serverSocketHandlers::remove);
                }

            } catch (IOException ioe) {
                LOGGER.info("Server channel terminated: {}", ioe.getMessage());
                serviceStatus = MIMPSocketServerStatus.DOWN;
                if (callback != null) {
                    callback.onServiceStatusChange(serviceStatus);
                }
            } finally {
                watchdogTimer.cancel();
                cleanup();
            }
        });
    }

    /**
     * @param callback Registers or replaces a callback handler to get the status changes.
     */
    public void setCallback(final IMIMPSocketServerStatusCallback callback) {
        this.callback = callback;
    }

}
