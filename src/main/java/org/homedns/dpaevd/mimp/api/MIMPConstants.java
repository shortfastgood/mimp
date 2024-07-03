/*
 * @(#)MIMPConstants.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api;

/**
 * Constants for the MIMP API.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface MIMPConstants {

     int BUFFER_SIZE = 1024;

     String LOCAL_SERVER_BUFFER_SIZE_KEY = "mimp.proxy.buffer.size";

     String PROXY_CHANNELS_KEY = "mimp.proxy.channels";

     String PROXY_HEADERS_KEY = "mimp.proxy.headers";

     String PROXY_TRACE_HEADERS_KEY = "mimp.proxy.trace.headers";
}
