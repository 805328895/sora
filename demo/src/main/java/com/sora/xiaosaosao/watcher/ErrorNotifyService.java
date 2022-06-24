package com.sora.xiaosaosao.watcher;

import com.sora.watcher.listerner.ISoraErrorNotify;
import com.sora.xiaosaosao.dingTalk.DingTalkSend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;

@Slf4j
@Component
public class ErrorNotifyService implements ISoraErrorNotify {
    @Resource
    private DingTalkSend dingTalkSend;
    @Override
    public void error(String msg) {
        dingTalkSend.sendTextMsg(msg,new ArrayList<>(),true);
    }
}
