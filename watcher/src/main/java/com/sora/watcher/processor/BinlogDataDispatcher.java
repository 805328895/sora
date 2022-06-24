package com.sora.watcher.processor;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.sora.watcher.config.DataConfig;
import com.sora.watcher.config.SoraModel;
import com.sora.watcher.listerner.ISoraErrorNotify;
import com.sora.watcher.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;

import java.io.Serializable;
import java.util.*;


@Slf4j
public class BinlogDataDispatcher implements BinaryLogClient.EventListener {
    private Map<Long, MySqlTable> tableNameMap = new HashMap<>();
    private Map<String, List<DataListenerContainer>> listenerMap = new HashMap<>();
    private DataConfig config;
    private BinaryLogClient client;
    private Boolean interrupt =false;
    private Map<String, ISoraErrorNotify> notifyMap;

    public void addListener(String database, String table, List<DataListenerContainer> listeners, DataConfig config,Map<String, ISoraErrorNotify> notifyMap) {
        String key = database + "." + table;
        this.listenerMap.put(key, listeners);
        this.config = config;
        this.notifyMap = notifyMap;
    }
    public void addClient(BinaryLogClient client){
        this.client =client;
    }


    @Override
    public void onEvent(Event event) {
        if(interrupt){
            try{
                client.disconnect();
            }catch (Exception e){
                log.error("disconnect error",e);
                errorNotify(e.toString());
            }
            return;
        }
        EventHeaderV4 header = event.getHeader();
        header.getNextPosition();


        String logId = String.format("%s-%s","sora", UUID.randomUUID().toString().replace("-",""));
        ThreadContext.put("logId",logId );
        EventType eventType = header.getEventType();

        config.setPosition(header.getPosition());
        config.setNexPosition(header.getNextPosition());
        try {
            if (eventType == EventType.ROTATE) {
                RotateEventData rotateEventData = event.getData();
                config.setFileName(rotateEventData.getBinlogFilename());
                Long position = rotateEventData.getBinlogPosition();
                config.setPosition(position);
                config.setNexPosition(position);
                setPosition();
            } else if (eventType == EventType.TABLE_MAP) {
                MySqlTable table = new MySqlTable(event.getData());
                String key = table.getDatabase() + "." + table.getTable();
//                log.info("----table:{}"+,key);
                if (this.listenerMap.containsKey(key))
                    tableNameMap.put(table.getId(), table);
            } else if (EventType.isUpdate(eventType)) {
                UpdateRowsEventData data = event.getData();
                if (!tableNameMap.containsKey(data.getTableId())) {
                    return;
                }
                dispatchEvent(data);
                setPosition();
            } else if (EventType.isWrite(eventType)) {
                WriteRowsEventData data = event.getData();
                if (!tableNameMap.containsKey(data.getTableId())) {
                    return;
                }
                dispatchEvent(data);
                setPosition();
            } else if (EventType.isDelete(eventType)) {
                DeleteRowsEventData data = event.getData();
                if (!tableNameMap.containsKey(data.getTableId())) {
                    return;
                }
                dispatchEvent(data);
                setPosition();
            }
        }catch (Exception e){
            interrupt = true; //中断
            log.error("",e);
            errorNotify(e.toString());
            try {
                client.disconnect();
            }catch (Exception e1){
                log.error("",e);
                errorNotify(e.toString());
            }
        }
    }


    private void errorNotify(String msg){
        if(notifyMap != null){
            notifyMap.forEach((k,v)->{
                v.error("msg:"+msg);
            });
        }
    }

    /**
     * 记录当前操作的
     * @param
     */
    private SoraModel dispatchPosition(){
        SoraModel soraModel = new SoraModel();
        soraModel.setPosition(config.getPosition());
        soraModel.setFileName(config.getFileName());
        soraModel.setNetxPosition(config.getNexPosition());
        soraModel.setDbName(config.getName());
        return soraModel;
    }

    private void setPosition(){
        SoraModel soraModel = dispatchPosition();
        if(config.getSequel() != null && config.getSequel() == true) {
            RedisUtils.set(soraModel);
        }
    }

    private void dispatchEvent(UpdateRowsEventData data) {
        MySqlTable table = tableNameMap.get(data.getTableId());
        String key = table.getDatabase() + "." + table.getTable();
        List<DataListenerContainer> containers = listenerMap.get(key);

        BitSet before = data.getIncludedColumnsBeforeUpdate();
        BitSet after = data.getIncludedColumns();

        List<Map.Entry<Serializable[], Serializable[]>> rows = data.getRows();
        containers.forEach(c -> c.invokeUpdate(rows,before,after));
    }

    private void dispatchEvent(DeleteRowsEventData data) {
        MySqlTable table = tableNameMap.get(data.getTableId());
        String key = table.getDatabase() + "." + table.getTable();

        List<DataListenerContainer> containers = listenerMap.get(key);
        List<Serializable[]> rows = data.getRows();
        containers.forEach(c -> c.invokeDelete(rows,data.getIncludedColumns()));
    }

    private void dispatchEvent(WriteRowsEventData data) {
        MySqlTable table = tableNameMap.get(data.getTableId());
        String key = table.getDatabase() + "." + table.getTable();

        List<DataListenerContainer> containers = listenerMap.get(key);
        List<Serializable[]> rows = data.getRows();
        containers.forEach(c -> c.invokeInsert(rows,data.getIncludedColumns()));
    }
}
