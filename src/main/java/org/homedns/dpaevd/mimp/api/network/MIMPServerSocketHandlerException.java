/*
 * @(#)MIMPServerSocketHandlerException.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

/**
 * The exception thrown by the MIMP server socket handler.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPServerSocketHandlerException extends RuntimeException {

    /**
     * The constructor.
     * @param message The message of the exception.
     */
    public MIMPServerSocketHandlerException(String message) {
        super(message);
    }
}
