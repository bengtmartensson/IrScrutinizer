package org.harctoolbox.guicomponents;

import java.beans.PropertyChangeListener;

/**
 *
 */
public interface ISendingReceivingBean {

    public static final String PROP_VERSION = "PROP_VERSION";
    public static final String PROP_BAUD = "PROP_BAUD";
    public static final String PROP_ISOPEN = "PROP_ISOPEN";
    public static final String PROP_PROPS = "PROP_PROPS";
    public static final String PROP_PORTNAME = "PROP_PORTNAME";

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

}
