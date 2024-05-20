/*
 * @(#)WebSocketController.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.server.web.rest;

import org.springframework.core.env.Environment;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.client.ResourceAccessException;

import java.net.InetAddress;
import java.net.URI;

/**
 * WEB socket controller.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class WebSocketController {

    private final Environment environment;

    private URI instance;

    public WebSocketController(final Environment environment) {
        this.environment = environment;
    }

    @MessageExceptionHandler
    @SendTo("/topic/error")
    public ProblemDetails handleException(final Exception e) {
        if (e instanceof ResourceAccessException rae) {
            return new ProblemDetails(rae.getMessage(), instance.toString(), 500, "Resource access error", "about:blank");
        } else {
            return new ProblemDetails(e.getCause() != null ?  e.getCause().getMessage() : e.getMessage(), instance.toString(), 500, "Internal server error", "about:blank");
        }
    }

    public void initialize() {
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "localhost";
        }
        String hostPort = environment.getProperty("server.port", "8181");
        instance = URI.create("ws://" + hostName + ":" + hostPort + "/mimp-ws");
    }
}
