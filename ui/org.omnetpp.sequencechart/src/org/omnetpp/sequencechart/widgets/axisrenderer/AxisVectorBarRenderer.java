/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.sequencechart.widgets.axisrenderer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.eventlog.engine.SequenceChartFacade;
import org.omnetpp.scave.engine.EnumType;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.engine.XYArray;
import org.omnetpp.sequencechart.widgets.SequenceChart;

/**
 * A special axis representation for sequence charts which displays a horizontal colored bar
 * with the names representing the individual values of a data vector.
 */
public class AxisVectorBarRenderer implements IAxisRenderer {
    private static final Color AXIS_COLOR = ColorFactory.BLACK;

    private static final Font VALUE_NAME_FONT = new Font(null, "Courier New", 8, 0);

    private static final Color VALUE_NAME_COLOR = ColorFactory.BLACK;

    private static final Color NO_VALUE_COLOR = ColorFactory.WHITE;

    private SequenceChart sequenceChart;

    private String vectorFileName;

    private String vectorRunName;

    private String vectorModuleFullPath;

    private String vectorName;

    private XYArray data;

    private ResultItem.DataType type;

    private EnumType enumType;

    public AxisVectorBarRenderer(SequenceChart sequenceChart, String vectorFileName, String vectorRunName, String vectorModuleFullPath, String vectorName, ResultItem resultItem, XYArray data) {
        this.sequenceChart = sequenceChart;
        this.vectorFileName = vectorFileName;
        this.vectorRunName = vectorRunName;
        this.vectorModuleFullPath = vectorModuleFullPath;
        this.vectorName = vectorName;
        this.data = data;
        this.type = resultItem.getDataType();
        if (type == ResultItem.DataType.TYPE_ENUM)
            enumType = resultItem.getEnum();
    }

    public String getVectorFileName() {
        return vectorFileName;
    }

    public String getVectorRunName() {
        return vectorRunName;
    }

    public String getVectorModuleFullPath() {
        return vectorModuleFullPath;
    }

    public String getVectorName() {
        return vectorName;
    }

    public int getHeight() {
        return 13;
    }

    /**
     * Draws a colored tick bar based on the values in the data vector in the given range.
     */
    public void drawAxis(Graphics graphics, long startEventPtr, long endEventPtr)
    {
        Rectangle rect = graphics.getClip(Rectangle.SINGLETON);
        int size = getDataLength();

        int startIndex = getIndex(startEventPtr, true);
        if (startIndex == -1)
            startIndex = 0;

        int endIndex = getIndex(endEventPtr, false);
        if (endIndex == -1)
            endIndex = size;

        // draw default color where no value is available
        graphics.setLineStyle(SWT.LINE_SOLID);
        graphics.setBackgroundColor(NO_VALUE_COLOR);
        graphics.fillRectangle(rect.x, 0, rect.right() - rect.x, getHeight());

        SequenceChartFacade sequenceChartFacade = sequenceChart.getInput().getSequenceChartFacade();
        long endEventNumber = sequenceChartFacade.IEvent_getEventNumber(endEventPtr);

        // draw axis as a colored thick line with labels representing values
        // two phases: first draw the background and after that draw the values
        for (int phase = 0; phase < 2; phase++) {
            for (int i = startIndex; i < endIndex; i++) {
                long eventNumber = getEventNumber(i);
                long nextEventNumber = Math.min(endEventNumber, (i == size - 1) ? endEventNumber : getEventNumber(i + 1));

                if (eventNumber == -1 || nextEventNumber == -1)
                    continue;

                long eventPtr = sequenceChartFacade.IEvent_getEventForEventNumber(eventNumber);
                long nextEventPtr = sequenceChartFacade.IEvent_getEventForEventNumber(nextEventNumber);

                int x1 = Integer.MAX_VALUE;
                int x2 = Integer.MAX_VALUE;

                // check for events being filtered out
                if (eventPtr != 0)
                    x1 = (int)sequenceChart.getViewportCoordinateForTimelineCoordinate(sequenceChartFacade.getTimelineCoordinateBegin(eventPtr));
                else {
                    eventPtr = sequenceChartFacade.IEvent_getNonFilteredEventForEventNumber(eventNumber);
                    org.omnetpp.common.engine.BigDecimal eventSimulationTime = sequenceChartFacade.IEvent_getSimulationTime(eventPtr);
                    double eventTimelineCoordinate = sequenceChartFacade.getTimelineCoordinateForSimulationTime(eventSimulationTime, false);

                    if (eventTimelineCoordinate == sequenceChartFacade.getTimelineCoordinateForSimulationTime(eventSimulationTime, true))
                        x1 = (int)sequenceChart.getViewportCoordinateForTimelineCoordinate(eventTimelineCoordinate);
                }

                if (nextEventPtr != 0)
                    x2 = (int)sequenceChart.getViewportCoordinateForTimelineCoordinate(sequenceChartFacade.getTimelineCoordinateBegin(nextEventPtr));
                else {
                    nextEventPtr = sequenceChartFacade.IEvent_getNonFilteredEventForEventNumber(nextEventNumber);
                    org.omnetpp.common.engine.BigDecimal nextEventSimulationTime = sequenceChartFacade.IEvent_getSimulationTime(nextEventPtr);
                    double nextEventTimelineCoordinate = sequenceChartFacade.getTimelineCoordinateForSimulationTime(nextEventSimulationTime, false);

                    if (nextEventTimelineCoordinate == sequenceChartFacade.getTimelineCoordinateForSimulationTime(nextEventSimulationTime, true))
                        x2 = (int)sequenceChart.getViewportCoordinateForTimelineCoordinate(nextEventTimelineCoordinate);
                }

                if (x1 == Integer.MAX_VALUE || x2 == Integer.MAX_VALUE)
                    continue;

                int colorIndex = getValueIndex(i);
                graphics.setBackgroundColor(ColorFactory.getGoodLightColor(colorIndex));

                if (phase == 0) {
                    graphics.fillRectangle(x1, 0, x2 - x1, getHeight());
                    graphics.setForegroundColor(AXIS_COLOR);
                    graphics.drawLine(x1, 0, x1, getHeight());
                }

                // draw labels starting at each value change and repeat labels based on canvas width
                if (phase == 1) {
                    String name = getValueText(i);
                    int labelWidth = graphics.getFontMetrics().getAverageCharWidth() * name.length();

                    if (x2 - x1 > labelWidth + 6) {
                        graphics.setForegroundColor(VALUE_NAME_COLOR);
                        graphics.setFont(VALUE_NAME_FONT);

                        int x = x1 + 5;
                        while (x < rect.right() && x < x2 - labelWidth) {
                            graphics.drawText(name, x, 0);
                            x += sequenceChart.getClientArea().width;
                        }
                    }
                }
            }
        }

        graphics.setForegroundColor(AXIS_COLOR);
        graphics.drawLine(rect.x, 0, rect.right(), 0);
        graphics.drawLine(rect.x, getHeight(), rect.right(), getHeight());
    }

    /**
     * Returns the element index having less or greater or equal event number in the data array depending on the given flag.
     */
    public int getIndex(long eventPtr, boolean before)
    {
        int index = -1;
        int left = 0;
        int right = getDataLength();
        long eventNumber = sequenceChart.getInput().getSequenceChartFacade().IEvent_getEventNumber(eventPtr);

        while (left <= right) {
            int mid = (right + left) / 2;

            if (getEventNumber(mid) == eventNumber) {
                do {
                    if (before)
                        mid--;
                    else
                        mid++;
                }
                while (mid >= 0 && mid < getDataLength() && getEventNumber(mid) == eventNumber);

                index = mid;
                break;
            }
            else if (eventNumber < getEventNumber(mid))
                right = mid - 1;
            else
                left = mid + 1;
        }

        if (left > right)
            if (before)
                if (eventNumber < getEventNumber(left))
                    index = left - 1;
                else
                    index = left;
            else
                if (eventNumber > getEventNumber(right))
                    index = right + 1;
                else
                    index = right;

        if (index < 0 || index >= getDataLength())
            return -1;
        else {
            Assert.isTrue((before && getEventNumber(index) < eventNumber) ||
                          (!before && getEventNumber(index) > eventNumber));
            return index;
        }
    }

    /**
     * Returns the index having less or greater or equal simulation time in the data array depending on the given flag.
     */
    public int getIndex(org.omnetpp.common.engine.BigDecimal simulationTime, boolean before)
    {
        int index = -1;
        int left = 0;
        int right = getDataLength();

        while (left <= right) {
            int mid = (right + left) / 2;

            if (getSimulationTime(mid) == simulationTime) {
                do {
                    if (before)
                        mid--;
                    else
                        mid++;
                }
                while (mid >= 0 && mid < getDataLength() && getSimulationTime(mid) == simulationTime);

                index = mid;
                break;
            }
            else if (simulationTime.less(getSimulationTime(mid)))
                right = mid - 1;
            else
                left = mid + 1;
        }

        if (left > right)
            if (before)
                if (simulationTime.less(getSimulationTime(left)))
                    index = left - 1;
                else
                    index = left;
            else
                if (simulationTime.greater(getSimulationTime(right)))
                    index = right + 1;
                else
                    index = right;

        if (index < 0 || index >= getDataLength())
            return -1;
        else {
            Assert.isTrue((before && getSimulationTime(index).less(simulationTime)) ||
                          (!before && getSimulationTime(index).greater(simulationTime)));
            return index;
        }
    }

    public int getDataLength()
    {
        return data.length();
    }

    public org.omnetpp.common.engine.BigDecimal getSimulationTime(int index)
    {
        return data.getPreciseX(index);
    }

    public long getEventNumber(int index)
    {
        return data.getEventNumber(index);
    }

    public double getValue(int index)
    {
        return data.getY(index);
    }

    private int getValueIndex(int index)
    {
        if (type == ResultItem.DataType.TYPE_ENUM || type == ResultItem.DataType.TYPE_INT)
            return (int)Math.floor(getValue(index));
        else
            return index % 2;
    }

    private String getValueText(int index)
    {
        if (type == ResultItem.DataType.TYPE_ENUM)
            return enumType.nameOf((int)getValue(index));
        else {
            double value = getValue(index);

            if (value == Math.floor(value))
                return String.valueOf((long)value);
            else
                return String.valueOf(value);
        }
    }
}