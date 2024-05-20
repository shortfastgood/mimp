/*
 * @(#)NetworkFunctions.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.network;

import org.homedns.dpaevd.mimp.api.util.Alphabet;
import org.homedns.dpaevd.mimp.impl.http.HTTPMethod;

/**
 * Network functions.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class NetworkFunctions {

    static Protocol getProtocol(final String buffer) {
        if ((buffer.startsWith(HTTPMethod.GET.name())
                || buffer.startsWith(HTTPMethod.HEAD.name())
                || buffer.startsWith(HTTPMethod.POST.name())
                || buffer.startsWith(HTTPMethod.PUT.name())
                || buffer.startsWith(HTTPMethod.DELETE.name())
                || buffer.startsWith(HTTPMethod.TRACE.name())
                || buffer.startsWith(HTTPMethod.CONNECT.name())) && buffer.contains("HTTP/1.1")) {
            return Protocol.HTTP_1_1;
        }
        return Protocol.UNKNOWN;
    }

    static String toString( byte[] bytes, int offset, int length ) {
        if (bytes == null) {
            return "null";
        }
        if (length <= 0 || offset < 0 || offset >= bytes.length || length > bytes.length) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        for( int i = offset; i < length - offset; i++ ) {
            byte b = bytes[i];
            if (Alphabet.ASCII_PRINTABLE.contains((char)b) || Alphabet.WHITE_CHARS.contains((char)b)) {
                buf.append((char) b);
            } else {
                buf.append(String.format("\\x%02X", b));
            }
        }
        return buf.toString();
    }
}
