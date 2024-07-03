/*
 * @(#)HTTPFunctionsTest.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test suite for HTTP functions.
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class HTTPFunctionsTest {

    @Test
    void createRequestTest() {
        String buffer = """
                GET / HTTP/1.1\r
                Host: localhost:8182\r
                Connection: keep-alive\r
                Cache-Control: max-age=0\r
                sec-ch-ua: "Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"\r
                sec-ch-ua-mobile: ?0\r
                sec-ch-ua-platform: "macOS"\r
                DNT: 1\r
                Upgrade-Insecure-Requests: 1\r
                User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome//124.0.0.0 Safari/537.36\r
                Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\r
                Sec-Fetch-Site: none\r
                Sec-Fetch-Mode: navigate\r
                Sec-Fetch-User: ?1\r
                Sec-Fetch-Dest: document\r
                Accept-Encoding: gzip, deflate, br, zstd\r
                Accept-Language: it,it-IT;q=0.9,de;q=0.8,en;q=0.7,en-US;q=0.6\r
                If-Modified-Since: Sun, 19 May 2024 17:07:52 GMT\r
                \r
                """;

        HTTPRequest request = HTTPFunctions.createRequest(buffer);
        assertNotNull(request);
        assertEquals(HTTPMethod.GET, request.method());
        assertEquals("/", request.requestURI());
        assertEquals(17, request.headers().size());
    }

    @Test
    void createResponseTest() {
        String buffer = """
                HTTP/1.1 200 OK\r
                Date: Sun, 19 May 2024 17:07:52 GMT\r
                Server: Apache/2.4.41 (Unix) OpenSSL/1.1.1d PHP/7.4.3 mod_perl/2.0.8-dev Perl/v5.16.3\r
                Last-Modified: Sun, 19 May 2024 17:07:52 GMT\r
                ETag: "2c-5c6e1f1b7a1e4"\r
                Accept-Ranges: bytes\r
                Content-Length: 44\r
                Keep-Alive: timeout=5, max=100\r
                Connection: Keep-Alive\r
                Content-Type: text/html\r
                \r
                <html><body><h1>It works!</h1></body></html>\r
                """;

        HTTPResponse response = HTTPFunctions.createResponse(buffer);
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.reasonPhrase());
        assertEquals(9, response.headers().size());
        assertEquals("<html><body><h1>It works!</h1></body></html>", new String(HTTPFunctions.getBody(buffer.getBytes())).trim());
    }
}
