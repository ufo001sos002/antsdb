/*-------------------------------------------------------------------------------------------------
 _______ __   _ _______ _______ ______  ______
 |_____| | \  |    |    |______ |     \ |_____]
 |     | |  \_|    |    ______| |_____/ |_____]

 Copyright (c) 2016, antsdb.com and/or its affiliates. All rights reserved. *-xguo0<@

 This program is free software: you can redistribute it and/or modify it under the terms of the
 GNU Affero General Public License, version 3, as published by the Free Software Foundation.

 You should have received a copy of the GNU Affero General Public License along with this program.
 If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
-------------------------------------------------------------------------------------------------*/
package com.antsdb.saltedfish.nosql;

import com.antsdb.saltedfish.cpp.KeyBytes;

/**
 * 
 * @author *-xguo0<@
 */
public abstract class ScanResult implements AutoCloseable {
    static ScanResult _empty = new EmptyResult();
    
    static class EmptyResult extends ScanResult {

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public boolean eof() {
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public long getVersion() {
            return 0;
        }

        @Override
        public long getKeyPointer() {
            return 0;
        }

        @Override
        public long getIndexRowKeyPointer() {
            return 0;
        }

        @Override
        public long getRowPointer() {
            return 0;
        }

        @Override
        public byte getMisc() {
            return 0;
        }

        @Override
        public void rewind() {
        }
    }
    
    /**
     * move to the next row in the result set
     *
     * @return false if the result is empty
     */
    public abstract boolean next();

    /**
     * if the result reached end of file
     * 
     * @return true if it is
     */
    public abstract boolean eof();
    
    /**
     * close the result and release resource
     * 
     */
    public abstract void close();

    /**
     * get the version of the row
     * 
     * @return
     */
    public abstract long getVersion();
    
    /**
     * get the pointer to the key
     */
    public abstract long getKeyPointer();
    
    /**
     * pointer to the row key. only applicable to index entry
     * @return
     */
    public abstract long getIndexRowKeyPointer();
    
    /**
     * pointer to the row, only applicable to row entry
     * @return
     */
    public abstract long getRowPointer();
    
    /**
     * get the misc byte 
     * 
     * @return
     */
    public abstract byte getMisc();
    
    public abstract void rewind();
    
    /**
     * index suffix. only applicable to non-unique index
     * 
     * @return
     */
    public long getIndexSuffix() {
        long pKey = getKeyPointer();
        if (pKey == 0) {
            return 0;
        }
        long suffix = KeyBytes.create(pKey).getSuffix();
        return suffix;
    }
    
    /**
     * get the row object
     * 
     * @return
     */
    public Row getRow() {
        long pRow = getRowPointer();
        if (pRow == 0) {
            return null;
        }
        return Row.fromMemoryPointer(pRow, getVersion());
    }
    
    public static ScanResult emptyResult() {
        return _empty;
    }
}
