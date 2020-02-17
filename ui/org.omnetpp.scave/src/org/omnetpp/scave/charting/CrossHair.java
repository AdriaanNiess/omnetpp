/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

import static org.apache.commons.math.util.MathUtils.addAndCheck;
import static org.apache.commons.math.util.MathUtils.mulAndCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Control;
import org.omnetpp.common.canvas.ICoordsMapping;
import org.omnetpp.common.canvas.ZoomableCanvasMouseSupport;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.engine.BigDecimal;
import org.omnetpp.common.ui.HoverSupport;
import org.omnetpp.common.ui.HtmlHoverInfo;
import org.omnetpp.common.ui.IHoverInfoProvider;
import org.omnetpp.common.util.GraphicsUtils;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.scave.charting.LinePlotViewer.LineProperties;
import org.omnetpp.scave.charting.dataset.DatasetUtils;
import org.omnetpp.scave.charting.dataset.IAveragedXYDataset;
import org.omnetpp.scave.charting.dataset.IStringValueXYDataset;
import org.omnetpp.scave.charting.dataset.IXYDataset;
import org.omnetpp.scave.charting.plotter.IPlotSymbol;
import org.omnetpp.scave.engine.Statistics;
import org.omnetpp.scave.model2.StatUtils;

/**
 * Displays crosshair mouse cursor for VectorChart.
 *
 * Note: add the CrossHair to the chart AFTER the {@link ZoomableCanvasMouseSupport}.
 */
class CrossHair {
    public static final Cursor CROSS_CURSOR = new Cursor(null, SWT.CURSOR_CROSS);
    public static final Font CROSSHAIR_NORMAL_FONT = new Font(null, "Arial", 8, SWT.NORMAL);
    public static final Font CROSSHAIR_BOLD_FONT = new Font(null, "Arial", 8, SWT.BOLD);
    public static final Color TOOLTIP_COLOR = new Color(null, 255, 255, 225);

    private static final int MAXCOUNT = 100;
    private static final int HALO = 3;

    private final LinePlotViewer viewer;

    private boolean detailedTooltip = false; // turned on while hovering

    Rectangle plotArea;

    // to transfer the coordinates from mouseMoved() to paint()
    private int canvasX = Integer.MAX_VALUE;
    private int canvasY = Integer.MAX_VALUE;

    protected static class DataPoint {
        int series;
        int index;
        // int multiplicity;

        public DataPoint(int series, int index) {
            this.series = series;
            this.index = index;
        }
    }

    public CrossHair(LinePlotViewer viewer) {
        this.viewer = viewer;

        final LinePlotViewer finalViewer = viewer;
        // add key listener to restore the cross cursor, after other cursor is turned off
        // (by ZoomableCanvasMouseSupport for example)
        viewer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (finalViewer.getCursor() == null)
                    finalViewer.setCursor(CROSS_CURSOR);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (finalViewer.getCursor() == null)
                    finalViewer.setCursor(CROSS_CURSOR);
            }
        });
        viewer.addMouseMoveListener(new MouseMoveListener() {
            public void mouseMove(MouseEvent e) {
                if ((e.stateMask & SWT.BUTTON_MASK) != 0) {
                    invalidatePosition();
                    finalViewer.redraw();
                    return;
                }

                canvasX = e.x;
                canvasY = e.y;
                detailedTooltip = false;

                finalViewer.redraw();  //XXX this is killer if canvas is not cached. unfortunately, graphics.setXORMode() cannot be used

                // set cursor
                if (plotArea != null && plotArea.contains(canvasX,canvasY)) {
                    if (finalViewer.getCursor() == null)
                        finalViewer.setCursor(CROSS_CURSOR);
                }
                else
                    finalViewer.setCursor(null);
            }
        });
        viewer.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                invalidatePosition();
                finalViewer.redraw();
            }
            @Override
            public void mouseExit(MouseEvent e) {
                invalidatePosition();
                finalViewer.redraw();
            }
            @Override
            public void mouseHover(MouseEvent e) {
                detailedTooltip = true;
                finalViewer.redraw();
            }
        });
        viewer.addMouseListener(new MouseListener() {
            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                invalidatePosition();
                finalViewer.redraw();
            }

            public void mouseUp(MouseEvent e) {
                if (finalViewer.getCursor() == null)
                    finalViewer.setCursor(CROSS_CURSOR);

                canvasX = e.x;
                canvasY = e.y;
                detailedTooltip = false;
                finalViewer.redraw();
            }
        });
        HoverSupport hoverSupport = new HoverSupport();
        hoverSupport.setHoverSizeConstaints(500, 400);
        hoverSupport.adapt(viewer, new IHoverInfoProvider() {
            @Override
            public HtmlHoverInfo getHoverFor(Control control, int x, int y) {
                return new HtmlHoverInfo(getHoverText(x, y, finalViewer.getOptimizedCoordinateMapper()));
            }
        });
    }

    private void invalidatePosition() {
        canvasX = Integer.MAX_VALUE;
        canvasY = Integer.MAX_VALUE;
        detailedTooltip = false;
    }

    public Rectangle layout(Graphics graphics, Rectangle rect) {
        this.plotArea = rect;
        return rect;
    }

    public void draw(Graphics graphics, ICoordsMapping coordsMapping) {
        if (plotArea != null && plotArea.contains(canvasX, canvasY)) {
            graphics.pushState();
            // collect points close to cursor (x,y)
            ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
            int totalFound = dataPointsNear(canvasX, canvasY, HALO, dataPoints, 1, coordsMapping);

            // snap to data point
            DataPoint dataPoint = dataPoints.isEmpty() ? null : dataPoints.get(0);
            if (dataPoint != null) {
                IXYDataset dataset = viewer.getDataset();
                double x = viewer.transformX(dataset.getX(dataPoint.series, dataPoint.index));
                double y = viewer.transformY(dataset.getY(dataPoint.series, dataPoint.index));
                canvasX = (int)coordsMapping.toCanvasX(x);
                canvasY = (int)coordsMapping.toCanvasY(y);
            }

            // draw crosshair
            graphics.setLineDash(new int[] {3, 3});
            graphics.setLineWidth(1);
            graphics.setForegroundColor(ColorFactory.RED);
            graphics.drawLine(plotArea.x, canvasY, plotArea.x + plotArea.width, canvasY);
            graphics.drawLine(canvasX, plotArea.y, canvasX, plotArea.y + plotArea.height);

            // draw tooltip
            drawTooltip(graphics, dataPoints, totalFound, coordsMapping);
            graphics.popState();
        }
    }

    private void drawTooltip(Graphics graphics, ArrayList<DataPoint> dataPoints, int totalFound, ICoordsMapping coordsMapping) {
        graphics.setForegroundColor(ColorFactory.BLACK);
        graphics.setBackgroundColor(TOOLTIP_COLOR);
        graphics.setLineStyle(SWT.LINE_SOLID);
        graphics.setFont(CROSSHAIR_NORMAL_FONT);

        if (dataPoints.isEmpty() || !detailedTooltip) {
            double x = viewer.inverseTransformX(coordsMapping.fromCanvasX(canvasX));
            double y = viewer.inverseTransformY(coordsMapping.fromCanvasY(canvasY));
            String coordinates = String.format("%g, %g", x, y); //FIXME precision: the x distance one pixel represents!
            Point size = GraphicsUtils.getTextExtent(graphics, coordinates);
            int left = canvasX + 3;
            int top = canvasY - size.y - 4;
            if (left + size.x + 3 > plotArea.x + plotArea.width)
                left = canvasX - size.x - 6;
            if (top < plotArea.y)
                top = canvasY + 3;
            graphics.fillRectangle(left, top, size.x + 2, size.y + 2);
            graphics.drawRectangle(left, top, size.x + 2, size.y + 2);
            graphics.drawText(coordinates, left + 2, top + 1); // XXX set as tooltip, rather than draw it on the canvas!
        }
    }

    private String getHoverText(int x, int y, ICoordsMapping coordsMapping) {
        ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
        int totalFound = dataPointsNear(x, y, HALO, dataPoints, MAXCOUNT, coordsMapping);
        if (!dataPoints.isEmpty()) {
            Collections.sort(dataPoints, new Comparator<DataPoint> () {
                @Override
                public int compare(DataPoint dp1, DataPoint dp2) {
                    String key1 = viewer.getDataset().getSeriesKey(dp1.series);
                    String key2 = viewer.getDataset().getSeriesKey(dp2.series);
                    return StringUtils.dictionaryCompare(key1, key2);
                }
            });

            StringBuilder htmlText = new StringBuilder();
            StringBuilder plainText  = new StringBuilder();
            int maxTextLength = 0;
            htmlText.append("<table>");
            int lineNo = 0;
            for (DataPoint dp : dataPoints) {
                LineProperties props = viewer.getLineProperties(dp.series);
                Color color = props.getColor();
                String text = getText(dp);
                IPlotSymbol symbol = props.getSymbol();
                String imageFile = SymbolImageFactory.getImageFile(color, symbol, true);
                htmlText.append("<tr>");
                htmlText.append("<td>");
                if (imageFile != null)
                    htmlText.append(String.format("<img src='%s'>", imageFile));
                htmlText.append("</td>");
                htmlText.append("<td>");
                htmlText.append(StringEscapeUtils.escapeHtml4(text));
                if (lineNo > 0)
                    plainText.append("\n");
                plainText.append(text);
                htmlText.append("</td>");
                htmlText.append("</tr>");
                maxTextLength = Math.max(maxTextLength, text.length());
                ++lineNo;
            }
            if (totalFound > dataPoints.size())
                htmlText.append(String.format("<tr><td></td><td>... and %d more</td></tr>", totalFound - dataPoints.size()));
            htmlText.append("</table>");

            TextLayout textLayout = new TextLayout(viewer.getDisplay());
            textLayout.setText(plainText.toString());
            textLayout.setWidth(320); // comes from HoverSupport
            org.eclipse.swt.graphics.Rectangle bounds= textLayout.getBounds();
//          preferredSize.preferredWidth = 20 + bounds.width;
//          preferredSize.preferredHeight = 25 + bounds.height + (lineNo > 1 ? (lineNo - 1) * 6 : 0);

//          Point preferredSize2 = computePreferedSize(htmlText.toString(), 320);
//          preferredSize.preferredWidth =  preferredSize2.x; //20 + maxTextLength * 7;
//          preferredSize.preferredHeight = preferredSize2.y; //25 + dataPoints.size() * 12;

            return HoverSupport.addHTMLStyleSheet(htmlText.toString());
        }
        else
            return null;
    }

    private String getText(DataPoint dataPoint) {
        IXYDataset dataset = viewer.getDataset();
        //String coordinates = String.format("%g, %g", dataset.getXValue(dataPoint.series, dataPoint.index), dataset.getYValue(dataPoint.series, dataPoint.index));
        int series = dataPoint.series;
        int index = dataPoint.index;
        double xConf = Double.NaN, yConf = Double.NaN;
        String xStr = null, yStr = null;
        if (dataset instanceof IStringValueXYDataset) {
            xStr = ((IStringValueXYDataset)dataset).getXAsString(series, index);
            yStr = ((IStringValueXYDataset)dataset).getYAsString(series, index);
        }
        if (xStr == null) {
            BigDecimal xp = dataset.getPreciseX(series, index);
            double x = dataset.getX(series, index);
            if (dataset instanceof IAveragedXYDataset) {
                Statistics xStat = ((IAveragedXYDataset)dataset).getXStatistics(series, index);
                xConf = StatUtils.confidenceInterval(xStat, PlotViewerBase.CONFIDENCE_LEVEL);
            }
            xStr = (xp != null ? xp.toString() : viewer.formatValue(x, xConf));
        }
        if (yStr == null) {
            BigDecimal yp = dataset.getPreciseY(series, index);
            double y = dataset.getY(series, index);
            if (dataset instanceof IAveragedXYDataset) {
                Statistics yStat = ((IAveragedXYDataset)dataset).getYStatistics(series, index);
                yConf = StatUtils.confidenceInterval(yStat, PlotViewerBase.CONFIDENCE_LEVEL);
            }
            yStr = (yp != null ? yp.toString() : viewer.formatValue(y, yConf));
        }
        String seriesStr = dataset.getSeriesKey(series);
        //seriesStr = StringUtils.abbreviate(series, series.length(), 25);
        return String.format("%s, %s - %s", xStr, yStr, seriesStr);
    }

    // XXX move this method into a VectorPlot class
    protected int dataPointsNear(int x, int y, int d, List<DataPoint> result, int maxCount, ICoordsMapping coordsMapping) {
        IXYDataset dataset = viewer.getDataset();
        if (dataset==null)
            return 0;

        // for each series, collect data points close to (x,y), at most maxCount of them
        int totalFound = 0;
        for (int series = 0; series < dataset.getSeriesCount(); ++series) {
            LineProperties props = viewer.getLineProperties(series);
            if (!props.getDisplayLine())
                continue;
            // find data point nearest to cursor x, using binary search
            int mid = DatasetUtils.findXLowerLimit(dataset, series, viewer.inverseTransformX(coordsMapping.fromCanvasX(x)));

            // then search downwards and upwards for data points close to (x,y)
            for (int i = mid; i >= 0; --i) {
                try {
                    double xx = viewer.transformX(dataset.getX(series, i));
                    double yy = viewer.transformY(dataset.getY(series, i));
                    int dx = (int)coordsMapping.toCanvasX(xx) - x;
                    int dy = (int)coordsMapping.toCanvasY(yy) - y;
                    if (addAndCheck(mulAndCheck(dx, dx), mulAndCheck(dy, dy)) <= d * d)
                    {
                        totalFound++;
                        if (result.size() < maxCount)  //XXX add at least one point for each series
                            result.add(new DataPoint(series, i));
                    }
                    if (Math.abs(dx) > d)
                        break;
                }
                catch (ArithmeticException e) {}
            }
            for (int i = mid + 1; i < dataset.getItemCount(series); ++i) {
                try {
                    double xx = viewer.transformX(dataset.getX(series, i));
                    double yy = viewer.transformY(dataset.getY(series, i));
                    int dx = (int)coordsMapping.toCanvasX(xx) - x;
                    int dy = (int)coordsMapping.toCanvasY(yy) - y;
                    if (addAndCheck(mulAndCheck(dx, dx), mulAndCheck(dy, dy)) <= d * d)
                    {
                        totalFound++;
                        if (result.size() < maxCount)
                            result.add(new DataPoint(series, i));
                    }
                    if (Math.abs(dx) > d)
                        break;
                }
                catch (ArithmeticException e) {}
            }
        }
        return totalFound;
    }
}