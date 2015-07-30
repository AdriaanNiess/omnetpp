//==========================================================================
//  SUBMODULEITEM.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_QTENV_SUBMODULEITEM_H
#define __OMNETPP_QTENV_SUBMODULEITEM_H

#include "qtutil.h"

#include <QGraphicsColorizeEffect>
#include <QGraphicsObject>
#include <QAbstractGraphicsShapeItem>

namespace omnetpp {

class cModule;
class cDisplayString;

namespace qtenv {

class SubmoduleItem;
class SubmoduleItemUtil {
public:
    static void setupFromDisplayString(SubmoduleItem *si, cModule *mod);
    static QColor parseColor(const QString &name);
};


class SubmoduleItem : public QGraphicsObject
{
    Q_OBJECT

protected:
    cModule *module;

    // Sets the text of the name item, appending the
    // vector index to it if any, and center-aligns it.
    void updateNameItem();

    // Realigns the queue length label, the info text, and the
    // decoration icon. Call this every time after the size
    // of the image or shape changes.
    void realignAnchoredItems();
    void adjustShapeItem();
    QRectF shapeImageBoundingRect() const; // whichever is bigger in each direction

protected slots:
    void onPositionChanged(); // keeping the range items underneath outselves

public:
    enum Shape {
        SHAPE_NONE,
        SHAPE_OVAL,
        SHAPE_RECT
    };
    enum TextPos {
        TEXTPOS_LEFT,
        TEXTPOS_RIGHT,
        TEXTPOS_TOP
    };

protected:
    // appearance
    int alpha = 255;
    QString name;
    int vectorIndex = -1; // if < 0, not displayed
    Shape shape = SHAPE_NONE;
    int shapeWidth = 0; // zero if unspec
    int shapeHeight = 0; // zero if unspec
    QColor shapeFillColor;
    QColor shapeBorderColor;
    int shapeBorderWidth = 2;
    QImage *image = nullptr; // not owned
    QImage *decoratorImage = nullptr; // not owned
    bool pinVisible = false;
    bool nameVisible = true;
    QString text;
    TextPos textPos = TEXTPOS_TOP;
    QColor textColor;
    QString queueText;
    QList<QGraphicsEllipseItem *> rangeItems;

    QAbstractGraphicsShapeItem *shapeItem = nullptr;
    QGraphicsPixmapItem *imageItem = nullptr;
    // TODO FIXME - this effect does not look the same as the one in tkenv
    QGraphicsColorizeEffect *colorizeEffect = nullptr; // owned by the image item
    QGraphicsPixmapItem *decoratorImageItem = nullptr;
    // TODO FIXME - this effect does not look the same as the one in tkenv
    QGraphicsColorizeEffect *decoratorColorizeEffect; // owned by the decorator image item

    OutlinedTextItem *nameItem = nullptr; // includes the vector index
    OutlinedTextItem *textItem = nullptr;
    OutlinedTextItem *queueItem = nullptr;

    GraphicsLayer *rangeLayer = nullptr;

public:
    SubmoduleItem(cModule *mod);
    virtual ~SubmoduleItem();

    void setShape(Shape shape);
    void setWidth(int width);
    void setHeight(int height);
    void setFillColor(const QColor &color);
    void setBorderColor(const QColor &color);
    void setBorderWidth(int width);

    void setImage(QImage *image);
    void setImageColor(const QColor &color);
    void setImageColorPercentage(int percent);

    void setDecoratorImage(QImage *image);
    void setDecoratorImageColor(const QColor &color);
    void setDecoratorImageColorPercentage(int percent);

    void setName(const QString &text);
    void setVectorIndex(int index);
    void setQueueText(const QString &queueText);
    void setInfoText(const QString &text, TextPos pos, const QColor &color);

    void setRangeLayer(GraphicsLayer *layer);
    QList<QGraphicsEllipseItem *> &getRangeItems();
    void addRangeItem(double r, QColor fillColor, QColor outlineColor, int outlineWidth);

    QRectF boundingRect() const;
    void paint(QPainter *painter, const QStyleOptionGraphicsItem *option, QWidget *widget);
};


} // namespace qtenv
} // namespace omnetpp

#endif // __OMNETPP_QTENV_SUBMODULEITEM_H