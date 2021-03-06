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

import java.util.ArrayList;
import java.util.List;

import com.antsdb.saltedfish.sql.meta.ColumnMeta;

class KeyGenerator {
    PrimaryKeyGenerator gen;

    public KeyGenerator(List<ColumnMeta> columns) {
        super();
        if (columns.size() == 1) {
            this.gen = new SingleColumnKeyGenerator(columns.get(0));
        }
        else {
            this.gen = new MultiColumnKeyGenerator(columns);
        }
    }

    byte[] generate(VdmContext ctx, Parameters params, long pRecord, List<Operator> exprs) {
        byte[] key;
        if (gen instanceof SingleColumnKeyGenerator) {
            SingleColumnKeyGenerator generator = (SingleColumnKeyGenerator)this.gen;
            Object value = Util.eval(ctx, exprs.get(0), params, pRecord);
            key = generator.generate(value);
        }
        else {
            MultiColumnKeyGenerator generator = (MultiColumnKeyGenerator)this.gen;
            List<Object> values = new ArrayList<>(exprs.size());
            for (Operator i:exprs) {
                values.add(Util.eval(ctx, i, params, pRecord));
            }
            key = generator.generate(values);
        }
        return key;
    }
}
