/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.properties;

import org.omnetpp.common.properties.Property;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.commands.CommandStack;

public class HistogramPlotProperties extends PlotProperties
{
    // private static final String DEFAULT_HIST_PROPERTIES_ID = "default";

    public static final String
        PROP_HIST_BAR           = "Hist.Bar",
        PROP_HIST_DATA          = "Hist.Data",
        PROP_SHOW_OVERFLOW_CELL = "Hist.ShowOverflowCell",
        PROP_BAR_BASELINE       = "Bars.Baseline";

    public enum HistogramBar {
        Solid,
        Outline,
    }

    public enum HistogramDataType {
        Count("count"),
        Pdf("probability density"),
        Cdf("cumulative density");

        private String displayName;

        private HistogramDataType(String displayName) {
            this.displayName = displayName;
        }

        @Override public String toString() {
            return displayName;
        }
    }

    public HistogramPlotProperties(Chart chart) {
        this(chart, null);
    }

    public HistogramPlotProperties(Chart chart, CommandStack commandStack) {
        super(chart, commandStack);
    }

    @Property(category="Plot",id=PROP_HIST_BAR,description="Histogram drawing method.")
    public HistogramBar getBarType() { return getEnumProperty(PROP_HIST_BAR, HistogramBar.class); }
    public void setBarType(HistogramBar placement) { setProperty(PROP_HIST_BAR, placement); }
    public HistogramBar defaultBarType() { return ChartDefaults.DEFAULT_HIST_BAR; }

    @Property(category="Plot",id=PROP_BAR_BASELINE,description="Baseline of the bars.")
    public Double getBarBaseline() { return getDoubleProperty(PROP_BAR_BASELINE); }
    public void setBarBaseline(Double baseline) { setProperty(PROP_BAR_BASELINE, baseline); }
    public Double defaultBarBaseline() { return ChartDefaults.DEFAULT_BAR_BASELINE; }

    @Property(category="Plot",id=PROP_HIST_DATA,description="Histogram data. Counts, probability density and cumulative density can be displayed.")
    public HistogramDataType getHistogramDataType() { return getEnumProperty(PROP_HIST_DATA, HistogramDataType.class); }
    public void setHistogramDataType(HistogramDataType data) { setProperty(PROP_HIST_DATA, data); }
    public HistogramDataType defaultHistogramData() { return ChartDefaults.DEFAULT_HIST_DATA; }

    @Property(category="Plot",id=PROP_SHOW_OVERFLOW_CELL,description="Show over/underflow cells.")
    public boolean getShowOverflowCell() { return getBooleanProperty(PROP_SHOW_OVERFLOW_CELL); }
    public void setShowOverflowCell(boolean value) { setProperty(PROP_SHOW_OVERFLOW_CELL, value); }
    public boolean defaultShowOverflowCell() { return ChartDefaults.DEFAULT_SHOW_OVERFLOW_CELL; }

//    /*======================================================================
//     *                             Histograms
//     *======================================================================*/
//    @Property(category="Plot",id="Histograms",displayName="Histograms",
//            description="Histogram plot properties.")
//    public IPropertySource getHistogramProperties()
//    {
//        IPropertyDescriptor[] descriptors = new IPropertyDescriptor[0];
//        return new BasePropertySource(descriptors) {
//            @Override public Object getPropertyValue(Object id) {
//                return new HistogramVisualProperties(HistogramChartVisualProperties.this,
//                        id == DEFAULT_HIST_PROPERTIES_ID ? null : (String)id);
//            }
//        };
//    }
}