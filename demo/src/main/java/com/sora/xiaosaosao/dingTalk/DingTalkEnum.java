package com.sora.xiaosaosao.dingTalk;

public enum  DingTalkEnum {
    TEXT("text","文本信息"),
    MARKDOWN("markdown","markdown"),

    ;

    private String code;
    private String desc;
    DingTalkEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode(){
        return code;
    }


}
