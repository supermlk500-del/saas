package com.zhihuitong.security.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.security.config.SecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CaptchaService {
    private static final String CAPTCHA_PREFIX = "auth:captcha:";
    private static final DefaultRedisScript<String> GET_AND_DELETE = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); if value then redis.call('DEL', KEYS[1]); end; return value", String.class);
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(StringRedisTemplate redisTemplate, SecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public Map<String, Object> createCaptcha() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.getCaptcha().isEnabled());
        if (!properties.getCaptcha().isEnabled()) {
            return result;
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + uuid, code,
                Duration.ofMinutes(properties.getCaptcha().getExpireMinutes()));
        result.put("uuid", uuid);
        result.put("image", renderBase64(code));
        return result;
    }

    public void validateAndConsume(String uuid, String code) {
        if (!properties.getCaptcha().isEnabled()) {
            return;
        }
        if (uuid == null || uuid.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        String key = CAPTCHA_PREFIX + uuid;
        String expected = redisTemplate.execute(GET_AND_DELETE, Collections.singletonList(key));
        if (expected == null) {
            throw new BusinessException(400, "验证码已过期");
        }
        if (!expected.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < properties.getCaptcha().getLength(); i++) {
            builder.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    private String renderBase64(String code) {
        try {
            BufferedImage image = new BufferedImage(140, 44, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRect(0, 0, 140, 44);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(new Color(70 + random.nextInt(100), 50 + random.nextInt(90), 30 + random.nextInt(80)));
                graphics.drawString(String.valueOf(code.charAt(i)), 14 + i * 29, 31 + random.nextInt(5));
            }
            graphics.setColor(new Color(180, 190, 205));
            for (int i = 0; i < 5; i++) {
                graphics.drawLine(random.nextInt(140), random.nextInt(44), random.nextInt(140), random.nextInt(44));
            }
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate captcha", exception);
        }
    }
}
