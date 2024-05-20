/*
 * @(#)MIMPWebServer.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.server.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MIMP WEB server for testing purposes.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
@SpringBootApplication
public class MIMPWebServer {
    public static void main(String[] args) {
        SpringApplication.run(MIMPWebServer.class, args);
    }
}
