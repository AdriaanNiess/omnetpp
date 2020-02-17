/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.properties.ColorPropertyDescriptor;
import org.omnetpp.common.properties.PropertySource;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model.Property;
import org.omnetpp.scave.model.commands.CommandStack;
import org.omnetpp.scave.model.commands.SetChartPropertyCommand;

/**
 * Property source for charts.
 * @author tomi
 */
public class PlotProperties extends PropertySource {

    /**
     * Property names used in the model.
     */
    public static final String
        // Titles
        PROP_PLOT_TITLE         = "Plot.Title",
        PROP_PLOT_TITLE_FONT    = "Plot.Title.Font",
        PROP_X_AXIS_TITLE       = "X.Axis.Title",
        PROP_Y_AXIS_TITLE       = "Y.Axis.Title",
        PROP_AXIS_TITLE_FONT    = "Axis.Title.Font",
        PROP_LABEL_FONT         = "Label.Font",
        PROP_X_LABELS_ROTATE_BY = "X.Label.RotateBy",
        // Axes
        PROP_Y_AXIS_MIN         = "Y.Axis.Min",
        PROP_Y_AXIS_MAX         = "Y.Axis.Max",
        PROP_Y_AXIS_LOGARITHMIC = "Y.Axis.Log",
        PROP_XY_GRID            = "Axes.Grid",
        // Legend
        PROP_DISPLAY_LEGEND     = "Legend.Display",
        PROP_LEGEND_BORDER      = "Legend.Border",
        PROP_LEGEND_FONT        = "Legend.Font",
        PROP_LEGEND_POSITION    = "Legend.Position",
        PROP_LEGEND_ANCHORING   = "Legend.Anchoring",
        // Plot
        PROP_ANTIALIAS          = "Plot.Antialias",
        PROP_CACHING            = "Plot.Caching",
        PROP_BACKGROUND_COLOR   = "Plot.BackgroundColor";

    public enum LegendPosition {
        Inside,
        Above,
        Below,
        Left,
        Right,
    }

    public enum LegendAnchor {
        North,
        NorthEast,
        East,
        SouthEast,
        South,
        SouthWest,
        West,
        NorthWest,
    }

    public enum ShowGrid {
        None,
        Major,
        All,
    }

    public static IPropertySource2 createPropertySource(Chart chart) {
        switch (chart.getType()) {
        case BAR:
            return new BarChartVisualProperties(chart);
        case HISTOGRAM:
            return new HistogramChartVisualProperties(chart);
        case LINE:
            return new LineChartVisualProperties(chart);
        case MATPLOTLIB:
            return new MatplotlibChartProperties(chart);
        default:
            return null;
        }
    }

    protected Chart chart;               // the chart what the properties belongs to
    protected CommandStack commandStack; // result file manager to access chart content (for line properties)

    public ChartVisualProperties(Chart chart) {
        this(chart, null);
    }

    /*======================================================================
     *                             Main
     *======================================================================*/
    @org.omnetpp.common.properties.Property(category="Main",id=PROP_ANTIALIAS,displayName="antialias",
            description="Enables antialising.")
    public boolean getAntialias() { return getBooleanProperty(PROP_ANTIALIAS); }
    public void setAntialias(boolean flag) { setProperty(PROP_ANTIALIAS, flag); }
    public boolean defaultAntialias() { return PlotDefaults.DEFAULT_ANTIALIAS; }

    @org.omnetpp.common.properties.Property(category="Main",id=PROP_CACHING,displayName="caching",
            description="Enables caching. Caching makes scrolling faster, but sometimes the plot might not be correct.")
    public boolean getCaching() { return getBooleanProperty(PROP_CACHING); }
    public void setCaching(boolean flag) { setProperty(PROP_CACHING, flag); }
    public boolean defaultCaching() { return PlotDefaults.DEFAULT_CANVAS_CACHING; }

    @org.omnetpp.common.properties.Property(category="Main",id=PROP_BACKGROUND_COLOR,displayName="background color",
            descriptorClass=ColorPropertyDescriptor.class, description="Background color of the chart.")
    public String getBackgroundColor() { return getStringProperty(PROP_BACKGROUND_COLOR); }
    public void setBackgroundColor(String rgb) { setProperty(PROP_BACKGROUND_COLOR, rgb); }
    public String defaultBackgroundColor() { return ColorFactory.asString(PlotDefaults.DEFAULT_BACKGROUND_COLOR.getRGB()); }

    /*======================================================================
     *                             Titles
     *======================================================================*/
    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_PLOT_TITLE,
            descriptorClass=TitlePatternPropertyDescriptor.class, description="Main title of the chart.")
    public String getGraphTitle() { return getStringProperty(PROP_PLOT_TITLE); }
    public void setGraphTitle(String title) { setProperty(PROP_PLOT_TITLE, title); }
    public String defaultGraphTitle() { return PlotDefaults.DEFAULT_TITLE; }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_PLOT_TITLE_FONT,
            description="Font used to draw the title.")
    public FontData getGraphTitleFont() { return getFontProperty(PROP_PLOT_TITLE_FONT); }
    public void setGraphTitleFont(FontData font) { setProperty(PROP_PLOT_TITLE_FONT, font); }
    public FontData getDefaultTitleFont() { return getDefaultFontProperty(PROP_PLOT_TITLE_FONT); }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_X_AXIS_TITLE,
            description="Title of the horizontal axis.")
    public String getXAxisTitle() { return getStringProperty(PROP_X_AXIS_TITLE); }
    public void setXAxisTitle(String title) { setProperty(PROP_X_AXIS_TITLE, title); }
    public String defaultXAxisTitle() { return PlotDefaults.DEFAULT_X_AXIS_TITLE; }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_Y_AXIS_TITLE,
            description="Title of the vertical axis.")
    public String getYAxisTitle() { return getStringProperty(PROP_Y_AXIS_TITLE); }
    public void setYAxisTitle(String title) { setProperty(PROP_Y_AXIS_TITLE, title); }
    public String defaultYAxisTitle() { return PlotDefaults.DEFAULT_Y_AXIS_TITLE; }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_AXIS_TITLE_FONT,
            description="Font used to draw the axes titles.")
    public FontData getAxisTitleFont() { return getFontProperty(PROP_AXIS_TITLE_FONT); }
    public void setAxisTitleFont(FontData font) { setProperty(PROP_AXIS_TITLE_FONT, font); }
    public FontData defaultAxisTitleFont() { return getDefaultFontProperty(PROP_AXIS_TITLE_FONT); }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_LABEL_FONT,
            description="Font used to draw the tick labels.")
    public FontData getLabelsFont() { return getFontProperty(PROP_LABEL_FONT); }
    public void setLabelsFont(FontData font) { setProperty(PROP_LABEL_FONT, font); }
    public FontData defaultLabelsFont() { return getDefaultFontProperty(PROP_LABEL_FONT); }

    @org.omnetpp.common.properties.Property(category="Titles",id=PROP_X_LABELS_ROTATE_BY,displayName="x labels rotated by",
            description="Rotates the tick labels of the horizontal axis by the given angle (in degrees).")
    public Double getXLabelsRotate() { return getDoubleProperty(PROP_X_LABELS_ROTATE_BY); }
    public void setXLabelsRotate(Double value) { setProperty(PROP_X_LABELS_ROTATE_BY, value); }
    public Double defaultXLabelsRotate() { return PlotDefaults.DEFAULT_X_LABELS_ROTATED_BY; }

    /*======================================================================
     *                             Axes
     *======================================================================*/
    @org.omnetpp.common.properties.Property(category="Axes",id=PROP_Y_AXIS_MIN,
            description="Crops the input below this y value.")
    public Double getYAxisMin() { return getDoubleProperty(PROP_Y_AXIS_MIN); }
    public void setYAxisMin(Double min) { setProperty(PROP_Y_AXIS_MIN, min); }

    @org.omnetpp.common.properties.Property(category="Axes",id=PROP_Y_AXIS_MAX,
            description="Crops the input above this y value.")
    public Double getYAxisMax() { return getDoubleProperty(PROP_Y_AXIS_MAX); }
    public void setYAxisMax(Double max) { setProperty(PROP_Y_AXIS_MAX, max); }

    @org.omnetpp.common.properties.Property(category="Axes",id=PROP_Y_AXIS_LOGARITHMIC,
            description="Applies a logarithmic transformation to the y values.")
    public boolean getYAxisLogarithmic() { return getBooleanProperty(PROP_Y_AXIS_LOGARITHMIC); }
    public void setYAxisLogarithmic(boolean flag) { setProperty(PROP_Y_AXIS_LOGARITHMIC, flag); }
    public boolean defaultYAxisLogarithmic() { return PlotDefaults.DEFAULT_Y_AXIS_LOGARITHMIC; }

    @org.omnetpp.common.properties.Property(category="Axes",id=PROP_XY_GRID,displayName="grid",
            description="Add grid lines to the plot.")
    public ShowGrid getXYGrid() { return getEnumProperty(PROP_XY_GRID, ShowGrid.class); }
    public void setXYGrid(ShowGrid showgrid) { setProperty(PROP_XY_GRID, showgrid); }
    public ShowGrid defaultXYGrid() { return PlotDefaults.DEFAULT_SHOW_GRID; }

    /*======================================================================
     *                             Legend
     *======================================================================*/
    @org.omnetpp.common.properties.Property(category="Legend", id=PROP_DISPLAY_LEGEND,
            displayName="display", description="Displays the legend.")
    public boolean getDisplayLegend() { return getBooleanProperty(PROP_DISPLAY_LEGEND); }
    public void setDisplayLegend(boolean flag) { setProperty(PROP_DISPLAY_LEGEND, flag); }
    public boolean defaultDisplayLegend() { return PlotDefaults.DEFAULT_DISPLAY_LEGEND; }

    @org.omnetpp.common.properties.Property(category="Legend", id=PROP_LEGEND_BORDER,
            displayName="border", description="Add border around the legend.")
    public boolean getLegendBorder() { return getBooleanProperty(PROP_LEGEND_BORDER); }
    public void setLegendBorder(boolean flag) { setProperty(PROP_LEGEND_BORDER, flag); }
    public boolean defaultLegendBorder() { return PlotDefaults.DEFAULT_LEGEND_BORDER; }

    @org.omnetpp.common.properties.Property(category="Legend", id=PROP_LEGEND_FONT,
            displayName="font", description="Font used to draw the legend items.")
    public FontData getLegendFont() { return getFontProperty(PROP_LEGEND_FONT); }
    public void setLegendFont(FontData font) { setProperty(PROP_LEGEND_FONT, font); }
    public FontData defaultLegendFont() { return getDefaultFontProperty(PROP_LEGEND_FONT); }

    @org.omnetpp.common.properties.Property(category="Legend", id=PROP_LEGEND_POSITION,
            displayName="position", description="Position of the legend.")
    public LegendPosition getLegendPosition() { return getEnumProperty(PROP_LEGEND_POSITION, LegendPosition.class); }
    public void setLegendPosition(LegendPosition position) { setProperty(PROP_LEGEND_POSITION, position); }
    public LegendPosition defaultLegendPosition() { return PlotDefaults.DEFAULT_LEGEND_POSITION; }

    @org.omnetpp.common.properties.Property(category="Legend",id=PROP_LEGEND_ANCHORING,
            displayName="anchor point", description="Anchor point of the legend.")
    public LegendAnchor getLegendAnchoring() { return getEnumProperty(PROP_LEGEND_ANCHORING, LegendAnchor.class); }
    public void setLegendAnchoring(LegendAnchor anchoring) { setProperty(PROP_LEGEND_ANCHORING, anchoring); }
    public LegendAnchor defaultLegendAnchor() { return PlotDefaults.DEFAULT_LEGEND_ANCHOR; }

    /*---------------------------------------------------------------
     *                   Helpers
     *---------------------------------------------------------------*/

    public Property getProperty(String propertyName) {
        return chart.lookupProperty(propertyName);
    }

    public String getStringProperty(String propertyName) {
        Property property = getProperty(propertyName);
        return property != null ? StringUtils.defaultString(property.getValue()) :
                                  getDefaultStringProperty(propertyName);
    }

    public Boolean getBooleanProperty(String propertyName) {
        Property property = getProperty(propertyName);
        return property != null ? Boolean.valueOf(property.getValue()) :
                                  getDefaultBooleanProperty(propertyName);
    }

    public <T extends Enum<T>> T getEnumProperty(String propertyName, Class<T> type) {
        Property property = getProperty(propertyName);
        return property != null && property.getValue() != null ? Converter.stringToEnum(property.getValue(), type) :
                                                                 getDefaultEnumProperty(propertyName, type);
    }

    public FontData getFontProperty(String propertyName) {
        Property property = getProperty(propertyName);
        return property != null ? Converter.stringToFontdata(property.getValue()) :
                                  getDefaultFontProperty(propertyName);
    }

    public RGB getColorProperty(String propertyName) {
        Property property = getProperty(propertyName);
        return property != null ? Converter.stringToRGB(property.getValue()) :
                                  getDefaultColorProperty(propertyName);
    }

    public Integer getIntegerProperty(String propertyName, boolean useDefault) {
        Property property = getProperty(propertyName);
        return property != null ? Converter.stringToInteger(property.getValue()) :
               useDefault       ? getDefaultIntegerProperty(propertyName) :
                                  null;
    }

    public Double getDoubleProperty(String propertyName) {
        Property property = getProperty(propertyName);
        return property != null ? Converter.stringToDouble(property.getValue()) :
                                    getDefaultDoubleProperty(propertyName);
    }

    protected void doSetProperty(String propertyName, String propertyValue) {
        commandStack.execute(new SetChartPropertyCommand(chart, propertyName, propertyValue));
    }

    public void setProperty(String propertyName, String propertyValue) {
        String defaultValue = getDefaultStringProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, propertyValue);
    }

    public void setProperty(String propertyName, Boolean propertyValue) {
        Boolean defaultValue = getDefaultBooleanProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, propertyValue == null ? null : String.valueOf(propertyValue));
    }

    @SuppressWarnings("unchecked")
    public void setProperty(String propertyName, Enum<?> propertyValue) {
        Enum<?> defaultValue = propertyValue == null ? null : getDefaultEnumProperty(propertyName, propertyValue.getClass());
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, propertyValue == null ? null : propertyValue.name());
    }

    public void setProperty(String propertyName, FontData propertyValue) {
        FontData defaultValue = getDefaultFontProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, Converter.fontdataToString(propertyValue));
    }

    public void setProperty(String propertyName, RGB propertyValue) {
        RGB defaultValue = getDefaultColorProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, Converter.rgbToString(propertyValue));
    }

    public void setProperty(String propertyName, Integer propertyValue) {
        Integer defaultValue = getDefaultIntegerProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, Converter.integerToString(propertyValue));
    }

    public void setProperty(String propertyName, Double propertyValue) {
        Double defaultValue = getDefaultDoubleProperty(propertyName);
        if (defaultValue != null && defaultValue.equals(propertyValue))
            propertyValue = null;
        doSetProperty(propertyName, Converter.doubleToString(propertyValue));
    }

    public String getDefaultStringProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof String)
            return (String)defaultValue;
        else
            return StringUtils.EMPTY;
    }

    public boolean getDefaultBooleanProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof Boolean)
            return (Boolean)defaultValue;
        else
            return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T getDefaultEnumProperty(String propertyName, Class<T> type) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue != null && type.isInstance(defaultValue))
            return (T)defaultValue;
        else
            return null;
    }

    public FontData getDefaultFontProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof FontData)
            return (FontData)defaultValue;
        else
            return null;
    }

    public RGB getDefaultColorProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof RGB)
            return (RGB)defaultValue;
        else
            return null;
    }

    public Integer getDefaultIntegerProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof Integer)
            return (Integer)defaultValue;
        else
            return null;
    }

    public Double getDefaultDoubleProperty(String propertyName) {
        Object defaultValue = PlotDefaults.getDefaultPropertyValue(propertyName);
        if (defaultValue instanceof Double)
            return (Double)defaultValue;
        else
            return null;
    }

    protected IPropertyDescriptor[] createDescriptors(Object defaultId, String[] ids, String[] names) {
        if (ids == null || names == null)
            return new IPropertyDescriptor[0];
        Assert.isTrue(ids.length == names.length);

        IPropertyDescriptor[] descriptors = new IPropertyDescriptor[ids.length+1];
        descriptors[0] = new PropertyDescriptor(defaultId, "default");
        for (int i= 0; i < ids.length; ++i)
            descriptors[i+1] = new PropertyDescriptor(ids[i], names[i]);

        return descriptors;
    }

    String propertyName(String baseName, String elementId) {
        return elementId == null ? baseName : baseName + "/" + elementId;
    }
}
