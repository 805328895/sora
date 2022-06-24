package com.sora.xiaosaosao.watcher;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sora.watcher.config.SoraModel;
import com.sora.watcher.listerner.ISoraDataListener;
import com.sora.xiaosaosao.model.TestUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestUserWatcher implements ISoraDataListener<TestUser> {



    @Override
    public void onUpdate(TestUser from, TestUser to) {
        log.info("id 为 {} 的条目数据变更 ", from.getId());
        log.info("before:" + JSON.toJSONString(from, SerializerFeature.WriteDateUseDateFormat));
        log.info("after:" + JSON.toJSONString(to, SerializerFeature.WriteDateUseDateFormat));
    }

    @Override
    public void onInsert(TestUser data ) {
        log.info("插入TestUser {} 的数据", JSON.toJSONString(data, SerializerFeature.WriteDateUseDateFormat));
    }

    @Override
    public void onDelete(TestUser data) {
        log.info("ID 为 {} 的数据被删除", data.getId());
    }

    @Override
    public void savePosition(String key, SoraModel soraModel) {

    }

    @Override
    public Boolean saveErrorData(TestUser from, TestUser to,Integer type,String msg) {
        return false;
    }
}
