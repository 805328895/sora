package com.sora.watcher.processor;


import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.sora.watcher.config.SoraModel;
import com.sora.watcher.listerner.ISoraDataListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;


@Slf4j
@Data
public class DataListenerContainer<T> {
    private Class<T> entityClass;
    private ISoraDataListener<T> listener;
    private String[] columnName;
    private SoraModel soraModel;


    private long timeOffset = 0;

    private static final ParserConfig snakeCase;
    static {
        snakeCase = new ParserConfig();
        snakeCase.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
    }


    public DataListenerContainer(Class<T> entityClass, ISoraDataListener<T> listener, String[] columnName, long timeOffset) {
        this.entityClass = entityClass;
        this.listener = listener;
        this.columnName = columnName;
        this.timeOffset = timeOffset;
    }


    public void invokeInsert(List<Serializable[]> data,BitSet column) {
        data.forEach(row-> {
            T targetEntity = toEntity(row, column);
            try {
                listener.onInsert(targetEntity);
            }catch (Exception e){
                log.error("insert error",e);
                Boolean ret = false;
                try {
                   ret = listener.saveErrorData(null, targetEntity, 1,e.getMessage());
                }catch (Exception ee){
                    log.error("save error err",ee);
                }
                if(!ret) {
                    throw e;
                }
            }
        });
    }

    public void invokeDelete(List<Serializable[]> data,BitSet column) {
        data.forEach(row->{
                    listener.onDelete(toEntity(row,column));
                });
    }

    public void invokeUpdate(List<Map.Entry<Serializable[], Serializable[]>> data,BitSet before,BitSet after ) {
        data.forEach(row -> {
            T from = toEntity(row.getKey(), before);
            T targetEntity = toEntity(row.getValue(), after);
            try {
                listener.onUpdate(from, targetEntity);
            } catch (Exception e) {
                log.error("update error",e);
                Boolean ret = false;
                try {
                    ret = listener.saveErrorData(from, targetEntity, 2,e.getMessage());
                } catch (Exception ee) {
                    log.error("save error err", ee);
                }
                if (!ret) {
                    throw e;
                }
            }
        });
    }

    private T toEntity(Serializable[] data, BitSet colum) {
        int[] c = colum.stream().toArray();
//        log.info(JSON.toJSONString(c));

        for (int i = 0; i < data.length; i++) {
            Serializable da = data[i];
            if(da instanceof Timestamp){

                data[i] = new Date(((Date)da).getTime());
            } else if (da instanceof Date) {
                data[i] = new Date(((Date)da).getTime() + timeOffset);
            }
        }

        Map<String, Object> b = new HashMap<>(data.length);
        for (int i = 0; i < data.length; i++) {
            b.put(columnName[c[i]], data[i]);
//            log.info("index:"+i+","+columnName[i]+":"+data[i]);
        }

        return TypeUtils.cast(b, entityClass, snakeCase);
    }
}
