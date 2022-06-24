package com.sora.watcher.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class SoraModel implements Serializable {
    private String dbName;
    private String fileName;
    private Long position;
    private Long netxPosition;
}
