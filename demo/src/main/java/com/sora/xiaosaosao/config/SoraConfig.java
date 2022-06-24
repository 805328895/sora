package com.sora.xiaosaosao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sora.sync")
public class SoraConfig {
    private String startTime;
    private Integer thread = 10;
    private Integer pageSize = 500;
}
