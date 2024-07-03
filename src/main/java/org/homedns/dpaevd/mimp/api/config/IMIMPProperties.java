/*
 * @(#)IMIMPProperties.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.api.config;

/**
 * MIMP properties.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public interface IMIMPProperties {

    /**
     * Returns the value of the property with the specified key.
     *
     * @param key The key of the property.
     * @return The value of the property.
     */
    int getIntValue(final String key, final int defaultValue);

    /**
     * Returns the value of the property with the specified key.
     *
     * @param key The key of the property.
     * @return The value of the property.
     */
    String getProperty(String key, String defaultValue);

}
