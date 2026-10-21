package com.orangecrave.admin.audit.util;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * pHash 性能基准：给优化前后提供可复现的对比数字（不是功能测试）。
 *
 * <p>跑法：{@code mvn -pl orangecrave-audit test -Dtest=PHashPerfTest}。
 * 数字会打到 stdout，优化前后各跑一次即可对比。断言只做「不退化到离谱」
 * 的兜底（防止有人把实现改成 O(n^4) 还全绿），不锁死具体机器上的数值。 */
class PHashPerfTest {

    private static final int WARMUP = 500;
    private static final int ROUNDS = 2000;
    private static final int BLACKLIST_SIZE = 10_000;

    /** 造一张有结构的图（纯色图会让 AC 全 0，测不出真实开销）。 */
    private BufferedImage sample(int size, int seed) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int value = Math.floorMod(x * 7 + y * 13 + rnd.nextInt(0, 40) + seed, 256);
                image.getRaster().setSample(x, y, 0, value);
            }
        }
        return image;
    }

    @Test
    void benchmarkCompute() {
        BufferedImage image = sample(600, 0);
        for (int i = 0; i < WARMUP; i++) {
            PHashUtil.compute(image);
        }
        long start = System.nanoTime();
        long sink = 0;
        for (int i = 0; i < ROUNDS; i++) {
            sink ^= PHashUtil.compute(image);
        }
        long elapsed = System.nanoTime() - start;
        double nsPerOp = elapsed / (double) ROUNDS;
        double opsPerSec = 1_000_000_000.0 / nsPerOp;
        System.out.printf("[pHash] compute  : %.0f ns/op  (%.0f ops/s)  rounds=%d sink=%d%n",
                nsPerOp, opsPerSec, ROUNDS, sink);
        // 兜底：单张图算哈希不应慢于 20ms（正常实现应在 1ms 量级）
        if (nsPerOp > 20_000_000) {
            throw new AssertionError("pHash.compute 过慢: " + nsPerOp + " ns/op");
        }
    }

    @Test
    void benchmarkBlacklistScan() {
        long[] blacklist = new long[BLACKLIST_SIZE];
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < BLACKLIST_SIZE; i++) {
            blacklist[i] = rnd.nextLong();
        }
        long target = PHashUtil.compute(sample(600, 1));
        for (int i = 0; i < 20; i++) {
            scan(blacklist, target);
        }
        long start = System.nanoTime();
        int hits = 0;
        for (int i = 0; i < 100; i++) {
            hits += scan(blacklist, target);
        }
        long elapsed = System.nanoTime() - start;
        double msPerScan = elapsed / 100.0 / 1_000_000.0;
        System.out.printf("[pHash] blacklist: %.3f ms/scan  (size=%d, %d 次汉明距离) hits=%d%n",
                msPerScan, BLACKLIST_SIZE, BLACKLIST_SIZE, hits);
    }

    /** 模拟 AuditService 的做法：逐个算汉明距离，取最近的一个。 */
    private int scan(long[] blacklist, long target) {
        int min = Integer.MAX_VALUE;
        for (long hash : blacklist) {
            int distance = PHashUtil.hamming(target, hash);
            if (distance < min) {
                min = distance;
            }
        }
        return min;
    }
}
