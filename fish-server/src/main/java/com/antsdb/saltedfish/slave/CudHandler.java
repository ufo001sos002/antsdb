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
package com.antsdb.saltedfish.slave;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.antsdb.saltedfish.nosql.HColumnRow;
import com.antsdb.saltedfish.nosql.Row;
import com.antsdb.saltedfish.nosql.SysMetaRow;

/**
 * 
 * @author *-xguo0<@
 */
class CudHandler {
    List<ReplicatorColumnMeta> columns = new ArrayList<>();
    List<String> key = new ArrayList<>();
    PreparedStatement insert;
    int[] insertParams;
    PreparedStatement update;
    int[] updateParams;
    PreparedStatement delete;
    List<ReplicatorColumnMeta> deleteParams;
    PreparedStatement replace;
    List<ReplicatorColumnMeta> replaceParams;
    
    public void prepare(Connection conn, SysMetaRow table, List<HColumnRow> hcolumns) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        
        // fetch columns
        
        loadColumnMeta(meta, table, hcolumns);
        if (this.columns.size() == 0) {
            throw new IllegalArgumentException("table not found : " + table.getTableName());
        }
        
        // fetch primary key
        
        try (ResultSet rs = meta.getPrimaryKeys(table.getNamespace(), null, table.getTableName())) {
            while (rs.next()) {
                String columnName = rs.getString(4);
                this.key.add(columnName);
            }
        }
        
        // build replace
        
        String sql = String.format("REPLACE INTO `%s`.`%s` VALUES (%s)", 
                table.getNamespace(), 
                table.getTableName(),
                repeat(this.columns.size()));
        this.replace = conn.prepareStatement(sql);
        this.replaceParams = this.columns;
        
        // build delete
        
        StringBuilder buf = new StringBuilder();
        if (this.key.size() != 0) {
            for (int i=0; i<this.key.size(); i++) {
                if (i > 0) {
                    buf.append(" AND ");
                }
                buf.append(this.key.get(i));
                buf.append("=?");
            }
            this.deleteParams = buildParameters(this.key);
        }
        else {
            for (int i=0; i<this.columns.size(); i++) {
                if (i > 0) {
                    buf.append(" AND ");
                }
                buf.append(this.columns.get(i));
                buf.append("=?");
            }
            this.deleteParams = this.columns;
        }
        sql = String.format(
                "DELETE FROM `%s`.`%s` WHERE %s", 
                table.getNamespace(), 
                table.getTableName(), 
                buf.toString());
        this.delete = conn.prepareStatement(sql);
    }
    
    private void loadColumnMeta(DatabaseMetaData meta, SysMetaRow table, List<HColumnRow> hcolumns) 
    throws SQLException {
        this.columns.clear();
        try (ResultSet rs = meta.getColumns(table.getNamespace(), null, table.getTableName(), "%")) {
            while (rs.next()) {
                ReplicatorColumnMeta columnMeta  = new ReplicatorColumnMeta();
                columnMeta.dataType = rs.getInt(5);
                columnMeta.typeName = rs.getString(6);
                columnMeta.columnName = rs.getString(4);
                columnMeta.nullable = rs.getInt(11);
                columnMeta.defaultValue = rs.getString(13);
                for (HColumnRow j:hcolumns) {
                    if (columnMeta.columnName.equals(j.getColumnName())) {
                        columnMeta.hcolumnPos = j.getColumnPos();
                    }
                }
                this.columns.add(columnMeta);
            }
        }
    }

    private List<ReplicatorColumnMeta> buildParameters(List<String> columnNames) {
        List<ReplicatorColumnMeta> result = new ArrayList<>(columnNames.size());
        for (int i=0; i<columnNames.size(); i++) {
            String ii = columnNames.get(i); 
            for (ReplicatorColumnMeta j:this.columns) {
                if (ii.equals(j.columnName)) {
                    result.add(j); 
                }
            }
        }
        return result;
    }

    private String repeat(int n) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<n; i++) {
            buf.append("?,");
        }
        buf.deleteCharAt(buf.length()-1);
        return buf.toString();
    }

    public void insert(Row row) throws SQLException {
        for (int i=0; i<this.replaceParams.size(); i++) {
            Object value = getValue(row, replaceParams.get(i)); 
            row.get(this.replaceParams.get(i).hcolumnPos);
            this.replace.setObject(i+1, value);
        }
        int result = this.replace.executeUpdate();
        if (result < 1) {
            throw new IllegalArgumentException();
        }
    }

    public void update(Row row) throws SQLException {
        insert(row);
    }
    
    public void delete(Row row) throws SQLException {
        for (int i=0; i<this.deleteParams.size(); i++) {
            Object value = row.get(this.deleteParams.get(i).hcolumnPos);
            this.delete.setObject(i+1, value);
        }
        int result = this.delete.executeUpdate();
        if (result < 1) {
            throw new IllegalArgumentException();
        }
    }
    
    private Object getValue(Row row, ReplicatorColumnMeta meta) {
        Object result = row.get(meta.hcolumnPos);
        // special logic to handle mysql 0000-00-00 00:00
        if (result==null && meta.dataType==Types.TIMESTAMP && meta.nullable==DatabaseMetaData.columnNoNulls) {
            if ("0000-00-00 00:00:00".equals(meta.defaultValue)) {
                result = "0000-00-00 00:00:00";
            }
        }
        if (result==null && meta.dataType==Types.DATE && meta.nullable==DatabaseMetaData.columnNoNulls) {
            if ("0000-00-00".equals(meta.defaultValue)) {
                result = "0000-00-00";
            }
        }
        if (result instanceof Date && ((Date)result).getTime() == Long.MIN_VALUE) {
            result = "0000-00-00";
        }
        if (result instanceof Timestamp && ((Timestamp)result).getTime() == Long.MIN_VALUE) {
            result = "0000-00-00 00:00:00";
        }
        return result;
    }

}
