package com.sora.watcher.config;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class WatchDataBase {
    private String name;
    private HashMap<String,String> watch;
}
