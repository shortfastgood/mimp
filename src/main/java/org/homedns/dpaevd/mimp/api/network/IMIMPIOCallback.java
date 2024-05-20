/*
 * @(#)IMIMPMessageIOCallback.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Returns the incoming messages.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface IMIMPIOCallback {

    /**
     * Handles the data exchange between the proxy and the remote.
     *
     * @param handler The handler of the socket.
     * @param proxyIn The input stream of the proxy.
     * @param remoteOut The output stream of the remote.
     */
    void proxyInRemoteOut(IMIMPServerSocketHandler handler, DataInputStream proxyIn, DataOutputStream remoteOut);

    /**
     * Handles the data exchange between the remote and the proxy.
     *
     * @param handler The handler of the socket.
     * @param remoteIn The input stream of the remote.
     * @param proxyOut The output stream of the proxy.
     */
    void remoteInProxyOut(IMIMPServerSocketHandler handler, DataInputStream remoteIn, DataOutputStream proxyOut);
}
