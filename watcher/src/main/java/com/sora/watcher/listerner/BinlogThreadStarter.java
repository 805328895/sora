package com.sora.watcher.listerner;


import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.sora.watcher.config.DataConfig;
import com.sora.watcher.config.SoraModel;
import com.sora.watcher.processor.BinlogDataDispatcher;
import com.sora.watcher.processor.DataListenerContainer;
import com.sora.watcher.processor.SoraListenerData;
import com.sora.watcher.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class BinlogThreadStarter {
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Map<String, Connection> connectionPool = new HashMap<>();
    private Connection getConnection(DataConfig config) throws SQLException {
        String key = config.getHost() + ":" + config.getPort();
        Connection connection = connectionPool.get(key);

        if (connection == null) {
            String url =  "jdbc:mysql://" + key + "/INFORMATION_SCHEMA?useUnicode=true&characterEncoding=UTF-8&useSSL=false";

            connection = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            connectionPool.put(key, connection);
        }

        return connection;
    }

    private void releaseConnection() {
        for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
            try {
                entry.getValue().close();
            } catch (SQLException e) {

            }
        }

        connectionPool.clear();
    }

    public void start(DataConfig config, List<SoraListenerData> listeners,Map<String, ISoraErrorNotify> notifyBeans) {
        Map<String, List<SoraListenerData>> map = listeners.stream()
                .collect(Collectors.groupingBy(l -> l.getDataBase() + ":" + l.getTable()));
        BinlogDataDispatcher logListener = new BinlogDataDispatcher();

        map.forEach((k, v) -> {
            String[] arr = k.split(":");
            String[] columns = getColumns(config, arr[0], arr[1]);

            List<DataListenerContainer> containers = v.stream()
                    .map(l -> new DataListenerContainer(l.getEntityClass(), l.getListener(), columns, config.getTimeOffset()))
                    .collect(Collectors.toList());

            logListener.addListener(arr[0], arr[1], containers,config,notifyBeans);
        });


        new Thread(() -> {
            BinaryLogClient client = new BinaryLogClient(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
            if(config.getSequel() != null && config.getSequel() == true){
                //断点续传
                SoraModel soraModel =RedisUtils.get(config.getName());
                if(soraModel !=null){
                    client.setBinlogPosition(soraModel.getNetxPosition());
                    client.setBinlogFilename(soraModel.getFileName());
                }else {
                    client.setBinlogPosition(config.getPosition());                 //位移量
                    client.setBinlogFilename(config.getFileName());                 //起始文件名
                }

            }else {
                client.setBinlogPosition(config.getPosition());                 //位移量
                client.setBinlogFilename(config.getFileName());                 //起始文件名
            }
            client.registerEventListener(logListener);
            logListener.addClient(client);
            try {
                client.connect();
            } catch (IOException e) {
                log.error("{}:{}监听器错误", config.getHost(), config.getPort(), e);
            }
        }).start();

        releaseConnection();
    }

    private String[] getColumns(DataConfig host, String db, String table) {
        try {
            Connection connection = getConnection(host);
            Statement statement = connection.createStatement();

            String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='"
                    + db + "' and TABLE_NAME='" + table + "' order by ORDINAL_POSITION asc;";
            log.info("sql:"+sql);
            ResultSet resultSet = statement.executeQuery(sql);
            List<String> buf = new ArrayList<>();

            while (resultSet.next()) {
                buf.add(resultSet.getString(1));
            }

            return buf.toArray(new String[0]);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
