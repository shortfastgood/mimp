/*
 * @(#)ProblemDetails.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.server.web.rest;

/**
 * RFC 7807 Problem Details for REST APIs.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public record ProblemDetails(String detail, String instance, int status, String title, String type) {
    public ProblemDetails {
        if (detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("Detail must not be null or blank");
        }
        if (instance == null || instance.isBlank()) {
            throw new IllegalArgumentException("Instance must not be null or blank");
        }
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("Status must be between 100 and 599");
        }
        if (title == null || title.isBlank()) {
            title = "RFC 8707";
        }
        if (type == null || type.isBlank()) {
            type = "about:blank";
        }
    }
}
