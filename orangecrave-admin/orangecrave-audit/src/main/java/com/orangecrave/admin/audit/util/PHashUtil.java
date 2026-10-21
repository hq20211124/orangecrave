package com.orangecrave.admin.audit.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;

/**
 * 感知哈希（pHash，见 05 篇 §2.2）。DCT 法：缩放至 32×32 灰度 → 二维 DCT → 取左上 8×8 → 中位数阈值 → 64 位。
 * 相似图（缩放、压缩、轻微裁剪）汉明距离小。
 */
public final class PHashUtil {

    private static final int SIZE = 32;
    private static final int HASH_DIM = 8;

    /**
     * DCT 余弦系数表 COS[u][x] = cos(PI * (2x+1) * u / (2*SIZE))。
     * 尺寸固定为 SIZE，这张表只依赖下标、与输入图片无关，所以静态算一次即可。
     * 原实现每次调用要算 SIZE*SIZE*(行+列) = 65536 次 {@code Math.cos}，
     * 占了单张图耗时的 95% 以上；换成查表后这一步退化为数组访问。 */
    private static final double[][] COS = buildCosTable();

    private PHashUtil() {
    }

    private static double[][] buildCosTable() {
        double[][] table = new double[SIZE][SIZE];
        for (int u = 0; u < SIZE; u++) {
            for (int x = 0; x < SIZE; x++) {
                table[u][x] = Math.cos(Math.PI * (2 * x + 1) * u / (2.0 * SIZE));
            }
        }
        return table;
    }

    public static long compute(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            throw new IOException("无法解析图片");
        }
        return compute(image);
    }

    public static long compute(BufferedImage source) {
        BufferedImage gray = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(source, 0, 0, SIZE, SIZE, null);
        g.dispose();

        double[][] pixels = grayscale(gray);
        // 只取左上 HASH_DIM×HASH_DIM，所以行/列 DCT 两头都只算前 8 个系数。
        // 可分离 DCT 下这与算完整的再截断数学上等价，但少算约 6/7 的乘加。
        double[][] rows = new double[SIZE][HASH_DIM];
        for (int y = 0; y < SIZE; y++) {
            rows[y] = dct1d(pixels[y], HASH_DIM);
        }
        double[][] dct = new double[HASH_DIM][HASH_DIM];
        double[] column = new double[SIZE];
        for (int u = 0; u < HASH_DIM; u++) {
            for (int y = 0; y < SIZE; y++) {
                column[y] = rows[y][u];
            }
            double[] transformed = dct1d(column, HASH_DIM);
            for (int v = 0; v < HASH_DIM; v++) {
                dct[v][u] = transformed[v];
            }
        }

        // 取左上 8×8，跳过 DC 系数 (0,0)：DC 只反映整体亮度，会让哈希对亮度变化过于敏感
        double[] block = new double[HASH_DIM * HASH_DIM - 1];
        int[] positions = new int[HASH_DIM * HASH_DIM - 1];
        int k = 0;
        for (int y = 0; y < HASH_DIM; y++) {
            for (int x = 0; x < HASH_DIM; x++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                block[k] = dct[y][x];
                positions[k] = y * HASH_DIM + x;
                k++;
            }
        }
        double median = median(block);

        long hash = 0L;
        for (int i = 0; i < block.length; i++) {
            if (block[i] > median) {
                hash |= (1L << positions[i]);
            }
        }
        return hash;
    }

    public static int hamming(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    /**
     * 直接读灰度字节，绕过 {@code Raster.getSample} 的逐像素方法调用
     * （后者要走 SampleModel 的坐标换算，1024 个像素的开销不可忽略）。
     * 拿不到字节缓冲时退回 getSample，保证任何 Raster 实现都能出结果。 */
    private static double[][] grayscale(BufferedImage gray) {
        Raster raster = gray.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        double[][] out = new double[SIZE][SIZE];
        if (buffer instanceof DataBufferByte && buffer.getNumBanks() == 1) {
            byte[] data = ((DataBufferByte) buffer).getData();
            int stride = SIZE;
            int offset = 0;
            if (raster.getSampleModel() instanceof ComponentSampleModel) {
                ComponentSampleModel model = (ComponentSampleModel) raster.getSampleModel();
                stride = model.getScanlineStride();
                int[] bandOffsets = model.getBandOffsets();
                offset = bandOffsets.length > 0 ? bandOffsets[0] : 0;
            }
            for (int y = 0; y < SIZE; y++) {
                int rowStart = offset + y * stride;
                for (int x = 0; x < SIZE; x++) {
                    out[y][x] = data[rowStart + x] & 0xFF;
                }
            }
            return out;
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                out[y][x] = raster.getSample(x, y, 0);
            }
        }
        return out;
    }

    /** 一维 DCT，只算前 {@code limit} 个系数；余弦值走 {@link #COS} 查表。 */
    private static double[] dct1d(double[] in, int limit) {
        double[] out = new double[limit];
        for (int u = 0; u < limit; u++) {
            double[] cosRow = COS[u];
            double sum = 0.0;
            for (int x = 0; x < SIZE; x++) {
                sum += in[x] * cosRow[x];
            }
            out[u] = sum;
        }
        return out;
    }

    private static double median(double[] values) {
        double[] copy = values.clone();
        java.util.Arrays.sort(copy);
        int mid = copy.length / 2;
        return (copy[mid - 1] + copy[mid]) / 2.0;
    }
}
