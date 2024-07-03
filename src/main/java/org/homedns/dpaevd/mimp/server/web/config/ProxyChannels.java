/*
 * @(#)ProxyChannels.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.server.web.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.homedns.dpaevd.mimp.api.MIMPConstants;
import org.homedns.dpaevd.mimp.api.network.IMIMPSocketServer;
import org.homedns.dpaevd.mimp.impl.config.MIMPProperties;
import org.homedns.dpaevd.mimp.impl.network.MIMPIOCallback;
import org.homedns.dpaevd.mimp.impl.network.MIMPSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multiple proxy channels configuration.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
@Configuration
public class ProxyChannels {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyChannels.class);

    private final Environment environment;

    private final List<IMIMPSocketServer> proxyChannelList;

    public ProxyChannels(final Environment environment) {
        this.environment = environment;
        this.proxyChannelList = new ArrayList<>();
    }

    @PreDestroy
    public void cleanup() {
        proxyChannelList.forEach(IMIMPSocketServer::cleanup);
    }

    @PostConstruct
    public void initialize() {
        String proxyChannelCollection = environment.getProperty(MIMPConstants.PROXY_CHANNELS_KEY);
        if (proxyChannelCollection == null) {
            LOGGER.error("No proxy channels!");
            return;
        }
        String[] proxyChannels = proxyChannelCollection.split(",");
        Arrays.asList(proxyChannels).forEach(proxyChannel -> {
            String[] proxyChannelParts = proxyChannel.split(":");
            if (proxyChannelParts.length <3 || proxyChannelParts.length > 4) {
                LOGGER.error("Invalid proxy channel: " + proxyChannel);
                return;
            }
            int proxyIpPort = Integer.parseInt(proxyChannelParts[0]);
            String remoteHostName = proxyChannelParts[1];
            int remotePort = Integer.parseInt(proxyChannelParts[2]);

            boolean secure = proxyChannelParts.length == 4 && "secure".equalsIgnoreCase(proxyChannelParts[3]);

            MIMPProperties properties = new MIMPProperties();
            properties.put(MIMPConstants.LOCAL_SERVER_BUFFER_SIZE_KEY, 2048);
            properties.put(MIMPConstants.PROXY_HEADERS_KEY, environment.getProperty(MIMPConstants.PROXY_HEADERS_KEY, ""));
            properties.put(MIMPConstants.PROXY_TRACE_HEADERS_KEY, environment.getProperty(MIMPConstants.PROXY_TRACE_HEADERS_KEY, "false"));

            MIMPIOCallback iOCallback = new MIMPIOCallback();

            MIMPSocketServer proxy = new MIMPSocketServer(iOCallback, properties, proxyIpPort, remoteHostName, remotePort);
            proxy.initialize();

            proxyChannelList.add(proxy);
        });
    }
}
