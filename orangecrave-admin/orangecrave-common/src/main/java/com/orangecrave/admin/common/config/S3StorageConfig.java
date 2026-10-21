package com.orangecrave.admin.common.config;

import com.orangecrave.admin.common.storage.FileStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * S3 客户端装配。仅 {@code orangecrave.storage.type=s3} 时创建，且未用时根本不建连——
 * 默认 local 的下发不引入任何对象存储依赖。Bean 名即 {@code s3Client}，Spring 销毁阶段会自动 close。
 */
@Configuration
@ConditionalOnProperty(prefix = "orangecrave.storage", name = "type", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                // MinIO / Ceph 只认 path-style（endpoint/bucket/key），AWS 需要 virtual-host
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.isPathStyleAccess())
                        .build());

        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }
        if (StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        }
        return builder.build();
    }
}
