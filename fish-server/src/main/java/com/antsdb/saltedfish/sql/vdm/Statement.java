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
package com.antsdb.saltedfish.sql.vdm;

import java.util.Collections;
import java.util.List;

import com.antsdb.saltedfish.sql.meta.TableMeta;

/**
 * an statement object resembles a statement in SQL language
 * 
 * @author xguo
 *
 */
public abstract class Statement extends Instruction {
    public abstract Object run(VdmContext ctx, Parameters params);

    /** get the list of tables this statement depends. runtime needs this information to regenerate the statement */  
    List<TableMeta> getDependents() {
        return Collections.emptyList();
    }
    
    @Override
    public Object run(VdmContext ctx, Parameters params, long pMaster) {
        return run(ctx, params);
    }
}
