package com.sora.watcher.processor;

import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import lombok.Data;


@Data
public class MySqlTable {
    private long id;
    private String database;
    private String table;

    public MySqlTable(TableMapEventData data) {
        this.id = data.getTableId();
        this.database = data.getDatabase();
        this.table = data.getTable();
    }
}
