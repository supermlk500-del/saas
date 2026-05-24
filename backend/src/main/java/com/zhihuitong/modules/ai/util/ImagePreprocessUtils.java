package com.zhihuitong.modules.ai.util;

import com.zhihuitong.modules.ai.model.YoloBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

public final class ImagePreprocessUtils {

    private ImagePreprocessUtils() {
    }

    public static PreprocessResult preprocess(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        int originalWidth = sourceImage.getWidth();
        int originalHeight = sourceImage.getHeight();
        float scale = Math.min(targetWidth / (float) originalWidth, targetHeight / (float) originalHeight);
        int resizedWidth = Math.max(1, Math.round(originalWidth * scale));
        int resizedHeight = Math.max(1, Math.round(originalHeight * scale));
        int padX = (targetWidth - resizedWidth) / 2;
        int padY = (targetHeight - resizedHeight) / 2;

        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setColor(new Color(114, 114, 114));
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, padX, padY, resizedWidth, resizedHeight, null);
        } finally {
            graphics.dispose();
        }

        float[] chw = new float[3 * targetWidth * targetHeight];
        int channelSize = targetWidth * targetHeight;
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int rgb = canvas.getRGB(x, y);
                int index = y * targetWidth + x;
                chw[index] = ((rgb >> 16) & 0xFF) / 255.0F;
                chw[channelSize + index] = ((rgb >> 8) & 0xFF) / 255.0F;
                chw[2 * channelSize + index] = (rgb & 0xFF) / 255.0F;
            }
        }
        return new PreprocessResult(chw, scale, padX, padY, originalWidth, originalHeight, targetWidth, targetHeight);
    }

    public static BufferedImage renderDetections(BufferedImage sourceImage, List<YoloBox> boxes, String resultJudge) {
        BufferedImage resultImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = resultImage.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, null);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(2.0F));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 18));

            if (boxes == null || boxes.isEmpty()) {
                graphics.setColor("PASS".equals(resultJudge) ? new Color(46, 125, 50) : new Color(245, 124, 0));
                graphics.drawString("Result: " + resultJudge, 20, 30);
                return resultImage;
            }

            for (YoloBox box : boxes) {
                graphics.setColor(colorForLabel(box.getLabel()));
                graphics.drawRect(box.getX1(), box.getY1(), Math.max(1, box.getX2() - box.getX1()), Math.max(1, box.getY2() - box.getY1()));
                String caption = box.getLabel() + " " + String.format("%.2f", box.getScore());
                int textY = Math.max(20, box.getY1() - 6);
                graphics.fillRect(box.getX1(), textY - 18, Math.max(80, caption.length() * 9), 22);
                graphics.setColor(Color.WHITE);
                graphics.drawString(caption, box.getX1() + 4, textY);
                graphics.setColor(colorForLabel(box.getLabel()));
            }
        } finally {
            graphics.dispose();
        }
        return resultImage;
    }

    private static Color colorForLabel(String label) {
        int hash = Math.abs(label == null ? 0 : label.hashCode());
        return new Color(64 + hash % 128, 64 + (hash / 7) % 128, 64 + (hash / 13) % 128);
    }

    public record PreprocessResult(
            float[] chw,
            float scale,
            int padX,
            int padY,
            int originalWidth,
            int originalHeight,
            int targetWidth,
            int targetHeight
    ) {
    }
}
