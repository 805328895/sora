package com.sora.xiaosaosao.dingTalk;

import lombok.Data;

@Data
public class DingTalkMsg<T> {
    private String msgtype;
    private T text;
    private T markdown;
    private DingTalkAt at;
}
