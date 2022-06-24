package com.sora.xiaosaosao.dingTalk;

import lombok.Data;

import java.util.List;

@Data
public class DingTalkAt {
    private List<String> atMobiles;
    private Boolean isAtAll;
}
