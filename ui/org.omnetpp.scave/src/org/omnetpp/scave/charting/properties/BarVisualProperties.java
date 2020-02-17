/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.properties;

import org.omnetpp.common.properties.ColorPropertyDescriptor;
import org.omnetpp.common.properties.PropertySource;

public class BarVisualProperties extends PropertySource {

    public static final String
        PROP_BAR_COLOR          = "Bar.Color";

    private final PlotProperties chartProps;
    private String barId;

    public BarVisualProperties(PlotProperties chartProps, String barId) {
        this.chartProps = chartProps;
        this.barId = barId;
    }

    private String propertyName(String baseName) {
        return barId == null ? baseName : baseName + "/" + barId;
    }

    @org.omnetpp.common.properties.Property(category="Bars",id=PROP_BAR_COLOR,
            descriptorClass=ColorPropertyDescriptor.class,optional=true,
            description="Color of the bar. Color name or #RRGGBB. Press Ctrl+Space for a list of color names.")
    public String getColor() { return chartProps.getStringProperty(propertyName(PROP_BAR_COLOR)); } // FIXME use RGB
    public void setColor(String color) { chartProps.setProperty(propertyName(PROP_BAR_COLOR), color); }
    public String defaultColor() { return null; }
}