package com.sora.watcher.processor;


import com.alibaba.fastjson.parser.ParserConfig;
import com.sora.watcher.config.DataConfigProfile;
import com.sora.watcher.config.ProcessWatchData;
import com.sora.watcher.config.RedisConfig;
import com.sora.watcher.listerner.BinlogThreadStarter;
import com.sora.watcher.listerner.ISoraDataListener;
import com.sora.watcher.listerner.ISoraErrorNotify;
import com.sora.watcher.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class BinLogBeanProcessor implements SmartInitializingSingleton {
    private ApplicationContext context;

    @Autowired
    private DataConfigProfile profile;

    @Autowired
    private RedisConfig redisConfig;

    @Value("${sora.start:true}")
    private boolean start;


    public BinLogBeanProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<ProcessWatchData> beanIds = profile.getBeanIds();
        log.info("watch start:{}",start);
        if (start) {
            ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
            Map<String, ISoraDataListener> beans = context.getBeansOfType(ISoraDataListener.class);  //获取监控类
            Map<String, ISoraErrorNotify> notifyBeans = context.getBeansOfType(ISoraErrorNotify.class);  //获取通知类

            Map<String, ISoraDataListener> activeBeans = new HashMap<>();

            for (Map.Entry<String, ISoraDataListener> entry : beans.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                Boolean has = beanIds.stream().filter(x->x.getClassNmae().equals(key)).count()>0;
                if (has) {
                    activeBeans.put(entry.getKey(), entry.getValue());
                }
            }
            RedisUtils.init(redisConfig);
            List<SoraListenerData> soraList = new ArrayList<>();
            activeBeans.forEach((k,v)->{
                SoraListenerData sora = new SoraListenerData(v,getWatchConfig(beanIds,k.toLowerCase(Locale.ROOT)));
                soraList.add(sora);
            });
            Map<String, List<SoraListenerData>> listeners = soraList.stream()
                    .collect(Collectors.groupingBy(SoraListenerData::getName));
            listeners.forEach((k, v) -> new BinlogThreadStarter().start(profile.getConfig(k), v,notifyBeans));
        }
    }

    private ProcessWatchData getWatchConfig(List<ProcessWatchData>list,String className){
        return list.stream().filter(x->x.getClassNmae().equals(className)).findFirst().get();
    }
}
