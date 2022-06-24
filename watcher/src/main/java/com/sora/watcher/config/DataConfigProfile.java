package com.sora.watcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Data
@Configuration
@ConfigurationProperties(prefix = "sora.mysql")
public class DataConfigProfile {
    private List<DataConfig> hosts;

    private Optional<DataConfig> getByName(String name) {
        return hosts.stream().filter(v -> name.equals(v.getName()))
                .findAny();
    }

    public DataConfig getConfig(String name) {
        return getByName(name).orElseThrow(() -> new RuntimeException("未配置名为 "+name+" 的 sora 连接信息"));
    }



    public List<ProcessWatchData> getBeanIds(){
        List<ProcessWatchData> ret = new ArrayList<>();
        if(hosts !=null){
            hosts.forEach(x->{
                if(x.getDatabase() !=null){
                    x.getDatabase().forEach(e->{
                        if(e.getWatch() !=null){
                            Iterator<Map.Entry<String, String>> it = e.getWatch().entrySet().iterator();
                            while(it.hasNext()){
                                Map.Entry<String, String> entry = it.next();
                                String className = entry.getKey().toLowerCase(Locale.ROOT);
                                String table = entry.getValue();
                                if(checkHas(ret,className)){
                                    throw new RuntimeException(String.format("className:%s 重复配置",className));
                                }
                                ProcessWatchData d = new ProcessWatchData();
                                d.setDataBase(e.getName());
                                d.setName(x.getName());
                                d.setTableName(table);
                                d.setClassNmae(className);
                                ret.add(d);
                            }
                        }
                    });
                }
            });
        }
        return ret;
    }

    private Boolean checkHas(List<ProcessWatchData> list,String key){
        return list.stream().filter(x->x.getClassNmae().equals(key)).count()>0;
    }
}
