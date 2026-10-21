package com.orangecrave.admin.common.crypto;

import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.util.MaskUtil;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 敏感字段加密与脱敏（01 篇 §2 加密存储、02 篇 §2.3 出参脱敏）。 */
class AesGcmCryptoTest {

    private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private AesGcmCrypto crypto(String key) {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey(key);
        return new AesGcmCrypto(properties);
    }

    @Test
    void roundTripKeepsThePlaintextAndNeverStoresIt() {
        AesGcmCrypto c = crypto(KEY);
        String stored = c.encrypt("110101199003071234");
        assertThat(stored).isNotEqualTo("110101199003071234").doesNotContain("19900307");
        assertThat(c.decrypt(stored)).isEqualTo("110101199003071234");
    }

    @Test
    void randomIvMakesTheSameValueEncryptDifferently() {
        AesGcmCrypto c = crypto(KEY);
        assertThat(c.encrypt("110101199003071234")).isNotEqualTo(c.encrypt("110101199003071234"));
    }

    @Test
    void tamperedOrForeignCipherTextIsReturnedAsIsInsteadOfCrashingAList() {
        AesGcmCrypto c = crypto(KEY);
        String stored = c.encrypt("110101199003071234");
        String tampered = stored.substring(0, stored.length() - 4) + (stored.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThat(c.decrypt(tampered)).isEqualTo(tampered);
        // 历史明文行不至于让后台报错
        assertThat(c.decrypt("110101199003071234")).isEqualTo("110101199003071234");
    }

    @Test
    void anotherKeyCannotReadTheValue() {
        String stored = crypto(KEY).encrypt("110101199003071234");
        String otherKey = Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());
        assertThat(crypto(otherKey).decrypt(stored)).isEqualTo(stored);
    }

    @Test
    void emptyOrNullPassThrough() {
        AesGcmCrypto c = crypto(KEY);
        assertThat(c.encrypt(null)).isNull();
        assertThat(c.encrypt("")).isEmpty();
        assertThat(c.decrypt(null)).isNull();
        assertThat(c.decrypt("")).isEmpty();
    }

    @Test
    void badKeyConfigurationFailsFastInsteadOfSilentlyStoringPlaintext() {
        assertThatThrownBy(() -> crypto(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("aes-key");
        assertThatThrownBy(() -> crypto("   ")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> crypto("not-base64!!!")).isInstanceOf(IllegalStateException.class);
        // 长度必须落在 AES 允许的 16/24/32 字节
        assertThatThrownBy(() -> crypto(Base64.getEncoder().encodeToString("short".getBytes())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("16/24/32");
    }

    @Test
    void maskedIdCardKeepsOnlyHeadAndTail() {
        assertThat(MaskUtil.idCard("110101199003071234")).isEqualTo("110***********1234");
        assertThat(MaskUtil.idCard("1234567")).isEqualTo("*******");
        assertThat(MaskUtil.idCard("ABC")).isEqualTo("***");
        assertThat(MaskUtil.idCard(null)).isNull();
        assertThat(MaskUtil.idCard("")).isEmpty();
    }
}
