/*
 * @(#)MIMPProperties.java 2024.1
 *
 * Copyright (c) 2024 by DPAEVD
 * All rights reserved
 */
package org.homedns.dpaevd.mimp.impl.config;

import org.homedns.dpaevd.mimp.api.config.IMIMPProperties;

import java.util.Properties;

/**
 * Properties of the MIMP.
 *
 * @author Daniele Denti <A HREF="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</A>
 * @version 2024.1
 * @since 2024.1
 */
public class MIMPProperties extends Properties implements IMIMPProperties {

    @Override public int getIntValue(final String key, final int defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(getProperty(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }
}
