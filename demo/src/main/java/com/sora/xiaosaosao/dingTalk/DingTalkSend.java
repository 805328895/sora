package com.sora.xiaosaosao.dingTalk;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DingTalkSend {

    @Value("${ding.talk.secret}")
    private String secret;
    @Value("${ding.talk.token}")
    private String token;


    /**
     * 发送文本
     * @param content 内容
     * @param at AT的人
     */
    public void sendTextMsg(String content, List<String> at,Boolean isAtAll) {
        try {
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");

            DingTalkMsg<DingTalkTextMsg> talkMsg = new DingTalkMsg<>();
            DingTalkTextMsg msg = new DingTalkTextMsg();
            msg.setContent(content);
            talkMsg.setMsgtype(DingTalkEnum.TEXT.getCode());
            talkMsg.setText(msg);


            DingTalkAt dingTalkAt = new DingTalkAt();
            if (isAtAll) {
                dingTalkAt.setIsAtAll(true);
            } else {
                dingTalkAt.setIsAtAll(false);
                dingTalkAt.setAtMobiles(at);
            }

            talkMsg.setAt(dingTalkAt);
            String url = token + "&timestamp=" + timestamp + "&sign=" + sign;
            post(url, null, JSON.toJSONString(talkMsg));
        }catch (Exception e){
            log.error("dingTalk send error",e);
        }
    }



    public void sendMarkDown(DingTalkMarkDownMsg msg, List<String> at, Boolean isAtAll){
        try{
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");

            DingTalkMsg<DingTalkMarkDownMsg> talkMsg = new DingTalkMsg<>();
            talkMsg.setMsgtype(DingTalkEnum.MARKDOWN.getCode());
            talkMsg.setMarkdown(msg);
            DingTalkAt dingTalkAt = new DingTalkAt();
            if (isAtAll) {
                dingTalkAt.setIsAtAll(true);
            } else {
                dingTalkAt.setIsAtAll(false);
                dingTalkAt.setAtMobiles(at);
            }
            talkMsg.setAt(dingTalkAt);
            String url = token + "&timestamp=" + timestamp + "&sign=" + sign;
            String e = post(url, null, JSON.toJSONString(talkMsg));
            log.info(e);
        } catch (Exception e){
            log.error("dingTalk send error",e);
        }
    }










    public String post(String url, Map<String,String> header, String body) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), body);

        Request.Builder requestBuilder  = new Request.Builder().url(url).post(requestBody);
        if(header!=null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                requestBuilder = requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = requestBuilder.build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Response _response = client.newCall(request).execute();
        String b = _response.body().string();
        return b;
    }
}
