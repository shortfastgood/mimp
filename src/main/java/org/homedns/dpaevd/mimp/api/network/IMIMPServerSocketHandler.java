/*
 * @(#)MIMPServerSocketHandler.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

import java.net.Socket;

/**
 * Definition of a server socket handler.
 *
 * @author Daniele Denti (daniele.denti@bluewin.ch)
 * @version 2024.1
 * @since 2024.1
 */
public interface IMIMPServerSocketHandler {

    /**
     * Releases all resources.
     */
    void cleanup();

    /**
     * Performs the data exchange.
     */
    void execute();

    /**
     * @return the proxy socket.
     */
    Socket getProxySocket();

    /**
     * @return the remote socket.
     */
    Socket getRemoteSocket();

    /**
     * @return the actual status of the connection.
     */
    MIMPSocketHandlerStatus getStatus();

    /**
     * @return true if the connection is not connected or open.
     */
    boolean isNotConnectedOrOpen();
}
