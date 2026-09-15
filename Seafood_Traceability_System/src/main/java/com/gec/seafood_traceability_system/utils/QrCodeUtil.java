package com.gec.seafood_traceability_system.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成工具（基于 zxing core，不依赖 javase 模块，自行绘制 PNG）
 */
public final class QrCodeUtil {

    /** 二维码前景色，用系统主题蓝，比纯黑美观 */
    private static final int FOREGROUND = 0xFF0B4F8C;
    private static final int BACKGROUND = 0xFFFFFFFF;

    private QrCodeUtil() {
    }

    /**
     * 把一段文本编码成 PNG 字节数组
     *
     * @param text 二维码内容（本系统为溯源查询页面的 URL）
     * @param size 图片边长（像素），建议 200~500
     */
    public static byte[] toPng(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 静默区：二维码四周留白，太小会导致扫码识别率下降
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = new MultiFormatWriter()
                .encode(text, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? FOREGROUND : BACKGROUND);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }
}
