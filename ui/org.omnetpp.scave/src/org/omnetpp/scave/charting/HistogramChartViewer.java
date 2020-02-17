/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_BAR_BASELINE;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_LABELS_FONT;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_X_AXIS_TITLE;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_Y_AXIS_LOGARITHMIC;
import static org.omnetpp.scave.charting.properties.ChartDefaults.DEFAULT_Y_AXIS_TITLE;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_AXIS_TITLE_FONT;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_LABEL_FONT;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_X_AXIS_TITLE;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_X_LABELS_ROTATE_BY;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_Y_AXIS_LOGARITHMIC;
import static org.omnetpp.scave.charting.properties.ChartVisualProperties.PROP_Y_AXIS_TITLE;
import static org.omnetpp.scave.charting.properties.HistogramChartVisualProperties.PROP_HIST_BAR;
import static org.omnetpp.scave.charting.properties.HistogramChartVisualProperties.PROP_HIST_DATA;
import static org.omnetpp.scave.charting.properties.HistogramChartVisualProperties.PROP_SHOW_OVERFLOW_CELL;
import static org.omnetpp.scave.charting.properties.HistogramVisualProperties.PROP_HIST_COLOR;
import static org.omnetpp.scave.charting.properties.BarChartVisualProperties.PROP_BAR_BASELINE;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.omnetpp.common.Debug;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.common.canvas.RectangularArea;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.GraphicsUtils;
import org.omnetpp.scave.charting.dataset.IDataset;
import org.omnetpp.scave.charting.dataset.IHistogramDataset;
import org.omnetpp.scave.charting.properties.ChartDefaults;
import org.omnetpp.scave.charting.properties.HistogramChartVisualProperties.HistogramBar;
import org.omnetpp.scave.charting.properties.HistogramChartVisualProperties.HistogramDataType;

public class HistogramChartViewer extends ChartViewer { //TODO HistogramPlot

    private static final boolean debug = false;


    private IHistogramDataset dataset = IHistogramDataset.EMPTY;
    private LinearAxis xAxis = new LinearAxis(false, false, false);
    private LinearAxis yAxis = new LinearAxis(true, DEFAULT_Y_AXIS_LOGARITHMIC, false);
    private HistogramPlot plot;

    private PropertyMap<HistogramProperties> properties = new PropertyMap<HistogramProperties>(HistogramProperties.class);
    static class HistogramProperties {
        RGB color;
    }

    public HistogramChartViewer(Composite parent, int style) {
        super(parent, style);
        plot = new HistogramPlot(this);
        new Tooltip(this);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                setSelection(new IChartSelection() {
                });
            }
        });
    }

    @Override
    protected double transformX(double x) {
        return xAxis.transform(x);
    }

    @Override
    protected double transformY(double y) {
        return yAxis.transform(y);
    }

    @Override
    protected double inverseTransformX(double x) {
        return xAxis.inverseTransform(x);
    }

    @Override
    protected double inverseTransformY(double y) {
        return yAxis.inverseTransform(y);
    }

    @Override
    public void setProperty(String name, String value) {
        Assert.isLegal(name != null);
        if (debug) Debug.println("HistogramChartViewer.setProperty: "+name+"='"+value+"'");

        if (PROP_X_AXIS_TITLE.equals(name))
            setXAxisTitle(value);
        else if (PROP_Y_AXIS_TITLE.equals(name))
            setYAxisTitle(value);
        else if (PROP_AXIS_TITLE_FONT.equals(name))
            setAxisTitleFont(Converter.stringToSwtfont(value));
        else if (PROP_LABEL_FONT.equals(name))
            setTickLabelFont(Converter.stringToSwtfont(value));
        else if (PROP_X_LABELS_ROTATE_BY.equals(name))
            ; //TODO PROP_X_LABELS_ROTATE_BY
        else if (PROP_HIST_BAR.equals(name))
            setBarType(Converter.stringToEnum(value, HistogramBar.class));
        else if (PROP_HIST_DATA.equals(name))
            setHistogramDataTransform(Converter.stringToEnum(value, HistogramDataType.class));
        else if (PROP_SHOW_OVERFLOW_CELL.equals(name))
            setShowOverflowCell(Converter.stringToBoolean(value));
        else if (PROP_BAR_BASELINE.equals(name))
            setBarBaseline(Converter.stringToDouble(value));
        else if (name.startsWith(PROP_HIST_COLOR))
            setHistogramColor(getElementId(name), ColorFactory.asRGB(value));
        else if (PROP_Y_AXIS_LOGARITHMIC.equals(name))
            setLogarithmicY(Converter.stringToBoolean(value));
        else
            super.setProperty(name, value);
    }

    public void setXAxisTitle(String value) {
        xAxis.setTitle(value != null ? value : DEFAULT_X_AXIS_TITLE);
        chartChanged();
    }

    public void setYAxisTitle(String value) {
        yAxis.setTitle(value != null ? value : DEFAULT_Y_AXIS_TITLE);
        chartChanged();
    }

    public void setAxisTitleFont(Font value) {
        if (value != null) {
            xAxis.setTitleFont(value);
            yAxis.setTitleFont(value);
            chartChanged();
        }
    }

    public void setTickLabelFont(Font font) {
        if (font == null)
            font = DEFAULT_LABELS_FONT;
        if (font != null) {
            xAxis.setTickFont(font);
            yAxis.setTickFont(font);
            chartChanged();
        }
    }

    public void setBarType(HistogramBar barType) {
        if (barType == null)
            barType = ChartDefaults.DEFAULT_HIST_BAR;
        plot.setBarType(barType);
        chartChanged();
    }

    public void setHistogramDataTransform(HistogramDataType dataTransform) {
        if (dataTransform == null)
            dataTransform = ChartDefaults.DEFAULT_HIST_DATA;
        plot.setHistogramData(dataTransform);
        chartArea = calculatePlotArea();
        updateArea();
        chartChanged();
    }

    public void setShowOverflowCell(Boolean value) {
        if (value == null)
            value = ChartDefaults.DEFAULT_SHOW_OVERFLOW_CELL;
        plot.showOverflowCell = value;
        chartArea = calculatePlotArea();
        updateArea();
        chartChanged();
    }


    public void setBarBaseline(Double value) {
        if (value == null)
            value = DEFAULT_BAR_BASELINE;

        plot.baseline = value;
        chartArea = calculatePlotArea();
        updateArea();
        chartChanged();
    }

    public void setLogarithmicY(Boolean value) {
        boolean logarithmic = value != null ? value : DEFAULT_Y_AXIS_LOGARITHMIC;
        yAxis.setLogarithmic(logarithmic);
        chartArea = calculatePlotArea();
        updateArea();
        chartChanged();
    }

    public RGB getHistogramColor(String key) {
        HistogramProperties histProps = properties.getProperties(key);
        if (histProps == null || histProps.color == null)
            histProps = properties.getDefaultProperties();
        return histProps != null ? histProps.color : null;
    }

    public void setHistogramColor(String key, RGB color) {
        HistogramProperties histProps = properties.getOrCreateProperties(key);
        histProps.color = color;
        updateLegends();
        chartChanged();
    }

    @Override
    void doSetDataset(IDataset dataset) {
        Assert.isLegal(dataset instanceof IHistogramDataset, "must be an IScalarDataset");
        this.dataset = (IHistogramDataset)dataset;
        updateLegends();
        chartArea = calculatePlotArea();
        updateArea();
        chartChanged();
    }

    IHistogramDataset getDataset() {
        return dataset;
    }

    private void updateLegends() {
        plot.updateLegend(legendTooltip);
        plot.updateLegend(legend);
    }

    @Override
    String getHoverHtmlText(int x, int y) {
        if (plot.getArea().contains(x, y))
            return plot.getTooltipText(x, y);
        else
            return null;
    }

    @Override
    protected RectangularArea calculatePlotArea() {
        return plot.calculatePlotArea();
    }

    Rectangle mainArea; // containing plots and axes
    Insets axesInsets; // space occupied by axes

    @Override
    protected Rectangle doLayoutChart(Graphics graphics, int pass) {
        ICoordsMapping coordsMapping = getOptimizedCoordinateMapper();
        if (pass == 1) {
            // Calculate space occupied by title and legend and set insets accordingly
            Rectangle area = new Rectangle(getClientArea());
            Rectangle remaining = legendTooltip.layout(graphics, area);
            remaining = title.layout(graphics, area);
            remaining = legend.layout(graphics, remaining, pass);

            mainArea = remaining.getCopy();
            axesInsets = xAxis.layout(graphics, mainArea, new Insets(), coordsMapping, pass);
            axesInsets = yAxis.layout(graphics, mainArea, axesInsets, coordsMapping, pass);

            // tentative plotArea calculation (y axis ticks width missing from the picture yet)
            Rectangle plotArea = mainArea.getCopy().crop(axesInsets);
            return plotArea;
        }
        else if (pass == 2) {
            // now the coordinate mapping is set up, so the y axis knows what tick labels
            // will appear, and can calculate the occupied space from the longest tick label.
            yAxis.layout(graphics, mainArea, axesInsets, coordsMapping, pass);
            xAxis.layout(graphics, mainArea, axesInsets, coordsMapping, pass);
            Rectangle remaining = mainArea.getCopy().crop(axesInsets);
            Rectangle plotArea = plot.layout(graphics, remaining);
            legend.layout(graphics, plotArea, pass);
            //FIXME how to handle it when plotArea.height/width comes out negative??
            return plotArea;
        }
        return null;
    }

    @Override
    protected void doPaintCachableLayer(Graphics graphics, ICoordsMapping coordsMapping) {
        graphics.fillRectangle(GraphicsUtils.getClip(graphics));
        yAxis.drawGrid(graphics, coordsMapping);
        plot.draw(graphics, coordsMapping);
    }

    @Override
    protected void doPaintNoncachableLayer(Graphics graphics, ICoordsMapping coordsMapping) {
        paintInsets(graphics);
        title.draw(graphics);
        legend.draw(graphics);
        xAxis.drawAxis(graphics, coordsMapping);
        yAxis.drawAxis(graphics, coordsMapping);
        legendTooltip.draw(graphics);
        drawStatusText(graphics);
        drawRubberband(graphics);
    }

    @Override
    public void setDisplayAxesDetails(Boolean value) {
        xAxis.setDrawTickLabels(value);
        xAxis.setDrawTitle(value);
        yAxis.setDrawTickLabels(value);
        yAxis.setDrawTitle(value);
        chartChanged();
    }
}
