package com.orangecrave.admin.common.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "orangecrave.crypto")
public class CryptoProperties {

    /** AES-GCM 密钥，Base64 编码的 16/24/32 字节；生产由环境变量注入，不落仓库。 */
    private String aesKey;
}
