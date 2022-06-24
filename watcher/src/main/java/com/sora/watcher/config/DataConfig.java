package com.sora.watcher.config;

import lombok.Data;

import java.util.List;

@Data
public class DataConfig {
    private String name;
    private List<WatchDataBase> database;
    private String host;
    private int port;
    private String username;
    private String password;
    private String fileName;
    private Long position;
    private Long nexPosition;
    private long timeOffset;
    private Boolean sequel;
}
