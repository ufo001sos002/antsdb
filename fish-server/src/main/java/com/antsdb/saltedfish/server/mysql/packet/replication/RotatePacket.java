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
package com.antsdb.saltedfish.server.mysql.packet.replication;

import com.antsdb.saltedfish.server.mysql.MysqlClientHandler;
import com.antsdb.saltedfish.server.mysql.util.BufferUtils;

import io.netty.buffer.ByteBuf;

public class RotatePacket extends ReplicationPacket {
    public long position;
    public String binlog;
    
    public RotatePacket(int type, long length, long pos) {
        super(type, length, pos);
    }

    @Override
    public void read(MysqlClientHandler handler, ByteBuf in) {
    	position = BufferUtils.readLongLong(in);
    	// string length is event_length - event_header_length - post_header_length
    	binlog = BufferUtils.readStringFixLength(in, (int)eventlength-27);
    }

}
