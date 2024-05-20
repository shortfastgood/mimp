/*
 * @(#)HTTPHeader.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.http;

import java.util.List;

/**
 * Simplified HTTP header for proxy purposes.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public record HTTPHeader(String name, List<String> values) {
    public HTTPHeader {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid header name");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Invalid header values");
        }
    }
}
