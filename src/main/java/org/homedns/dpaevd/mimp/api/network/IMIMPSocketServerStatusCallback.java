/*
 * @(#)MIMPServiceStatusCallback.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

/**
 * Returns the current service status.
 *
 * @author Daniele Denti (daniele.denti@bluewin.ch)
 * @version 2024.1
 * @since 2024.1
 */
public interface IMIMPSocketServerStatusCallback {

    /**
     * @param status The new status of the service
     */
    void onServiceStatusChange(MIMPSocketServerStatus status);

    /**
     * @param handler The handler to be notified if something changes.
     */
    void onHandler(IMIMPServerSocketHandler handler);
}
