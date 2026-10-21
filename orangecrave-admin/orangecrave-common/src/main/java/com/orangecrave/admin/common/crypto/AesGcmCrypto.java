package com.orangecrave.admin.common.crypto;

import com.orangecrave.admin.common.exception.BizException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感字段加密（01 篇 §2：id_card_no 加密存储）。AES-256/GCM，随机 IV，
 * 密文格式 Base64(iv[12] + ciphertext + tag[16])。
 *
 * <p>MyBatis 的 TypeHandler 由框架自己实例化，拿不到 Spring 容器，所以这里把实例挂成静态的。
 */
@Component
public class AesGcmCrypto {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static volatile AesGcmCrypto instance;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCrypto(CryptoProperties properties) {
        byte[] raw = decodeKey(properties.getAesKey());
        this.key = new SecretKeySpec(raw, "AES");
        instance = this;
    }

    /** 供 TypeHandler 使用；上下文未启动或未配密钥时直接失败，绝不退回明文。 */
    public static AesGcmCrypto current() {
        AesGcmCrypto crypto = instance;
        if (crypto == null) {
            throw new BizException("加密组件未初始化，请检查 orangecrave.crypto.aes-key 配置");
        }
        return crypto;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] body = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + body.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(body, 0, out, iv.length, body.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new BizException("敏感信息加密失败");
        }
    }

    /**
     * 解密。历史数据可能是明文（本仓库没有密文存量），密文校验不过时原样返回，
     * 读取路径不因单条脏数据整块报错。
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            if (all.length <= IV_LENGTH) {
                return stored;
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (RuntimeException | GeneralSecurityException e) {
            // 密文被篡改时 JDK 可能抛 AEADBadTagException，也可能抛 ShortBufferException 这类运行时异常
            return stored;
        }
    }

    private static byte[] decodeKey(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("必须配置 orangecrave.crypto.aes-key，禁止明文存储身份证号");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("orangecrave.crypto.aes-key 必须是 Base64 编码的 16/24/32 字节密钥", e);
        }
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalStateException("orangecrave.crypto.aes-key 长度必须是 16/24/32 字节，当前 " + raw.length);
        }
        return raw;
    }
}
