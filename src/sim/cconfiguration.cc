//=========================================================================
//  CCONFIGURATION.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2003-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#include <cerrno>
#include "common/unitconversion.h"
#include "common/stringutil.h"
#include "common/fileutil.h"
#include "common/stringtokenizer.h"
#include "common/fnamelisttokenizer.h"
#include "common/exception.h"
#include "omnetpp/cconfiguration.h"
#include "omnetpp/cconfigoption.h"

using namespace omnetpp::common;

namespace omnetpp {

bool cConfiguration::parseBool(const char *s, const char *defaultValue, bool fallbackValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        return fallbackValue;
    if (strcmp(s, "yes") == 0 || strcmp(s, "true") == 0)
        return true;
    else if (strcmp(s, "no") == 0 || strcmp(s, "false") == 0)
        return false;
    else
        throw opp_runtime_error("'%s' is not a valid boolean value, use true/false", s);
}

long cConfiguration::parseLong(const char *s, const char *defaultValue, long fallbackValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        return fallbackValue;
    return opp_atol(s);
}

double cConfiguration::parseDouble(const char *s, const char *unit, const char *defaultValue, double fallbackValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        return fallbackValue;
    return UnitConversion::parseQuantity(s, unit);
}

std::string cConfiguration::parseString(const char *s, const char *defaultValue, const char *fallbackValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        return fallbackValue;
    if (*s == '"')
        return opp_parsequotedstr(s);
    else
        return s;
}

inline std::string unquote(const std::string& txt)
{
    if (txt.find('"') == std::string::npos)
        return txt;
    try {
        return opp_parsequotedstr(txt.c_str());
    }
    catch (std::exception& e) {
        return txt;
    }
}

std::string cConfiguration::parseFilename(const char *s, const char *baseDir, const char *defaultValue)
{
    if (!s)
        s = defaultValue;
    if (!s || !s[0])
        return "";
    return tidyFilename(concatDirAndFile(baseDir, unquote(s).c_str()).c_str());
}

std::vector<std::string> cConfiguration::parseFilenames(const char *s, const char *baseDir, const char *defaultValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        s = "";
    std::vector<std::string> result;
    FilenamesListTokenizer tokenizer(s); // note: this observes quotation marks, although ignores backslashes (khmmm...)
    const char *fname;
    while ((fname = tokenizer.nextToken()) != nullptr) {
        if (fname[0] == '@' && fname[1] == '@')
            result.push_back("@@" + tidyFilename(concatDirAndFile(baseDir, fname + 2).c_str()));
        else if (fname[0] == '@')
            result.push_back("@" + tidyFilename(concatDirAndFile(baseDir, fname + 1).c_str()));
        else
            result.push_back(tidyFilename(concatDirAndFile(baseDir, fname).c_str()));
    }
    return result;
}

std::string cConfiguration::adjustPath(const char *s, const char *baseDir, const char *defaultValue)
{
    if (!s)
        s = defaultValue;
    if (!s)
        s = "";
    std::string result;
    StringTokenizer tokenizer(s, PATH_SEPARATOR);
    const char *dirName;
    while ((dirName = tokenizer.nextToken()) != nullptr) {
        if (result.size() != 0)
            result += ";";
        result += tidyFilename(concatDirAndFile(baseDir, dirName).c_str());
    }
    return result;
}

//---

#define TRY(CODE)    try { CODE; } catch (std::exception& e) { throw cRuntimeError("Could not read option %s= from the configuration: %s", option->getName(), e.what()); }

inline const char *fallback(const char *s, const char *defaultValue, const char *fallbackValue)
{
    return s != nullptr ? s : defaultValue != nullptr ? defaultValue : fallbackValue;
}

static void assertType(cConfigOption *option, bool isPerObject, cConfigOption::Type requiredType)
{
    if (option->isPerObject() != isPerObject) {
        if (option->isPerObject())
            throw cRuntimeError("Option %s= is read from the configuration in the wrong way: It is per-object configuration", option->getName());
        else
            throw cRuntimeError("Option %s= is read from the configuration in the wrong way: It is global (not per-object) configuration", option->getName());
    }
    if (option->getType() != requiredType)
        throw cRuntimeError("Option %s= is read from the configuration with the wrong type (type=%s, actual=%s)",
                option->getName(), cConfigOption::getTypeName(requiredType), cConfigOption::getTypeName(option->getType()));
}

const char *cConfiguration::getAsCustom(cConfigOption *option, const char *fallbackValue) const
{
    assertType(option, false, cConfigOption::CFG_CUSTOM);
    TRY(return fallback(getConfigValue(option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

bool cConfiguration::getAsBool(cConfigOption *option, bool fallbackValue) const
{
    assertType(option, false, cConfigOption::CFG_BOOL);
    TRY(return parseBool(getConfigValue(option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

long cConfiguration::getAsInt(cConfigOption *option, long fallbackValue) const
{
    assertType(option, false, cConfigOption::CFG_INT);
    TRY(return parseLong(getConfigValue(option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

double cConfiguration::getAsDouble(cConfigOption *option, double fallbackValue) const
{
    assertType(option, false, cConfigOption::CFG_DOUBLE);
    TRY(return parseDouble(getConfigValue(option->getName()), option->getUnit(), substituteVariables(option->getDefaultValue()), fallbackValue));
}

std::string cConfiguration::getAsString(cConfigOption *option, const char *fallbackValue) const
{
    assertType(option, false, cConfigOption::CFG_STRING);
    TRY(return parseString(getConfigValue(option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

std::string cConfiguration::getAsFilename(cConfigOption *option) const
{
    assertType(option, false, cConfigOption::CFG_FILENAME);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return parseFilename(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

std::vector<std::string> cConfiguration::getAsFilenames(cConfigOption *option) const
{
    assertType(option, false, cConfigOption::CFG_FILENAMES);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return parseFilenames(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

std::string cConfiguration::getAsPath(cConfigOption *option) const
{
    assertType(option, false, cConfigOption::CFG_PATH);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return adjustPath(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

//----

const char *cConfiguration::getAsCustom(const char *objectFullPath, cConfigOption *option, const char *fallbackValue) const
{
    assertType(option, true, cConfigOption::CFG_CUSTOM);
    TRY(return fallback(getPerObjectConfigValue(objectFullPath, option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

bool cConfiguration::getAsBool(const char *objectFullPath, cConfigOption *option, bool fallbackValue) const
{
    assertType(option, true, cConfigOption::CFG_BOOL);
    TRY(return parseBool(getPerObjectConfigValue(objectFullPath, option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

long cConfiguration::getAsInt(const char *objectFullPath, cConfigOption *option, long fallbackValue) const
{
    assertType(option, true, cConfigOption::CFG_INT);
    TRY(return parseLong(getPerObjectConfigValue(objectFullPath, option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

double cConfiguration::getAsDouble(const char *objectFullPath, cConfigOption *option, double fallbackValue) const
{
    assertType(option, true, cConfigOption::CFG_DOUBLE);
    TRY(return parseDouble(getPerObjectConfigValue(objectFullPath, option->getName()), option->getUnit(), substituteVariables(option->getDefaultValue()), fallbackValue));
}

std::string cConfiguration::getAsString(const char *objectFullPath, cConfigOption *option, const char *fallbackValue) const
{
    assertType(option, true, cConfigOption::CFG_STRING);
    TRY(return parseString(getPerObjectConfigValue(objectFullPath, option->getName()), substituteVariables(option->getDefaultValue()), fallbackValue));
}

std::string cConfiguration::getAsFilename(const char *objectFullPath, cConfigOption *option) const
{
    assertType(option, true, cConfigOption::CFG_FILENAME);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return parseFilename(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

std::vector<std::string> cConfiguration::getAsFilenames(const char *objectFullPath, cConfigOption *option) const
{
    assertType(option, true, cConfigOption::CFG_FILENAMES);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return parseFilenames(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

std::string cConfiguration::getAsPath(const char *objectFullPath, cConfigOption *option) const
{
    assertType(option, true, cConfigOption::CFG_FILENAMES);
    const KeyValue& keyvalue = getConfigEntry(option->getName());
    TRY(return adjustPath(keyvalue.getValue(), keyvalue.getBaseDirectory(), substituteVariables(option->getDefaultValue())));
}

}  // namespace omnetpp

