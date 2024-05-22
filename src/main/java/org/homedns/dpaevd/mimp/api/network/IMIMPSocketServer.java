/*
 * @(#)IMimpSocketServer.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

/**
 * Description of the public method of the socket server.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface IMIMPSocketServer {

    /**
     * Releases all resources.
     */
    void cleanup();

    /**
     * @return the proxy IP address.
     */
    int getProxyIpPort();

    /**
     * @return the remote host name.
     */
    String getRemoteHostName();

    /**
     * @return the remote port.
     */
    int getRemoteIpPort();

    /**
     * Sets up the server.
     */
    void initialize();

    /**
     * @param callback Callback interface to receive status updates.
     */
    void setCallback(final IMIMPSocketServerStatusCallback callback);
}
