# Man in The Middle Proxy Server
## Description

The main goal is the development of an essential proxy server to integrate into the development environment, 
primarily for use in automated tests. 

The proxy must be completely transparent and, unless required, should not make any changes to the traffic.

The basic implementation is done using sockets and is limited to copying incoming data and sending it out unchanged. 
A different thread is used for each direction of the data flow, making the two directions completely independent.

The software is offered as a library to be integrated into test environments, with an added server based on Springboot 
that also demonstrates how to use the library.

In its initial version, two features are implemented. 
The first one allows serving multiple remote servers simultaneously through configuration.
The second feature enables the addition or modification of one or more headers in requests made using the HTTP/1.1 protocol.

These two features can be utilized by adjusting the application.properties file of the server included with the library.

    ...
    mimp.proxy.channels=8182:localhost:8181,8183:192.168.1.1:80
    mimp.proxy.headers=AnyHeader1:anyValue1:::AnyHeader2:anyValue2a,anyValue2b
    ...

**mimp.proxy.channels** allows defining a list of channels. Each individual channel consists of 3 parts: the entry port number, 
the remote server name, and the port number on the remote server. Channels are separated by commas.

**mimp.proxy.headers** allows defining headers to be added or replaced in each HTTP/1.1 request. 
Each header follows the usual format: name, semicolon, value1. In the case of headers with multiple values, 
the separator is a comma. Headers are separated using three semicolons.