package com.orangecrave.admin.audit.util;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * pHash 单元测试，05 篇 §2.2。：确定性、亮度不变的稳健性、结构不同则距离大。  */
class PHashUtilTest {

    private static final int SIZE = 64;

    /**
     * 确定性、非退化的图案：base = (x*kx + y*ky) mod 200，再整体加 brightness。
     * 加常数只改 DC，AC 不变——用于验证 pHash 对亮度变化的稳健性。
     */
    private BufferedImage pattern(int kx, int ky, int brightness) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int base = Math.floorMod(x * kx + y * ky, 200);
                int value = Math.min(255, base + brightness);
                image.getRaster().setSample(x, y, 0, value);
            }
        }
        return image;
    }

    @Test
    void deterministic() {
        long a = PHashUtil.compute(pattern(7, 13, 0));
        long b = PHashUtil.compute(pattern(7, 13, 0));
        assertThat(PHashUtil.hamming(a, b)).isZero();
    }

    @Test
    void brightnessChangeDoesNotAffectHash() {
        long a = PHashUtil.compute(pattern(7, 13, 0));
        long b = PHashUtil.compute(pattern(7, 13, 10));
        assertThat(PHashUtil.hamming(a, b)).isLessThanOrEqualTo(4);
    }

    @Test
    void differentStructureIsFar() {
        long a = PHashUtil.compute(pattern(7, 13, 0));
        long c = PHashUtil.compute(pattern(13, 7, 0));
        assertThat(PHashUtil.hamming(a, c)).isGreaterThan(8);
    }
}
