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

    /**
     * 生成一张占位 PNG：灰色底 + 居中"无二维码"提示。
     * <p>
     * 用于编码不存在等无法出码的情况。之所以不返回 JSON 错误，是因为本接口
     * 被页面的 {@code <img src>} 直接引用——返回 JSON 的话浏览器会把它当图片解析，
     * 结果就是一个破图图标，用户完全不知道发生了什么。
     * 返回一张明确的占位图，界面上能直接看出"这里本该有个二维码但没有"。
     *
     * @param size 图片边长（像素）
     */
    public static byte[] placeholderPng(int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setColor(new java.awt.Color(0xF2F5F9));
            g.fillRect(0, 0, size, size);
            g.setColor(new java.awt.Color(0xC0C4CC));
            // 边框，让占位图在页面上有个明确的边界
            g.drawRect(1, 1, size - 3, size - 3);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, Math.max(12, size / 14)));
            g.setColor(new java.awt.Color(0x90_93_99));
            String text = "无二维码";
            java.awt.FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            g.drawString(text, (size - tw) / 2, size / 2 + fm.getAscent() / 3);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }
}
