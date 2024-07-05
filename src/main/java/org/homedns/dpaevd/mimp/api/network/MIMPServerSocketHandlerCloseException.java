/*
 * @(#)MIMPServerSocketHandlerCloseException.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.network;

/**
 * The exception thrown by the MIMP server socket handler if any side terminates the connection.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPServerSocketHandlerCloseException extends RuntimeException {

        /**
        * The constructor.
        * @param message The message of the exception.
        */
        public MIMPServerSocketHandlerCloseException(String message) {
            super(message);
        }
}
