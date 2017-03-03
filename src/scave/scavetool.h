//=========================================================================
//  SCAVETOOL.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_SCAVE_SCAVETOOL_H
#define __OMNETPP_SCAVE_SCAVETOOL_H

#include <string>
#include "scavedefs.h"
#include "resultfilemanager.h"

namespace omnetpp {
namespace scave {

class ScaveTool
{
protected:
    void loadFiles(ResultFileManager& manager, const std::vector<std::string>& fileNames, bool indexingAllowed, bool verbose);
    std::string rebuildCommandLine(int argc, char **argv);
    void helpCommand(int argc, char **argv);
    void printHelpPage(const std::string& page);
    void queryCommand(int argc, char **argv);
    void vectorCommand(int argc, char **argv);
    void parseScalarFunction(const std::string& functionCall,  /*out*/ std::string& name,  /*out*/ std::vector<std::string>& params);
    void scalarCommand(int argc, char **argv);
    void indexCommand(int argc, char **argv);
public:
    int main(int argc, char **argv);

};

}  // namespace scave
}  // namespace omnetpp

#endif
