//==========================================================================
//  RUNUNTILDIALOG.H - part of
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

#ifndef RUNUNTILDIALOG_H
#define RUNUNTILDIALOG_H

#include <QDialog>
#include "qtenv.h"

namespace Ui {
class RunUntilDialog;
}

namespace omnetpp {

class cObject;

namespace qtenv {

class RunUntilDialog : public QDialog
{
    Q_OBJECT
private:
    QList<cObject*> fesEvents(int maxNum, bool wantEvents, bool wantSelfMsgs, bool wantNonSelfMsgs, bool wantSilentMsgs);

public:
    explicit RunUntilDialog(QWidget *parent = 0);
    ~RunUntilDialog();

    simtime_t getTime();
    eventnumber_t getEventNumber();
    cObject *getMessage();
    Qtenv::eRunMode getMode();

public slots:
    virtual void accept();

private:
    Ui::RunUntilDialog *ui;
};

} // namespace qtenv
} // namespace omnetpp

#endif // RUNUNTILDIALOG_H
