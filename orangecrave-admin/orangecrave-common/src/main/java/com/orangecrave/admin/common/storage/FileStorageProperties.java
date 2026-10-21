package com.orangecrave.admin.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储参数。{@code type} 决定装配哪个 {@link FileStorage} 实现（local / s3），
 * 同一套业务代码据此在「本机目录」与「对象存储」间切换。
 */
@Data
@Component
@ConfigurationProperties(prefix = "orangecrave.storage")
public class FileStorageProperties {

    /** 存储类型：local 本机目录（默认），s3 走 S3 兼容对象存储。*/
    private String type = "local";

    /** 本地存储根目录（type=local 时生效）*/
    private String localDir = "./data/upload";

    /** 本地下发链接的前缀，key 直接拼接其后（type=local 时生效）*/
    private String urlPrefix = "/admin-api/file/download?key=";

    /** 对象存储参数（type=s3 时生效）。*/
    private S3 s3 = new S3();

    @Data
    public static class S3 {

        /** 服务端点：MinIO 形如 http://localhost:9000；留空则用官方 AWS 端点。*/
        private String endpoint;

        /** 区域，S3 协议必填（MinIO 随便填，如 us-east-1）。*/
        private String region = "us-east-1";

        /** 访问密钥。*/
        private String accessKey;

        /** 访问密钥口令。*/
        private String secretKey;

        /** 桶名。*/
        private String bucket = "orangecrave";

        /** 是否强制 path-style：MinIO / Ceph 需为 true，AWS 为 false。*/
        private boolean pathStyleAccess = true;

        /** 对外可访问的基础地址（CDN 或桶公开域名）；留空则回退为「端点 + 桶 + key」。*/
        private String publicUrlPrefix;
    }
}
