//==========================================================================
//   CHASHER.H  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2008 Andras Varga
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CHASHER_H
#define __CHASHER_H

#include <string.h>
#include <stdlib.h>
#include "simkerneldefs.h"
#include "cobject.h"
#include "cexception.h"
#include "platdep/intxtypes.h"

NAMESPACE_BEGIN


/**
 * Utility class to calculate the "fingerprint" of a simulation.
 *
 * We are trying to achieve that the same simulation gives the same fingerprint
 * on a 32-bit machine and on a 64-bit machine. Longs can be either 32-bit or
 * 64-bit, so we always convert them to 64 bits. We do not try to convert
 * endianness, it would be too costly.
 */
class SIM_API cHasher : noncopyable
{
  private:
    uint32_t value;

    void merge(uint32_t x) {
        // rotate value left by one bit, and xor with new data
        uint32_t carry = (value & 0x80000000U) >> 31;
        value = ((value<<1)|carry) ^ x;
    }

    void merge2(uint64_t x) {
        merge((uint32_t)x);
        merge((uint32_t)(x>>32));
    }

  public:
    /**
     * Constructor.
     */
    cHasher() {ASSERT(sizeof(uint32_t)==4); ASSERT(sizeof(double)==8); value = 0;}

    /** @name Updating the hash */
    //@{
    void reset() {value = 0;}
    void add(const char *p, size_t length);
    void add(char d)           {merge((uint32_t)d);}
    void add(short d)          {merge((uint32_t)d);}
    void add(int d)            {merge((uint32_t)d);}
    void add(long d)           {int64_t tmp=d; merge2((uint64_t)tmp);}
    void add(opp_long_long d)   {merge2((uint64_t)d);}
    void add(unsigned char d)  {merge((uint32_t)d);}
    void add(unsigned short d) {merge((uint32_t)d);}
    void add(unsigned int d)   {merge((uint32_t)d);}
    void add(unsigned long d)  {uint64_t tmp=d; merge2(tmp);}
    void add(opp_unsigned_long_long d)  {merge2(d);}
    // note: safe(r) type punning, see http://cocoawithlove.decenturl.com/type-punning
    void add(double d)         {union _ {double d; uint64_t i;}; merge2(((union _ *)&d)->i);}
    void add(const char *s)    {if (s) add(s, strlen(s)+1); else add(0);}
    //@}

    /** @name Obtaining the result */
    //@{
    /**
     * Returns the hash value.
     */
    uint32_t getHash() const {return value;}

    /**
     * Converts the given string to a numeric fingerprint value. The object is
     * not changed. Throws an error if the string does not contain a valid
     * fingerprint.
     */
    uint32_t parse(const char *fingerprint) const;

    /**
     * Parses the given fingerprint string, and compares it to the stored hash.
     */
    bool equals(const char *fingerprint) const;

    /**
     * Returns the textual representation (hex string) of the stored hash.
     */
    std::string str() const;
    //@}
};

NAMESPACE_END

#endif


